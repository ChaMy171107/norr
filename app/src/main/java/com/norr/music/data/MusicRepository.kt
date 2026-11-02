package com.norr.music.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.Collator
import java.util.Locale

/**
 * Quét và thao tác file nhạc trên bộ nhớ trong + thẻ SD.
 *
 * Chiến lược quét ba lớp, từ nhanh nhất đến chậm nhất:
 *  1. **DB** — file không đổi (cùng size + mtime) thì dùng lại metadata đã lưu, không mở file.
 *  2. **MediaStore** — hệ thống đã đọc sẵn thẻ ID3, lấy luôn mà không cần mở file.
 *  3. **MediaMetadataRetriever** — chỉ dùng cho file mới mà MediaStore không có.
 *
 * Nhờ vậy lần quét thứ hai trở đi gần như tức thì.
 */
class MusicRepository(
    private val context: Context,
    private val db: NorrDb,
) {

    data class ScanResult(
        val tracks: List<Track>,
        val added: Int,
        val updated: Int,
        val removed: Int,
        val total: Int,
    )

    suspend fun storageRoots(): List<File> = withContext(Dispatchers.IO) {
        val roots = LinkedHashSet<File>()
        context.getExternalFilesDirs(null)
            .filterNotNull()
            .forEach { dir ->
                val path = dir.absolutePath
                val cut = path.indexOf("/Android/")
                if (cut > 0) {
                    val root = File(path.substring(0, cut))
                    if (root.canRead()) roots.add(root)
                }
            }
        roots.toList()
    }

    /** Đọc thư viện đã lưu trong DB. Dùng ngay khi mở app để có dữ liệu tức thì. */
    suspend fun cachedTracks(): List<Track> = db.loadTracks()

    /**
     * Quét tăng dần. Trả về danh sách đầy đủ sau khi cập nhật.
     */
    suspend fun refresh(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): ScanResult =
        withContext(Dispatchers.IO) {
            val cached = db.loadTracks().associateBy { it.file.absolutePath }
            val mediaStore = queryMediaStore()

            val files = LinkedHashSet<File>(1024)
            for (root in storageRoots()) {
                collectAudio(root, files)
            }

            val total = files.size
            val changed = ArrayList<Track>(64)
            val all = ArrayList<Track>(total)
            var done = 0
            var added = 0
            var updated = 0

            for (file in files) {
                val path = file.absolutePath
                val meta = mediaStore[path]
                // Ưu tiên số liệu MediaStore đã có sẵn — tránh hai syscall mỗi file.
                // Chỉ đọc thẳng từ file khi MediaStore chưa biết file đó.
                val size = meta?.sizeBytes?.takeIf { it > 0 } ?: file.length()
                val mtime = meta?.modifiedAt?.takeIf { it > 0 } ?: file.lastModified()
                val known = cached[path]

                if (known != null && known.sizeBytes == size && known.modifiedAt == mtime) {
                    // Không đổi: dùng lại metadata trong DB, không mở file.
                    all.add(known.copy(file = file))
                } else {
                    if (known == null) added++ else updated++
                    val track = trackFor(file, meta)
                    changed.add(track)
                    all.add(track)
                }

                done++
                if (done % 200 == 0 || done == total) onProgress(done, total)
            }

            // Ghi phần thay đổi vào DB.
            if (changed.isNotEmpty()) db.upsertTracks(changed)

            // Xoá bài đã biến mất khỏi ổ.
            val present = all.mapTo(HashSet(all.size)) { it.file.absolutePath }
            val gone = cached.keys.filterNot { it in present }
            if (gone.isNotEmpty()) db.deleteTracks(gone)

            // Sắp theo thứ tự tiếng Việt. Collator so sánh trực tiếp, không tạo chuỗi
            // lowercase mới ở mỗi phép so sánh (n·log n lần) như cách cũ.
            val collator = viCollator.get()
            val result = all.sortedWith { a, b -> collator.compare(a.fileName, b.fileName) }
            db.pruneOrphans()

            ScanResult(
                tracks = result,
                added = added,
                updated = updated,
                removed = gone.size,
                total = result.size,
            )
        }

    /** Đọc metadata của toàn bộ nhạc đã được hệ thống lập chỉ mục. */
    private fun queryMediaStore(): Map<String, TrackMeta> {
        val out = HashMap<String, TrackMeta>(1024)
        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            // Lấy sẵn kích thước và ngày sửa để khỏi phải gọi file.length() và
            // file.lastModified() cho từng file — mỗi lần là một syscall chậm trên thẻ SD.
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                null,
            )?.use { c ->
                val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val iModified = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val path = c.getString(iData) ?: continue
                    out[path] = TrackMeta(
                        title = c.getString(iTitle),
                        artist = c.getString(iArtist)?.takeIf { it != "<unknown>" },
                        album = c.getString(iAlbum)?.takeIf { it != "<unknown>" },
                        durationMs = c.getLong(iDur),
                        albumId = c.getLong(iAlbumId),
                        sizeBytes = c.getLong(iSize),
                        // DATE_MODIFIED tính bằng giây, File.lastModified() tính bằng ms.
                        modifiedAt = c.getLong(iModified) * 1000L,
                    )
                }
            }
        }
        return out
    }

    private fun trackFor(file: File, meta: TrackMeta?): Track {
        if (meta != null) {
            return Track(
                file = file,
                title = meta.title,
                artist = meta.artist,
                album = meta.album,
                durationMs = meta.durationMs,
                hasArtwork = meta.albumId > 0,
                sizeBytes = meta.sizeBytes.takeIf { it > 0 } ?: file.length(),
                modifiedAt = meta.modifiedAt.takeIf { it > 0 } ?: file.lastModified(),
                albumId = meta.albumId,
            )
        }

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = 0L
        var hasArt = false

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            hasArt = runCatching { retriever.embeddedPicture != null }.getOrDefault(false)
        } catch (_: Throwable) {
            // File hỏng hoặc định dạng lạ: vẫn đưa vào thư viện với tên file.
        } finally {
            runCatching { retriever.release() }
        }

        return Track(
            file = file,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            hasArtwork = hasArt,
            sizeBytes = file.length(),
            modifiedAt = file.lastModified(),
        )
    }

    suspend fun rename(track: Track, newName: String): Result<Track> = withContext(Dispatchers.IO) {
        runCatching {
            val src = track.file
            val parent = src.parentFile ?: error("Không xác định được thư mục chứa file")
            val dst = File(parent, newName)

            if (!src.exists()) error("File không còn tồn tại")
            if (src.name == newName) return@runCatching track

            val samePathIgnoringCase = src.absolutePath.equals(dst.absolutePath, ignoreCase = true)
            if (!samePathIgnoringCase && dst.exists()) {
                error("Đã có file cùng tên trong thư mục")
            }

            val renamed = if (samePathIgnoringCase && src.name != dst.name) {
                val tmp = File(parent, "${src.name}.norr-rename")
                if (tmp.exists()) tmp.delete()
                if (!src.renameTo(tmp)) error("Không thể đổi tên file (thẻ nhớ đang được sử dụng?)")
                if (!tmp.renameTo(dst)) {
                    tmp.renameTo(src)
                    error("Không thể đổi tên file (thẻ nhớ đang được sử dụng?)")
                }
                true
            } else {
                src.renameTo(dst)
            }

            if (!renamed) error("Không thể đổi tên file (thẻ nhớ đang được sử dụng?)")
            notifyMediaScan(src.absolutePath, dst.absolutePath)

            db.deleteTracks(listOf(src.absolutePath))
            val updated = track.copy(file = dst, modifiedAt = dst.lastModified())
            db.upsertTracks(listOf(updated))
            updated
        }
    }

    suspend fun writeCover(track: Track, uri: Uri): Result<Track> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = readBytes(uri, MAX_COVER_BYTES)
            val mime = sniffImageMime(bytes) ?: error("Ảnh phải là JPEG hoặc PNG")
            CoverEditor.write(track.file, bytes, mime).getOrThrow()
            notifyMediaScan(track.file.absolutePath)
            val updated = track.copy(hasArtwork = true, artworkVersion = track.artworkVersion + 1)
            db.upsertTracks(listOf(updated))
            ArtworkLoader.invalidate(updated)
            updated
        }
    }

    suspend fun removeCover(track: Track): Result<Track> = withContext(Dispatchers.IO) {
        runCatching {
            CoverEditor.remove(track.file).getOrThrow()
            notifyMediaScan(track.file.absolutePath)
            val updated = track.copy(hasArtwork = false, artworkVersion = track.artworkVersion + 1)
            db.upsertTracks(listOf(updated))
            ArtworkLoader.invalidate(updated)
            updated
        }
    }

    private fun collectAudio(
        dir: File,
        out: MutableSet<File>,
        depth: Int = 0,
    ) {
        if (depth > MAX_DEPTH) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                val name = child.name
                if (name.startsWith(".")) continue
                if (name in SKIP_DIRS) continue
                collectAudio(child, out, depth + 1)
            } else if (child.extension.lowercase() in FileNameRules.AUDIO_EXTENSIONS) {
                out.add(child)
            }
        }
    }

    private fun readBytes(uri: Uri, maxBytes: Int): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Không đọc được ảnh đã chọn")
        stream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                if (out.size() > maxBytes) error("Ảnh quá lớn (tối đa 8 MB)")
            }
            return out.toByteArray()
        }
    }

    private fun sniffImageMime(data: ByteArray): String? {
        if (data.size > 3 &&
            data[0] == 0xFF.toByte() &&
            data[1] == 0xD8.toByte() &&
            data[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }
        if (data.size > 8 &&
            data[0] == 0x89.toByte() &&
            data[1] == 'P'.code.toByte() &&
            data[2] == 'N'.code.toByte() &&
            data[3] == 'G'.code.toByte()
        ) {
            return "image/png"
        }
        return null
    }

    private fun notifyMediaScan(vararg paths: String) {
        runCatching { MediaScannerConnection.scanFile(context, paths, null, null) }
    }

    private data class TrackMeta(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val albumId: Long,
        val sizeBytes: Long,
        val modifiedAt: Long,
    )

    private companion object {
        const val MAX_DEPTH = 12
        const val MAX_COVER_BYTES = 8 * 1024 * 1024

        /**
         * Collator không an toàn khi dùng chung giữa các luồng, nên mỗi luồng giữ một bản.
         * Dùng để sắp xếp theo đúng thứ tự tiếng Việt (a, ă, â, b…) thay vì so byte.
         */
        val viCollator: ThreadLocal<Collator> =
            ThreadLocal.withInitial { Collator.getInstance(Locale("vi", "VN")) }

        val SKIP_DIRS = setOf(
            "Android",
            "LOST.DIR",
            "\$RECYCLE.BIN",
            "System Volume Information",
            "Config.Msi",
        )
    }
}
