package com.norr.music.data.remote

import android.util.Xml
import com.norr.music.data.TextFold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder

data class YouTubeTrack(
    val videoId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String?,
) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$videoId"
}

/**
 * Đọc danh sách bài trong một playlist YouTube — **chỉ đọc tên bài**, không tải âm thanh.
 *
 * Hai đường:
 *  1. **Không cần key** — dùng feed RSS công khai của YouTube. Không cần đăng ký gì,
 *     nhưng YouTube chỉ trả **15 bài gần nhất**.
 *  2. **Có API key** — dùng YouTube Data API chính thức, đọc được toàn bộ playlist,
 *     phân trang 50 bài mỗi lần.
 *
 * Cả hai đều là kênh chính thức. Không có phần nào giả lập trình phát hay lấy luồng âm thanh.
 */
object YouTubePlaylist {

    const val RSS_LIMIT = 15

    private const val RSS = "https://www.youtube.com/feeds/videos.xml?playlist_id="
    private const val API = "https://www.googleapis.com/youtube/v3/playlistItems"
    private const val MAX_PAGES = 10

    data class ScanResult(
        val tracks: List<YouTubeTrack>,
        val limited: Boolean,
    )

    /** Lấy mã playlist từ link đầy đủ hoặc từ chuỗi chỉ có mã. */
    fun extractPlaylistId(link: String): String? {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return null

        Regex("""[?&]list=([A-Za-z0-9_-]{10,})""").find(trimmed)?.let {
            return it.groupValues[1]
        }
        if (trimmed.matches(Regex("""^[A-Za-z0-9_-]{10,}$"""))) return trimmed
        return null
    }

    suspend fun scan(link: String, apiKey: String): ScanResult = withContext(Dispatchers.IO) {
        val id = extractPlaylistId(link)
            ?: error("Link không chứa mã playlist. Cần link có phần “list=…”.")

        if (apiKey.isBlank()) {
            ScanResult(fetchRss(id), limited = true)
        } else {
            ScanResult(fetchApi(id, apiKey), limited = false)
        }
    }

    // ------------------------------------------------------------- RSS (không key)

    private fun fetchRss(playlistId: String): List<YouTubeTrack> {
        val xml = Http.getText(RSS + URLEncoder.encode(playlistId, "UTF-8"))
        return parseRss(xml)
    }

    fun parseRss(xml: String): List<YouTubeTrack> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val out = ArrayList<YouTubeTrack>(RSS_LIMIT)
        var inEntry = false
        var videoId = ""
        var title = ""
        var channel = ""
        var thumb: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> {
                        inEntry = true
                        videoId = ""
                        title = ""
                        channel = ""
                        thumb = null
                    }

                    "yt:videoId" -> if (inEntry) videoId = parser.nextText().trim()
                    "title" -> if (inEntry) title = parser.nextText().trim()
                    "name" -> if (inEntry) channel = parser.nextText().trim()
                    "media:thumbnail" -> if (inEntry && thumb == null) {
                        thumb = parser.getAttributeValue(null, "url")
                    }
                }

                XmlPullParser.END_TAG -> if (parser.name == "entry") {
                    if (videoId.isNotBlank()) {
                        out.add(
                            YouTubeTrack(
                                videoId = videoId,
                                title = title.ifBlank { "Không rõ tên" },
                                channel = channel,
                                thumbnailUrl = thumb,
                            ),
                        )
                    }
                    inEntry = false
                }
            }
            event = parser.next()
        }
        return out
    }

    // ------------------------------------------------------- API chính thức (có key)

    private fun fetchApi(playlistId: String, apiKey: String): List<YouTubeTrack> {
        val out = ArrayList<YouTubeTrack>(50)
        var pageToken: String? = null
        var page = 0

        while (page < MAX_PAGES) {
            val url = buildString {
                append(API)
                append("?part=snippet&maxResults=50")
                append("&playlistId=").append(URLEncoder.encode(playlistId, "UTF-8"))
                append("&key=").append(URLEncoder.encode(apiKey.trim(), "UTF-8"))
                pageToken?.let { append("&pageToken=").append(URLEncoder.encode(it, "UTF-8")) }
            }

            val root = Http.getJson(url)

            root.optJSONObject("error")?.let { error ->
                val reason = error.optJSONArray("errors")
                    ?.optJSONObject(0)
                    ?.optString("reason")
                    .orEmpty()
                val message = error.optString("message")
                throw IllegalStateException(
                    "YouTube API từ chối: ${message.ifBlank { reason }}. " +
                        "Kiểm tra API key đã bật YouTube Data API v3 chưa.",
                )
            }

            val items = root.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val videoId = snippet.optJSONObject("resourceId")?.optString("videoId").orEmpty()
                if (videoId.isBlank()) continue

                val thumbs = snippet.optJSONObject("thumbnails")
                out.add(
                    YouTubeTrack(
                        videoId = videoId,
                        title = snippet.optString("title").ifBlank { "Không rõ tên" },
                        channel = snippet.optString("videoOwnerChannelTitle")
                            .ifBlank { snippet.optString("channelTitle") },
                        thumbnailUrl = thumbs?.optJSONObject("medium")?.optString("url")
                            ?: thumbs?.optJSONObject("default")?.optString("url"),
                    ),
                )
            }

            pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() }
            page++
            if (pageToken == null) break
        }
        return out
    }

    /**
     * Rút gọn tên bài để đối chiếu với nhạc trong máy: bỏ phần trong ngoặc
     * (Official Video, MV, Lyrics…) và các từ đệm thường gặp.
     */
    fun matchKey(title: String): String = TextFold.fold(
        title
            .replace(Regex("""[(\[{].*?[)\]}]"""), " ")
            .replace(
                Regex("""(?i)\b(official|video|lyrics?|audio|mv|m/v|remix|version|hd|4k)\b"""),
                " ",
            )
            // Bỏ dấu phân cách còn sót sau khi cắt từ đệm, ví dụ "Nhạc Trắng - Lyrics".
            .replace(Regex("""[-–—_|/]+"""), " "),
    ).replace(Regex("\\s+"), " ").trim()
}
