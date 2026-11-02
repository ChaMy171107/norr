package com.norr.music.ui

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.norr.music.data.AppSettings
import com.norr.music.data.AppIcon
import com.norr.music.data.AppIcons
import com.norr.music.data.ArtworkLoader
import com.norr.music.data.CustomAppIcon
import com.norr.music.data.DownloadManager
import com.norr.music.data.FixUiState
import com.norr.music.data.ImportUiState
import com.norr.music.data.LibraryUiState
import com.norr.music.data.MetadataFixer
import com.norr.music.data.MusicRepository
import com.norr.music.data.NorrDb
import com.norr.music.data.PlaybackUiState
import com.norr.music.data.Playlist
import com.norr.music.data.PlaylistBackup
import com.norr.music.data.PlaylistsUiState
import com.norr.music.data.ScanUiState
import com.norr.music.data.SortMode
import com.norr.music.data.StorageTreeResolver
import com.norr.music.data.TextFold
import com.norr.music.data.Track
import com.norr.music.playback.AudioEffects
import com.norr.music.data.WallpaperStyle
import com.norr.music.data.remote.ArchiveSource
import com.norr.music.data.remote.JamendoSource
import com.norr.music.data.remote.MusicSource
import com.norr.music.data.remote.RemoteTrack
import com.norr.music.data.remote.YouTubePlaylist
import com.norr.music.data.remote.YouTubeTrack
import com.norr.music.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NorrDb.get(application)
    private val repo = MusicRepository(application, db)
    private val settings = AppSettings(application)

    private val _library = MutableStateFlow(LibraryUiState(sort = settings.sortMode))
    val library = _library.asStateFlow()

    private val _playlists = MutableStateFlow(PlaylistsUiState())
    val playlists = _playlists.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackUiState())
    val playback = _playback.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue = _queue.asStateFlow()

    /** Một dòng trong hàng đợi phát. */
    data class QueueEntry(
        val index: Int,
        val track: Track?,
        val title: String,
        val artist: String,
        val isCurrent: Boolean,
    )

    private val _wallpaper = MutableStateFlow<Bitmap?>(null)
    val wallpaper = _wallpaper.asStateFlow()

    private val _wallpaperStyle = MutableStateFlow(WallpaperStyle.BLACK)
    val wallpaperStyle = _wallpaperStyle.asStateFlow()

    private val _appIcon = MutableStateFlow(AppIcon.VIOLET)
    val appIcon = _appIcon.asStateFlow()

    private val _customIcon = MutableStateFlow<Bitmap?>(null)
    val customIcon = _customIcon.asStateFlow()

    /**
     * Thư mục nhận nhạc tải về.
     *
     * Khai báo **trước** khối `init` là bắt buộc: Kotlin khởi tạo thuộc tính theo thứ tự
     * xuất hiện, nên nếu để sau, `refreshDownloadFolder()` trong `init` sẽ chạm vào một
     * MutableStateFlow còn null và app chết ngay khi mở.
     */
    data class DownloadFolder(val path: String, val isCustom: Boolean)

    private val _downloadFolder = MutableStateFlow(DownloadFolder("", false))
    val downloadFolder = _downloadFolder.asStateFlow()

    /** Kết quả quét playlist YouTube. Khai báo trước `init` để không dính bẫy khởi tạo. */
    private val _scan = MutableStateFlow(ScanUiState())
    val scanState = _scan.asStateFlow()

    /** Tiến trình dọn thư viện. Cũng phải khai báo trước `init`. */
    private val _fix = MutableStateFlow(FixUiState())
    val fixState = _fix.asStateFlow()

    /** Launcher trên máy có hỗ trợ ghim lối tắt không. */
    val pinSupported: Boolean = CustomAppIcon.isPinSupported(application)

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionTicker: Job? = null

    /** Chỉ mục path -> Track để tra cứu O(1) khi phát và khi mở playlist. */
    private var index: Map<String, Track> = emptyMap()

    val playlistDetail = MutableStateFlow<PlaylistDetail?>(null)

    data class PlaylistDetail(
        val playlist: Playlist,
        val tracks: List<Track>,
    )

    init {
        connectController()
        loadCachedLibrary()
        loadPlaylists()
        loadWallpaper()
        _appIcon.value = AppIcons.current(application)
        refreshDownloadFolder()
        viewModelScope.launch { _customIcon.value = CustomAppIcon.load(application) }
    }

    // ------------------------------------------------------------- thư viện

    private fun loadCachedLibrary() {
        viewModelScope.launch {
            val cached = repo.cachedTracks()
            if (cached.isNotEmpty()) {
                index = cached.associateBy { it.id }
                _library.update {
                    it.copy(tracks = cached, scanned = true, loading = true)
                }
                applyFilter()
            }
            // Controller có thể đã kết nối trước khi thư viện tải xong.
            controller?.let { restoreLastSession(it) }
            refreshLibrary(silent = cached.isNotEmpty())
        }
    }

    /**
     * Gọi khi người dùng vừa cấp quyền truy cập file.
     * Nếu thư viện còn trống thì quét lại ngay, vì lần quét đầu đã chạy khi chưa có quyền.
     */
    fun ensureLibraryLoaded() {
        val state = _library.value
        if (state.scanning) return
        if (state.tracks.isEmpty() || !state.scanned) {
            refreshLibrary(silent = false)
        }
    }

    fun refreshLibrary(silent: Boolean = false) {
        if (_library.value.scanning) return
        // Đặt cờ ngay (đồng bộ) để hai lần gọi liên tiếp không tạo hai lượt quét song song.
        _library.update {
            it.copy(
                scanning = true,
                loading = !silent || it.tracks.isEmpty(),
                error = null,
                progress = 0,
                progressTotal = 0,
            )
        }
        viewModelScope.launch {
            runCatching { repo.refresh { done, total -> publishProgress(done, total) } }
                .onSuccess { result ->
                    index = result.tracks.associateBy { it.id }
                    _library.update {
                        it.copy(
                            tracks = result.tracks,
                            scanning = false,
                            loading = false,
                            scanned = true,
                            progress = 0,
                            progressTotal = 0,
                        )
                    }
                    applyFilter()
                    loadPlaylists()
                    if (!silent) {
                        _messages.emit(
                            if (result.total == 0) "Không tìm thấy file nhạc nào"
                            else "Đã cập nhật ${result.total} bài hát",
                        )
                    }
                }
                .onFailure { error ->
                    _library.update {
                        it.copy(
                            scanning = false,
                            loading = false,
                            error = error.message ?: "Quét nhạc thất bại",
                        )
                    }
                }
        }
    }

    private fun publishProgress(done: Int, total: Int) {
        _library.update { it.copy(progress = done, progressTotal = total) }
    }

    fun setQuery(query: String) {
        if (_library.value.query == query) return
        _library.update { it.copy(query = query) }
        applyFilter()
    }

    fun setSort(sort: SortMode) {
        if (_library.value.sort == sort) return
        settings.sortMode = sort
        _library.update { it.copy(sort = sort) }
        applyFilter()
    }

    /**
     * Lọc + sắp xếp một lần trong ViewModel thay vì trong composable.
     * Danh sách kết quả được lưu sẵn nên khi cuộn không phải tính lại.
     */
    private fun applyFilter() {
        val state = _library.value
        val rawQuery = state.query.trim()
        val base = if (rawQuery.isEmpty()) {
            state.tracks
        } else {
            // So khớp cả bản có dấu lẫn bản bỏ dấu: gõ "nhac" vẫn ra "nhạc".
            val folded = TextFold.fold(rawQuery)
            val plain = rawQuery.lowercase()
            state.tracks.filter { track ->
                track.searchFolded.contains(folded) || track.searchKey.contains(plain)
            }
        }
        val sorted = when (state.sort) {
            SortMode.NAME -> base.sortedBy { it.fileName.lowercase() }
            SortMode.RECENT -> base.sortedByDescending { it.modifiedAt }
            SortMode.ARTIST -> base.sortedWith(
                compareBy({ (it.artist ?: "").lowercase() }, { it.fileName.lowercase() }),
            )
            SortMode.DURATION -> base.sortedByDescending { it.durationMs }
        }
        _library.update { it.copy(visible = sorted) }
    }

    // ----------------------------------------------------------- playlists

    private fun loadPlaylists() {
        viewModelScope.launch {
            val lists = db.playlists()
            val favs = db.favoritePaths()
            val recent = db.recentPaths()
            _playlists.value = PlaylistsUiState(
                playlists = lists,
                favorites = favs,
                recent = recent,
            )
            playlistDetail.value?.let { detail ->
                val refreshed = lists.firstOrNull { it.id == detail.playlist.id }
                if (refreshed == null) {
                    playlistDetail.value = null
                } else {
                    playlistDetail.value = PlaylistDetail(refreshed, detail.tracks)
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = db.createPlaylist(clean)
            loadPlaylists()
            openPlaylist(id)
            _messages.emit("Đã tạo danh sách “$clean”")
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            db.renamePlaylist(id, clean)
            loadPlaylists()
            playlistDetail.value?.let { detail ->
                if (detail.playlist.id == id) {
                    playlistDetail.value = detail.copy(playlist = detail.playlist.copy(name = clean))
                }
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            db.deletePlaylist(id)
            if (playlistDetail.value?.playlist?.id == id) playlistDetail.value = null
            loadPlaylists()
            _messages.emit("Đã xoá danh sách")
        }
    }

    fun openPlaylist(id: Long) {
        viewModelScope.launch {
            val list = _playlists.value.playlists.firstOrNull { it.id == id } ?: return@launch
            val paths = db.playlistPaths(id)
            val tracks = paths.mapNotNull { index[it] }
            playlistDetail.value = PlaylistDetail(list, tracks)
        }
    }

    /** Mở danh sách tự động: Yêu thích (id ảo -1) hoặc Nghe gần đây (-2). */
    fun openSmartList(favorites: Boolean) {
        viewModelScope.launch {
            val paths = if (favorites) db.favoritePaths().toList() else db.recentPaths()
            val tracks = paths.mapNotNull { index[it] }
            val id = if (favorites) ID_FAVORITES else ID_RECENT
            val name = if (favorites) "Yêu thích" else "Nghe gần đây"
            playlistDetail.value = PlaylistDetail(
                Playlist(id = id, name = name, trackCount = tracks.size),
                tracks,
            )
        }
    }

    fun closePlaylist() {
        playlistDetail.value = null
    }

    fun addToPlaylist(playlistId: Long, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            val inserted = db.addToPlaylist(playlistId, tracks.map { it.id })
            loadPlaylists()
            playlistDetail.value?.let { detail ->
                if (detail.playlist.id == playlistId) openPlaylist(playlistId)
            }
            _messages.emit(
                if (inserted == 0) "Các bài này đã có trong danh sách"
                else "Đã thêm $inserted bài vào danh sách",
            )
        }
    }

    fun removeFromPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            db.removeFromPlaylist(playlistId, track.id)
            loadPlaylists()
            openPlaylist(playlistId)
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val nowFav = db.toggleFavorite(track.id)
            val current = _playlists.value.favorites.toMutableSet()
            if (nowFav) current.add(track.id) else current.remove(track.id)
            _playlists.update { it.copy(favorites = current) }
            _messages.emit(if (nowFav) "Đã thêm vào Yêu thích" else "Đã bỏ khỏi Yêu thích")
        }
    }

    fun isFavorite(track: Track): Boolean = track.id in _playlists.value.favorites

    fun tracksForPaths(paths: List<String>): List<Track> = paths.mapNotNull { index[it] }

    // ------------------------------------------------------------ hình nền

    private fun loadWallpaper() {
        viewModelScope.launch {
            _wallpaperStyle.value = settings.wallpaperStyle
            _wallpaper.value = settings.loadWallpaper()
        }
    }

    fun setWallpaper(uri: Uri) {
        viewModelScope.launch {
            if (settings.saveWallpaper(uri)) {
                _wallpaper.value = settings.loadWallpaper()
                _wallpaperStyle.value = settings.wallpaperStyle
                _messages.emit("Đã đổi hình nền")
            } else {
                _messages.emit("Không đọc được ảnh đã chọn")
            }
        }
    }

    /** Chọn nền dựng sẵn. Chọn "ảnh của bạn" khi chưa có ảnh thì nhắc người dùng. */
    fun setWallpaperStyle(style: WallpaperStyle) {
        if (style == WallpaperStyle.CUSTOM && !settings.hasWallpaper()) {
            viewModelScope.launch { _messages.emit("Hãy chọn một ảnh trước") }
            return
        }
        settings.usePreset(style)
        _wallpaperStyle.value = settings.wallpaperStyle
    }

    fun clearWallpaper() {
        viewModelScope.launch {
            settings.clearWallpaper()
            _wallpaper.value = null
            _wallpaperStyle.value = WallpaperStyle.BLACK
            _messages.emit("Đã trả về nền đen")
        }
    }

    // ---------------------------------------------------- biểu tượng ứng dụng

    fun setAppIcon(icon: AppIcon) {
        if (_appIcon.value == icon) return
        viewModelScope.launch {
            if (AppIcons.apply(getApplication(), icon)) {
                _appIcon.value = icon
                _messages.emit(
                    "Đã đổi biểu tượng. Màn hình chính có thể cần vài giây để cập nhật.",
                )
            } else {
                _messages.emit("Máy này không cho đổi biểu tượng ứng dụng")
            }
        }
    }

    // ------------------------------------------- biểu tượng bằng ảnh của bạn

    fun setCustomIcon(uri: Uri) {
        viewModelScope.launch {
            if (!CustomAppIcon.save(getApplication(), uri)) {
                _messages.emit("Không đọc được ảnh đã chọn")
                return@launch
            }
            val bitmap = CustomAppIcon.load(getApplication())
            _customIcon.value = bitmap
            if (bitmap == null) {
                _messages.emit("Không đọc lại được ảnh vừa lưu")
                return@launch
            }
            if (CustomAppIcon.pin(getApplication(), bitmap)) {
                _messages.emit("Xác nhận trên màn hình chính để ghim biểu tượng mới")
            } else {
                _messages.emit("Đã lưu ảnh, nhưng launcher này không cho ghim tự động")
            }
        }
    }

    fun pinCustomIcon() {
        viewModelScope.launch {
            val bitmap = _customIcon.value ?: CustomAppIcon.load(getApplication())
            if (bitmap == null) {
                _messages.emit("Chưa có ảnh nào, hãy chọn ảnh trước")
                return@launch
            }
            if (CustomAppIcon.pin(getApplication(), bitmap)) {
                _messages.emit("Xác nhận trên màn hình chính để ghim biểu tượng")
            } else {
                _messages.emit("Launcher trên máy không hỗ trợ ghim lối tắt")
            }
        }
    }

    fun clearCustomIcon() {
        viewModelScope.launch {
            CustomAppIcon.delete(getApplication())
            _customIcon.value = null
            _messages.emit("Đã xoá ảnh biểu tượng")
        }
    }

    // --------------------------------------------------------------- nhập nhạc

    private val downloader = DownloadManager(application)

    private val _import = MutableStateFlow(ImportUiState())
    val importState = _import.asStateFlow()

    private fun refreshDownloadFolder() {
        val custom = settings.downloadFolderPath
        _downloadFolder.value = if (custom != null) {
            DownloadFolder(custom, true)
        } else {
            DownloadFolder(StorageTreeResolver.defaultFolder().absolutePath, false)
        }
    }

    fun setDownloadFolder(uri: Uri) {
        val resolved = StorageTreeResolver.resolve(uri)
        if (resolved == null) {
            viewModelScope.launch {
                _messages.emit("Chỉ chọn được thư mục trên bộ nhớ máy hoặc thẻ SD")
            }
            return
        }
        if (!resolved.canWrite()) {
            viewModelScope.launch { _messages.emit("Thư mục này không ghi được, chọn chỗ khác") }
            return
        }
        StorageTreeResolver.persistPermission(getApplication(), uri)
        settings.downloadFolderPath = resolved.absolutePath
        refreshDownloadFolder()
        viewModelScope.launch { _messages.emit("Nhạc tải về sẽ lưu vào ${resolved.name}") }
    }

    fun resetDownloadFolder() {
        settings.downloadFolderPath = null
        refreshDownloadFolder()
        viewModelScope.launch { _messages.emit("Đã về thư mục mặc định") }
    }

    val jamendoClientId: String get() = settings.jamendoClientId

    fun setJamendoClientId(value: String) {
        settings.jamendoClientId = value
        _import.update { it.copy(error = null) }
    }

    // ------------------------------------------------------- dọn thư viện

    /**
     * Người dùng tự sửa tên bài / nghệ sĩ / album.
     *
     * Cần thiết cho những bản remix hiếm mà không nguồn nào biết — tự động tra sẽ ra sai,
     * còn để nguyên thì xấu. Ghi xong phải chờ MediaStore quét lại rồi mới tải lại thư viện.
     */
    fun updateMetadata(track: Track, title: String, artist: String, album: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                MetadataFixer.writeManual(
                    context = getApplication(),
                    file = track.file,
                    title = title,
                    artist = artist,
                    album = album,
                )
            }
            result.onSuccess {
                _messages.emit("Đã cập nhật thông tin bài hát")
                delay(1200)
                refreshLibrary(silent = true)
            }.onFailure { error ->
                _messages.emit(error.message ?: "Không ghi được thông tin")
            }
        }
    }

    /**
     * Sửa tên bài + nghệ sĩ + ảnh bìa cho toàn bộ thư viện.
     *
     * Nghỉ 220 ms giữa các lần tra để không bị iTunes chặn. Sau khi sửa xong phải chờ
     * MediaStore quét lại rồi mới tải lại thư viện, nếu không app vẫn đọc metadata cũ.
     */
    fun runLibraryFix() {
        if (_fix.value.running) return
        val tracks = _library.value.tracks
        if (tracks.isEmpty()) {
            viewModelScope.launch { _messages.emit("Thư viện đang trống, không có gì để sửa") }
            return
        }

        _fix.value = FixUiState(running = true, total = tracks.size)

        viewModelScope.launch {
            var fixed = 0
            var notFound = 0
            var skipped = 0
            var failed = 0
            val unmatched = ArrayList<String>()
            val failedNames = ArrayList<String>()

            for ((index, track) in tracks.withIndex()) {
                _fix.update { it.copy(done = index, current = track.displayTitle) }

                if (!MetadataFixer.supports(track.file)) {
                    skipped++
                    continue
                }

                // Bỏ qua file đã sạch — tránh ghi lại 130 file mỗi lần chạy.
                if (!MetadataFixer.needsFix(track.title, track.artist)) {
                    continue
                }

                val match = MetadataFixer.lookup(track.fileName)
                if (match == null) {
                    notFound++
                    if (unmatched.size < 40) unmatched.add(track.displayTitle)
                    delay(120)
                    continue
                }

                val artwork = match.artworkUrl?.let { MetadataFixer.downloadArtwork(it) }
                val applied = MetadataFixer.apply(
                    context = getApplication(),
                    file = track.file,
                    match = match,
                    artwork = artwork,
                )
                if (applied.isSuccess) {
                    fixed++
                } else {
                    failed++
                    if (failedNames.size < 20) failedNames.add(track.fileName)
                }
                delay(120)
            }

            val summary = buildString {
                append("Đã sửa $fixed bài")
                if (notFound > 0) append(" · không tìm thấy $notFound")
                if (skipped > 0) append(" · bỏ qua $skipped (không phải MP3)")
                if (failed > 0) append(" · lỗi $failed")
            }

            _fix.update {
                it.copy(
                    running = false,
                    done = tracks.size,
                    fixed = fixed,
                    notFound = notFound,
                    skipped = skipped,
                    failed = failed,
                    current = "",
                    summary = summary,
                    unmatched = unmatched,
                    failedNames = failedNames,
                )
            }
            _messages.emit(summary)

            // Chờ MediaStore quét lại các file vừa sửa rồi mới đọc thư viện.
            delay(2500)
            refreshLibrary(silent = true)
        }
    }

    // ------------------------------------------- quét playlist YouTube (chỉ đọc tên)

    val youtubeApiKey: String get() = settings.youtubeApiKey

    fun setYouTubeApiKey(value: String) {
        settings.youtubeApiKey = value
        _scan.update { it.copy(error = null) }
    }

    fun setScanLink(link: String) {
        _scan.update { it.copy(link = link, error = null) }
    }

    fun clearScan() {
        _scan.value = ScanUiState()
    }

    fun runScan() {
        val state = _scan.value
        if (state.scanning) return
        val link = state.link.trim()
        if (link.isEmpty()) return

        _scan.update {
            it.copy(scanning = true, error = null, tracks = emptyList(), owned = emptySet())
        }
        viewModelScope.launch {
            runCatching { YouTubePlaylist.scan(link, settings.youtubeApiKey) }
                .onSuccess { result ->
                    _scan.update {
                        it.copy(
                            scanning = false,
                            tracks = result.tracks,
                            limited = result.limited,
                            owned = matchOwned(result.tracks),
                        )
                    }
                    if (result.tracks.isEmpty()) {
                        _messages.emit("Playlist này không có bài nào đọc được")
                    }
                }
                .onFailure { error ->
                    _scan.update {
                        it.copy(
                            scanning = false,
                            error = error.message ?: "Không đọc được playlist",
                        )
                    }
                }
        }
    }

    /**
     * Bài nào trong playlist đã có sẵn trong thư viện máy (so khớp tên đã bỏ dấu).
     *
     * Tối ưu: gom tất cả tên bài trong máy thành một chuỗi lớn nối bằng ký tự NULL
     * (không xuất hiện trong tên bài). Mỗi bài trong playlist chỉ cần một phép `contains`
     * trên chuỗi lớn thay vì lặp qua từng bài trong máy — từ O(n×m) xuống gần O(n+m).
     * Karena key không chứa NULL nên match không bao giờ vượt biên giữa hai bài.
     */
    private fun matchOwned(items: List<YouTubeTrack>): Set<String> {
        val tracks = _library.value.tracks
        if (tracks.isEmpty()) return emptySet()

        val big = tracks.joinToString(separator = "\u0000") { it.searchFolded }
        return items.filter { item ->
            val key = YouTubePlaylist.matchKey(item.title)
            key.length >= 6 && big.contains(key)
        }.mapTo(HashSet()) { it.videoId }
    }

    fun setImportSource(source: MusicSource) {
        if (_import.value.source == source) return
        _import.update {
            it.copy(
                source = source,
                results = emptyList(),
                selected = emptySet(),
                error = null,
                lastOutcome = null,
            )
        }
    }

    fun setImportQuery(query: String) {
        _import.update { it.copy(query = query) }
    }

    fun runImportSearch() {
        val state = _import.value
        val query = state.query.trim()
        if (query.isEmpty() || state.searching) return

        _import.update {
            it.copy(
                searching = true,
                error = null,
                results = emptyList(),
                selected = emptySet(),
                lastOutcome = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                when (state.source) {
                    MusicSource.JAMENDO -> JamendoSource.search(query, settings.jamendoClientId)
                    MusicSource.ARCHIVE -> ArchiveSource.search(query)
                }
            }.onSuccess { list ->
                _import.update { it.copy(results = list, searching = false) }
                if (list.isEmpty()) _messages.emit("Không tìm thấy bài nào cho “$query”")
            }.onFailure { error ->
                _import.update {
                    it.copy(
                        searching = false,
                        error = error.message ?: "Tìm kiếm thất bại",
                    )
                }
            }
        }
    }

    fun toggleImportSelection(track: RemoteTrack) {
        _import.update { state ->
            val next = if (track.id in state.selected) {
                state.selected - track.id
            } else {
                state.selected + track.id
            }
            state.copy(selected = next)
        }
    }

    fun selectAllImport() {
        _import.update { state ->
            state.copy(selected = state.results.mapTo(HashSet()) { it.id })
        }
    }

    fun clearImportSelection() {
        _import.update { it.copy(selected = emptySet()) }
    }

    fun downloadSelected() {
        val state = _import.value
        if (state.downloading) return

        val tracks = state.results.filter { it.id in state.selected }
        if (tracks.isEmpty()) {
            viewModelScope.launch { _messages.emit("Chưa chọn bài nào") }
            return
        }

        _import.update {
            it.copy(
                downloading = true,
                total = tracks.size,
                completed = 0,
                failed = 0,
                currentTitle = tracks.first().title,
                lastOutcome = null,
                error = null,
            )
        }

        viewModelScope.launch {
            val target = settings.downloadFolderPath?.let { File(it) }
            val outcome = downloader.downloadAll(tracks, target) { progress ->
                _import.update {
                    it.copy(
                        total = progress.total,
                        completed = progress.completed,
                        failed = progress.failed,
                        currentTitle = progress.currentTitle,
                    )
                }
            }
            val message = if (outcome.failed == 0) {
                "Đã tải ${outcome.saved} bài vào ${outcome.directory}"
            } else {
                "Tải được ${outcome.saved} bài, lỗi ${outcome.failed} bài"
            }
            _import.update {
                it.copy(
                    downloading = false,
                    selected = emptySet(),
                    lastOutcome = message,
                )
            }
            _messages.emit(message)
            refreshLibrary(silent = true)
        }
    }

    // ------------------------------------------------------- sao lưu / khôi phục

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = PlaylistBackup.export(db)
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Không ghi được file đã chọn")
                }
            }.onSuccess {
                _messages.emit("Đã sao lưu danh sách phát")
            }.onFailure {
                _messages.emit(it.message ?: "Sao lưu thất bại")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Không đọc được file đã chọn")
                }
                PlaylistBackup.import(db, json, index.keys)
            }.onSuccess { result ->
                loadPlaylists()
                val extra = if (result.skipped > 0) {
                    ", bỏ qua ${result.skipped} bài không còn trên máy"
                } else {
                    ""
                }
                _messages.emit(
                    "Đã nhập ${result.playlists} danh sách, thêm ${result.added} bài$extra",
                )
            }.onFailure {
                _messages.emit(it.message ?: "Khôi phục thất bại")
            }
        }
    }

    // -------------------------------------------------------------- phát nhạc

    private fun connectController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = runCatching {
            MediaController.Builder(context, token).buildAsync()
        }.getOrElse {
            _library.update { it.copy(error = "Không kết nối được trình phát nhạc") }
            return
        }
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }
                .onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    restoreLastSession(mediaController)
                    syncPlayback()
                }
                .onFailure {
                    _library.update { it.copy(error = "Không kết nối được trình phát nhạc") }
                }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Khôi phục bài đang nghe dở lần trước, nhưng không tự phát. */
    private fun restoreLastSession(c: MediaController) {
        val path = settings.lastTrackPath ?: return
        val track = index[path] ?: return
        if (c.mediaItemCount > 0) return
        c.setMediaItem(track.toMediaItem())
        c.prepare()
        c.seekTo(settings.lastPositionMs.coerceAtLeast(0L))
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPlayback()
        }
    }

    /** Bật cập nhật vị trí — chỉ gọi khi màn hình phát đang mở. */
    fun startPositionUpdates() {
        if (positionTicker != null) return
        positionTicker = viewModelScope.launch {
            while (isActive) {
                val c = controller
                if (c != null) {
                    _playback.update {
                        it.copy(positionMs = c.currentPosition.coerceAtLeast(0L))
                    }
                }
                delay(250)
            }
        }
    }

    fun stopPositionUpdates() {
        positionTicker?.cancel()
        positionTicker = null
    }

    // ------------------------------------------------ tuỳ chọn phát

    private var sleepJob: Job? = null

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _playback.update { it.copy(speed = speed) }
    }

    /**
     * Hẹn giờ tắt: đếm ngược từng phút, hiện số phút còn lại, hết thì dừng phát.
     * Truyền null để huỷ.
     */
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null

        if (minutes == null) {
            _playback.update { it.copy(sleepMinutesLeft = null) }
            return
        }

        _playback.update { it.copy(sleepMinutesLeft = minutes) }
        sleepJob = viewModelScope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000L)
                left--
                _playback.update { it.copy(sleepMinutesLeft = left) }
            }
            controller?.pause()
            _playback.update { it.copy(sleepMinutesLeft = null) }
            _messages.emit("Đã hết giờ hẹn, dừng phát")
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        if (!AudioEffects.setEnabled(enabled)) {
            viewModelScope.launch { _messages.emit("Thiết bị không hỗ trợ Equalizer") }
            return
        }
        _playback.update {
            it.copy(eqEnabled = AudioEffects.isEnabled, eqPresets = AudioEffects.presetNames())
        }
    }

    fun setEqualizerPreset(index: Int) {
        if (!AudioEffects.setPreset(index.toShort())) {
            viewModelScope.launch { _messages.emit("Không đổi được preset Equalizer") }
            return
        }
        if (!AudioEffects.isEnabled) AudioEffects.setEnabled(true)
        _playback.update {
            it.copy(
                eqEnabled = AudioEffects.isEnabled,
                eqPreset = AudioEffects.currentPreset.toInt(),
            )
        }
    }

    private fun syncPlayback() {
        val c = controller ?: return
        val mediaId = c.currentMediaItem?.mediaId
        val track = mediaId?.let { index[it] }
        val duration = c.duration
        // Giữ nguyên các trường do người dùng đặt (tốc độ, hẹn giờ, equalizer) —
        // hàm này dựng lại toàn bộ state nên nếu không chép lại thì chúng sẽ bị xoá
        // mỗi lần trình phát đổi trạng thái.
        val previous = _playback.value
        _playback.value = PlaybackUiState(
            current = track,
            isPlaying = c.playWhenReady && c.playbackState != Player.STATE_ENDED,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = if (duration != C.TIME_UNSET && duration > 0) {
                duration
            } else {
                track?.durationMs ?: 0L
            },
            repeatOne = c.repeatMode == Player.REPEAT_MODE_ONE,
            shuffle = c.shuffleModeEnabled,
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
            speed = c.playbackParameters.speed,
            sleepMinutesLeft = previous.sleepMinutesLeft,
            eqEnabled = AudioEffects.isEnabled,
            eqPreset = AudioEffects.currentPreset.toInt(),
            eqPresets = AudioEffects.presetNames(),
        )
        refreshQueue()
    }

    private fun refreshQueue() {
        val c = controller ?: return
        val count = c.mediaItemCount
        if (count == 0) {
            _queue.value = emptyList()
            return
        }
        val currentIndex = c.currentMediaItemIndex
        val list = ArrayList<QueueEntry>(count)
        for (i in 0 until count) {
            val item = c.getMediaItemAt(i)
            val track = index[item.mediaId]
            val meta = item.mediaMetadata
            list.add(
                QueueEntry(
                    index = i,
                    track = track,
                    title = meta.title?.toString() ?: track?.displayTitle ?: "Không rõ tên",
                    artist = meta.artist?.toString() ?: track?.displayArtist ?: "",
                    isCurrent = i == currentIndex,
                ),
            )
        }
        _queue.value = list
    }

    fun moveInQueue(from: Int, to: Int) {
        val c = controller ?: return
        if (from !in 0 until c.mediaItemCount) return
        if (to !in 0 until c.mediaItemCount) return
        if (from == to) return
        c.moveMediaItem(from, to)
        refreshQueue()
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        if (c.mediaItemCount <= 1) {
            viewModelScope.launch { _messages.emit("Hàng đợi chỉ còn một bài, không xoá được") }
            return
        }
        c.removeMediaItem(index)
        refreshQueue()
    }

    fun jumpToQueueIndex(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekToDefaultPosition(index)
        c.play()
        refreshQueue()
    }

    fun playTrack(track: Track, queue: List<Track>) {
        val c = controller ?: run {
            viewModelScope.launch { _messages.emit("Trình phát đang khởi động, thử lại sau 1–2 giây") }
            return
        }
        val playableQueue = queue.ifEmpty { listOf(track) }
        val startIndex = playableQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        c.setMediaItems(playableQueue.map { it.toMediaItem() }, startIndex, 0L)
        c.repeatMode = if (_playback.value.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        c.prepare()
        c.play()
        rememberSession(track, 0L)
        viewModelScope.launch { db.pushRecent(track.id) }
        syncPlayback()
    }

    fun playFromPlaylist(tracks: List<Track>, startAt: Int) {
        if (tracks.isEmpty()) return
        val track = tracks.getOrNull(startAt) ?: return
        val c = controller ?: return
        c.setMediaItems(tracks.map { it.toMediaItem() }, startAt, 0L)
        c.repeatMode = if (_playback.value.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        c.prepare()
        c.play()
        rememberSession(track, 0L)
        viewModelScope.launch { db.pushRecent(track.id) }
        syncPlayback()
    }

    private fun rememberSession(track: Track, position: Long) {
        settings.lastTrackPath = track.id
        settings.lastPositionMs = position
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.playWhenReady) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) c.seekToNextMediaItem()
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3_000L) c.seekTo(0L)
        else if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem()
        else c.seekTo(0L)
    }

    fun toggleLoop() {
        val current = controller?.repeatMode == Player.REPEAT_MODE_ONE
        val next = if (current) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        controller?.repeatMode = next
        _playback.update { it.copy(repeatOne = next == Player.REPEAT_MODE_ONE) }
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val next = !c.shuffleModeEnabled
        c.shuffleModeEnabled = next
        _playback.update { it.copy(shuffle = next) }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        _playback.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        val insertAt = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertAt, track.toMediaItem())
        viewModelScope.launch { _messages.emit("Sẽ phát tiếp theo: ${track.displayTitle}") }
    }

    // -------------------------------------------------------- sửa bài hát

    fun renameTrack(track: Track, newName: String) {
        viewModelScope.launch {
            repo.rename(track, newName)
                .onSuccess { updated ->
                    replaceTrack(track.id, updated)
                    val c = controller
                    if (c != null && c.currentMediaItem?.mediaId == track.id) {
                        val position = c.currentPosition
                        val playing = c.playWhenReady
                        c.replaceMediaItem(c.currentMediaItemIndex, updated.toMediaItem())
                        c.seekTo(position)
                        if (playing) c.play()
                    }
                    syncPlayback()
                    _messages.emit("Đã đổi tên thành “${updated.fileName}”")
                }
                .onFailure { _messages.emit(it.message ?: "Không đổi được tên file") }
        }
    }

    fun changeCover(track: Track, uri: Uri) {
        viewModelScope.launch {
            repo.writeCover(track, uri)
                .onSuccess { updated ->
                    replaceTrack(track.id, updated)
                    _messages.emit("Đã cập nhật ảnh bìa")
                }
                .onFailure { _messages.emit(it.message ?: "Không cập nhật được ảnh bìa") }
        }
    }

    fun removeCover(track: Track) {
        viewModelScope.launch {
            repo.removeCover(track)
                .onSuccess { updated ->
                    replaceTrack(track.id, updated)
                    _messages.emit("Đã xoá ảnh bìa")
                }
                .onFailure { _messages.emit(it.message ?: "Không xoá được ảnh bìa") }
        }
    }

    private fun replaceTrack(oldId: String, updated: Track) {
        ArtworkLoader.invalidate(updated)
        _library.update { state ->
            state.copy(tracks = state.tracks.map { if (it.id == oldId) updated else it })
        }
        index = index.toMutableMap().apply {
            remove(oldId)
            put(updated.id, updated)
        }
        applyFilter()
        _playback.update { state ->
            if (state.current?.id == oldId) state.copy(current = updated) else state
        }
        playlistDetail.value?.let { detail ->
            val tracks = detail.tracks.map { if (it.id == oldId) updated else it }
            playlistDetail.value = detail.copy(tracks = tracks)
        }
    }

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setUri(Uri.fromFile(file))
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(displayTitle)
                .setArtist(displayArtist)
                .apply {
                    album?.let { setAlbumTitle(it) }
                    artworkUri?.let { setArtworkUri(it) }
                }
                .build(),
        )
        .build()

    /**
     * Ảnh bìa cho thanh thông báo: trỏ thẳng vào file nhạc.
     * [com.norr.music.playback.AudioFileBitmapLoader] sẽ đọc ảnh nhúng bên trong khi cần.
     */
    private val Track.artworkUri: Uri?
        get() = if (hasArtwork) Uri.fromFile(file) else null

    override fun onCleared() {
        stopPositionUpdates()
        _playback.value.current?.let { settings.lastPositionMs = _playback.value.positionMs }
        controller?.removeListener(playerListener)
        controllerFuture?.let { future ->
            runCatching { MediaController.releaseFuture(future) }
        }
        controller = null
        controllerFuture = null
        super.onCleared()
    }

    companion object {
        const val ID_FAVORITES = -1L
        const val ID_RECENT = -2L
    }
}
