package com.norr.music.data

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import com.norr.music.data.remote.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder

/** Thông tin tra được cho một bài. */
data class MetadataMatch(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val score: Float,
)

/**
 * Sửa tên bài và nghệ sĩ cho cả thư viện.
 *
 * Vấn đề: file tải từ công cụ ngoài thường có tên dạng
 * `Artemas _ i like the way you kiss me _official mus_Audio_128k`, tag nghệ sĩ trống,
 * và không có ảnh bìa. App hiển thị đúng chuỗi rác đó.
 *
 * Cách sửa: làm sạch tên file thành từ khoá tìm kiếm, tra Deezer, rồi ghi đè ID3.
 *
 * **Vì sao Deezer chứ không phải iTunes:** đã đo thực tế — iTunes trả **403 ngay từ
 * request thứ 5** khi gọi liên tiếp (giới hạn ~20 lượt/phút), nên 130 bài sẽ hỏng phần
 * lớn. Deezer không cần key, không chặn khi gọi dồn, và trả ảnh bìa 1000×1000.
 */
object MetadataFixer {

    private const val DEEZER = "https://api.deezer.com/search"
    private const val MIN_SCORE = 0.6f
    private const val TAG = "NorrMusic"

    /** Bỏ đuôi rác và dấu phân cách để còn lại từ khoá tìm kiếm. */
    fun cleanName(fileName: String): String = fileName
        .substringBeforeLast('.')
        .replace('_', ' ')
        .replace(
            Regex(
                """(?i)\b(official|music video|lyric video|lyrics|audio|mv|m/v|hd|4k|128k|320k|kbps|youtube|video|remastered)\b""",
            ),
            " ",
        )
        .replace(Regex("""[\[\](){}]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    /**
     * File này có cần sửa không — tránh ghi lại những file đã sạch.
     *
     * Chỉ xét **tag hiện tại**, không xét tên file: sau khi sửa thành công, tên file vẫn
     * giữ nguyên (để không làm hỏng danh sách phát) nhưng tag đã sạch, nên lần chạy sau
     * sẽ bỏ qua đúng những file đó.
     */
    fun needsFix(title: String?, artist: String?): Boolean {
        val noArtist = artist.isNullOrBlank() ||
            artist.equals("<unknown>", ignoreCase = true)
        val dirtyTitle = title.isNullOrBlank() ||
            title.contains('_') ||
            Regex("""(?i)(_audio|_128k|_320k|official|\(youtube\)|_mv\b|_lyrics)""")
                .containsMatchIn(title)
        return noArtist || dirtyTitle
    }

    suspend fun lookup(fileName: String): MetadataMatch? = withContext(Dispatchers.IO) {
        val clean = cleanName(fileName)
        if (clean.length < 3) return@withContext null

        // Thử vài biến thể: bản đầy đủ, bản bỏ từ cuối (thường là rác còn sót),
        // và phần dài nhất khi tách theo " - ". Cách này tăng đáng kể tỉ lệ khớp.
        val variants = buildList {
            add(clean)
            val parts = clean.split(' ').filter { it.isNotBlank() }
            if (parts.size > 4) add(parts.dropLast(1).joinToString(" "))
            clean.split(" - ").maxByOrNull { it.length }?.let { if (it != clean) add(it) }
        }.distinct()

        var best: MetadataMatch? = null
        for (variant in variants) {
            val candidate = searchOnce(variant) ?: continue
            if (candidate.score > (best?.score ?: 0f)) best = candidate
            if (candidate.score >= 0.95f) break
        }
        if (best != null && best.score >= MIN_SCORE) return@withContext best

        // Deezer không có thì thử iTunes — nguồn dự phòng, bù được nhiều bài Deezer thiếu.
        // iTunes giới hạn tần suất gắt nên chỉ gọi khi thật sự cần.
        for (variant in variants.take(2)) {
            val candidate = searchItunes(variant) ?: continue
            if (candidate.score > (best?.score ?: 0f)) best = candidate
            if (candidate.score >= 0.95f) break
        }
        best?.takeIf { it.score >= MIN_SCORE }
    }

    /** Nguồn dự phòng. Không dùng làm nguồn chính vì iTunes chặn sau ~5 lượt liên tiếp. */
    private fun searchItunes(query: String): MetadataMatch? {
        val url = buildString {
            append("https://itunes.apple.com/search?term=")
            append(URLEncoder.encode(query, "UTF-8"))
            append("&entity=song&limit=5&country=US")
        }
        val root = runCatching { Http.getJson(url) }.getOrNull() ?: return null
        val results = root.optJSONArray("results") ?: return null

        var best: MetadataMatch? = null
        var bestScore = 0f
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val title = item.optString("trackName")
            val artist = item.optString("artistName")
            val score = score(query, title, artist)
            if (score > bestScore) {
                bestScore = score
                best = MetadataMatch(
                    title = title,
                    artist = artist,
                    album = item.optString("collectionName"),
                    artworkUrl = item.optString("artworkUrl100")
                        .takeIf { it.startsWith("http") }
                        ?.replace("100x100", "600x600"),
                    score = score,
                )
            }
        }
        return best
    }

    private fun searchOnce(query: String): MetadataMatch? {
        val url = buildString {
            append(DEEZER)
            append("?q=").append(URLEncoder.encode(query, "UTF-8"))
            append("&limit=5")
        }
        val root = runCatching { Http.getJson(url) }.getOrNull() ?: return null
        if (root.has("error")) return null

        val data = root.optJSONArray("data") ?: return null

        var best: MetadataMatch? = null
        var bestScore = 0f

        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val title = item.optString("title")
            val artist = item.optJSONObject("artist")?.optString("name").orEmpty()
            val album = item.optJSONObject("album")
            val score = score(query, title, artist)
            if (score > bestScore) {
                bestScore = score
                best = MetadataMatch(
                    title = title,
                    artist = artist,
                    album = album?.optString("title").orEmpty(),
                    artworkUrl = album?.optString("cover_xl")?.takeIf { it.startsWith("http") },
                    score = score,
                )
            }
        }
        return best
    }

    suspend fun downloadArtwork(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { Http.getBytes(url) }.getOrNull()
    }

    /**
     * Ghi tag và ảnh bìa, rồi báo cho MediaStore quét lại file.
     *
     * File M4A (rất nhiều file tải từ YouTube thực chất là M4A nhưng bị đặt đuôi .mp3)
     * không ghi được ID3 vì chúng dùng container MP4. Với nhóm này ta **đổi tên file**
     * thành "Nghệ sĩ - Tên bài.m4a" — app hiển thị tên file khi không có tag, nên tên
     * vẫn sạch, và đuôi file cũng được sửa cho đúng.
     */
    fun apply(
        context: Context,
        file: File,
        match: MetadataMatch,
        artwork: ByteArray?,
    ): Result<Unit> = writeAny(
        context = context,
        file = file,
        title = match.title,
        artist = match.artist,
        album = match.album,
        artwork = artwork,
    ).map { }

    /** File MP4/M4A: 12 byte đầu là kích thước rồi tới chữ "ftyp". */
    fun isMp4(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val header = ByteArray(12)
            if (input.read(header) < 12) return false
            header[4] == 'f'.code.toByte() &&
                header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() &&
                header[7] == 'p'.code.toByte()
        }
    }.getOrDefault(false)

    /**
     * Ghi thông tin vào file, chọn cách theo **nội dung thật** chứ không theo đuôi file:
     *  - MP3 thật        → mp3agic (đường cũ, đã ổn định)
     *  - M4A/MP4         → đổi tên cho đúng đuôi `.m4a` rồi ghi tag bằng jaudiotagger
     *  - FLAC/OGG/OPUS   → jaudiotagger
     *  - còn lại         → chỉ đổi tên file
     *
     * Điểm quan trọng: rất nhiều file tải từ YouTube là M4A nhưng mang đuôi `.mp3`.
     * Trước đây nhóm này chỉ đổi được tên; giờ ghi được cả tag lẫn ảnh bìa.
     */
    private fun writeAny(
        context: Context,
        file: File,
        title: String,
        artist: String,
        album: String,
        artwork: ByteArray?,
    ): Result<File> = runCatching {
        val mp4 = isMp4(file)
        val realMp3 = file.extension.equals("mp3", ignoreCase = true) && !mp4

        if (realMp3) {
            CoverEditor.writeMetadata(file, title, artist, album, artwork).getOrThrow()
            rescan(context, file)
            return@runCatching file
        }

        // M4A đặt sai đuôi: đổi tên trước để jaudiotagger nhận đúng định dạng.
        val target = if (mp4) renameFile(context, file, title, artist).getOrThrow() else file

        if (TagWriter.supports(target)) {
            TagWriter.write(target, title, artist, album, artwork)
                .onFailure { Log.w(TAG, "Không ghi được tag cho ${target.name}: ${it.message}") }
            rescan(context, target)
        }
        target
    }

    private fun renameFile(
        context: Context,
        file: File,
        title: String,
        artist: String,
    ): Result<File> = runCatching {
        val parent = file.parentFile ?: error("Không xác định được thư mục chứa file")
        val base = sanitizeName(
            listOf(artist, title).filter { it.isNotBlank() }.joinToString(" - "),
        )
        val ext = if (isMp4(file)) "m4a" else file.extension.ifBlank { "mp3" }

        var target = File(parent, "$base.$ext")
        var index = 2
        while (target.exists() && target.absolutePath != file.absolutePath) {
            target = File(parent, "$base ($index).$ext")
            index++
        }
        // Đã đúng tên rồi thì không làm gì.
        if (target.absolutePath == file.absolutePath) return@runCatching file

        if (!file.renameTo(target)) error("Không đổi được tên file (thẻ nhớ từ chối)")
        rescan(context, target)
        target
    }

    private fun sanitizeName(input: String): String = input
        .replace(Regex("""[\\/:*?"<>|\r\n\t]"""), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd('.')
        .take(120)
        .ifBlank { "bai-hat" }

    /**
     * Điểm khớp 0..1. Nếu tên tra được nằm trong chuỗi tìm kiếm (hoặc ngược lại) thì
     * gần như chắc chắn đúng; nếu không thì đếm tỉ lệ từ trùng.
     */
    private fun score(query: String, title: String, artist: String): Float {
        if (title.isBlank()) return 0f
        val q = TextFold.fold(query)
        val t = TextFold.fold(title)
        if (t.isNotEmpty() && (q.contains(t) || t.contains(q))) return 1f

        val queryTokens = q.split(' ').filter { it.length > 2 }.toSet()
        if (queryTokens.isEmpty()) return 0f

        val resultTokens = (t + " " + TextFold.fold(artist))
            .split(' ')
            .filter { it.length > 2 }
            .toSet()
        return queryTokens.count { it in resultTokens }.toFloat() / queryTokens.size
    }

    fun supports(file: File): Boolean =
        CoverEditor.supports(file) || TagWriter.supports(file)

    fun rescan(context: Context, file: File) {
        runCatching {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        }
    }

    /**
     * Người dùng tự nhập tên bài / nghệ sĩ / album.
     *
     * MP3 ghi thẳng vào tag; M4A đổi tên cho đúng đuôi rồi ghi tag; FLAC/OGG ghi tag
     * trực tiếp. Trả về file sau khi xử lý — có thể là file mới nếu đã đổi tên.
     */
    fun writeManual(
        context: Context,
        file: File,
        title: String,
        artist: String,
        album: String,
    ): Result<File> = writeAny(
        context = context,
        file = file,
        title = title,
        artist = artist,
        album = album,
        artwork = null,
    )
}
