package com.norr.music.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Tìm nhạc trên Internet Archive — kho nhạc public domain và nhạc tải lên cộng đồng,
 * **không cần đăng ký, không cần key**, cho tải trực tiếp.
 *
 * Cách tìm đã được đo thực nghiệm, vì bản đầu tiên cho kết quả sai hoàn toàn:
 *  - Tìm trong trường **title** thay vì tìm khắp mọi trường. Tìm khắp nơi thì
 *    "shape of you" trả về buổi diễn của Jerry Garcia chỉ vì phần mô tả có chữ đó.
 *  - Dùng **cụm từ chính xác** `title:("...")` để tăng độ chính xác.
 *  - **Sắp theo lượt tải** để bài phổ biến lên trước, thay vì thứ tự tuỳ tiện.
 *  - Bỏ lọc theo bộ sưu tập: lọc `collection:(netlabels OR audio_music)` khiến nhạc
 *    Việt gần như biến mất (chỉ còn 1 kết quả cho "nhac viet").
 *  - Archive tự bỏ dấu khi đánh chỉ mục, nên "tinh yeu" và "tình yêu" cho cùng kết quả.
 */
object ArchiveSource {

    private const val SEARCH = "https://archive.org/advancedsearch.php"
    private const val METADATA = "https://archive.org/metadata/"
    private const val DOWNLOAD = "https://archive.org/download/"

    private const val MAX_ITEMS = 8
    private const val MAX_FILES_PER_ITEM = 15
    private const val MAX_TOTAL = 60
    private const val MIN_FILE_BYTES = 150_000L

    /** Dấu hiệu nội dung không phải nhạc — sách nói, truyện đọc, tụng kinh. */
    private val NON_MUSIC_MARKERS = listOf(
        "sachnoi", "sách nói", "truyện", "truyen", "audiobook",
        "pháp âm", "phap am", "kinh pháp", "kinh phap",
        "radio program", "podcast", "nghe đọc", "nghe doc",
    )

    suspend fun search(query: String, limit: Int = MAX_ITEMS): List<RemoteTrack> =
        withContext(Dispatchers.IO) {
            val clean = sanitize(query)
            if (clean.isEmpty()) return@withContext emptyList()

            // Lần 1: cụm từ chính xác trong tên. Cách này cho kết quả đúng nhất.
            val precise = fetch("title:(\"$clean\")", limit)
            if (precise.isNotEmpty()) return@withContext precise

            // Lần 2: nới ra toàn bộ mô tả, phòng khi tên bài không chứa từ khoá.
            fetch(clean, limit)
        }

    private suspend fun fetch(term: String, limit: Int): List<RemoteTrack> {
        val rows = limit.coerceIn(1, MAX_ITEMS)
        val scoped = "$term AND mediatype:(audio) " +
            "AND NOT collection:(librivoxaudio OR oldtimeradio OR radioprograms)"
        val q = URLEncoder.encode(scoped, "UTF-8")
        val url = buildString {
            append(SEARCH)
            append("?q=").append(q)
            append("&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator")
            append("&rows=").append(rows)
            append("&page=1&output=json")
            // Sắp theo lượt tải: bài phổ biến lên trước.
            append("&sort%5B%5D=downloads+desc")
        }

        val root = runCatching { Http.getJson(url) }.getOrNull() ?: return emptyList()
        val docs = root.optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()

        val identifiers = ArrayList<Pair<String, String>>(docs.length())
        for (i in 0 until docs.length()) {
            val doc = docs.optJSONObject(i) ?: continue
            val id = doc.optString("identifier").takeIf { it.isNotBlank() } ?: continue
            val title = doc.optString("title").ifBlank { id }
            if (looksLikeNonMusic(title)) continue
            identifiers.add(id to title)
        }
        if (identifiers.isEmpty()) return emptyList()

        val nested = coroutineScope {
            identifiers.map { (id, title) -> async { loadItem(id, title) } }.awaitAll()
        }
        return nested.flatten().take(MAX_TOTAL)
    }

    private fun looksLikeNonMusic(title: String): Boolean {
        val lower = title.lowercase()
        return NON_MUSIC_MARKERS.any { lower.contains(it) }
    }

    private fun loadItem(identifier: String, collectionTitle: String): List<RemoteTrack> {
        val meta = runCatching {
            Http.getJson(METADATA + URLEncoder.encode(identifier, "UTF-8"))
        }.getOrNull() ?: return emptyList()

        val files = meta.optJSONArray("files") ?: return emptyList()

        // Gom tệp âm thanh trước; nếu bộ sưu tập có MP3 thì chỉ giữ MP3 để mỗi bài
        // không hiện hai lần (MP3 và Flac).
        data class Entry(val file: JSONObject, val name: String, val isMp3: Boolean)

        val entries = ArrayList<Entry>(16)
        for (i in 0 until files.length()) {
            val file = files.optJSONObject(i) ?: continue
            val name = file.optString("name")
            if (name.isBlank()) continue
            if (!isAudio(name, file.optString("format"))) continue

            val size = file.optString("size").toLongOrNull() ?: 0L
            if (size in 1 until MIN_FILE_BYTES) continue

            entries.add(Entry(file, name, name.endsWith(".mp3", ignoreCase = true)))
        }
        if (entries.isEmpty()) return emptyList()

        val chosen = if (entries.any { it.isMp3 }) entries.filter { it.isMp3 } else entries

        val itemCreator = meta.optJSONObject("metadata")?.optString("creator").orEmpty()
        val out = ArrayList<RemoteTrack>(chosen.size)
        for (entry in chosen.take(MAX_FILES_PER_ITEM)) {
            val file = entry.file
            val trackTitle = file.optString("title")
                .ifBlank { entry.name.substringBeforeLast('.') }
                .ifBlank { entry.name }
            val artist = file.optString("artist")
                .ifBlank { file.optString("creator") }
                .ifBlank { itemCreator }

            val encodedName = entry.name.split("/").joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }

            out.add(
                RemoteTrack(
                    id = "archive:$identifier/${entry.name}",
                    title = trackTitle,
                    artist = artist,
                    album = collectionTitle,
                    durationSec = parseLength(file.optString("length")),
                    thumbUrl = "https://archive.org/services/img/$identifier",
                    downloadUrl = DOWNLOAD + identifier + "/" + encodedName,
                    source = MusicSource.ARCHIVE,
                ),
            )
        }
        return out
    }

    /** Bỏ ký tự đặc biệt của cú pháp tìm kiếm, tránh làm hỏng câu truy vấn. */
    private fun sanitize(query: String): String = query
        .replace(Regex("""["()\[\]{}:^~*?\\/]"""), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun isAudio(name: String, format: String): Boolean {
        val lower = name.lowercase()
        if (AUDIO_EXTENSIONS.any { lower.endsWith(it) }) return true
        val f = format.lowercase()
        return f.contains("mp3") || f.contains("ogg") || f.contains("flac") ||
            f.contains("mpeg-4 audio") || f.contains("aac")
    }

    /**
     * Archive trả thời lượng ở hai dạng: số giây ("183.89") hoặc chuỗi đồng hồ
     * ("10:31", "1:02:44"). Phải xử lý cả hai, nếu không phần lớn bài hiện "--:--".
     */
    private fun parseLength(raw: String): Int {
        val text = raw.trim()
        if (text.isEmpty()) return 0
        text.toDoubleOrNull()?.let { return it.roundToInt() }

        val parts = text.split(':')
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: return 0
                val seconds = parts[1].toDoubleOrNull() ?: return 0
                (minutes * 60 + seconds).roundToInt()
            }

            3 -> {
                val hours = parts[0].toIntOrNull() ?: return 0
                val minutes = parts[1].toIntOrNull() ?: return 0
                val seconds = parts[2].toDoubleOrNull() ?: return 0
                (hours * 3600 + minutes * 60 + seconds).roundToInt()
            }

            else -> 0
        }
    }

    private val AUDIO_EXTENSIONS = listOf(".mp3", ".ogg", ".flac", ".m4a", ".aac", ".opus")
}
