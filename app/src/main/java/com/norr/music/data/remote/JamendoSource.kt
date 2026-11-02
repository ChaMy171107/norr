package com.norr.music.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale

/**
 * Tìm nhạc trên Jamendo — kho nhạc Creative Commons lớn nhất, cho phép tải về.
 *
 * Cần `client_id` miễn phí (đăng ký tại devportal.jamendo.com, khoảng 2 phút).
 * Không có key thì nguồn này báo lỗi rõ ràng thay vì im lặng.
 */
object JamendoSource {

    private const val BASE = "https://api.jamendo.com/v3.0/tracks/"

    suspend fun search(query: String, clientId: String, limit: Int = 40): List<RemoteTrack> =
        withContext(Dispatchers.IO) {
            if (clientId.isBlank()) {
                error("Chưa có client_id của Jamendo. Vào Cài đặt để nhập.")
            }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = buildString {
                append(BASE)
                append("?client_id=").append(clientId.trim())
                append("&format=json&limit=").append(limit)
                append("&audioformat=mp32")
                append("&include=musicinfo")
                // Sắp theo độ phổ biến: bài được nghe nhiều lên trước, thay vì thứ tự
                // tuỳ tiện của kho — đây là lý do bản trước trả về toàn bài ít người biết.
                append("&order=popularity_total")
                append("&search=").append(encoded)
            }

            val root = Http.getJson(url)
            val status = root.optJSONObject("headers")?.optString("status").orEmpty()
            if (status != "success") {
                val message = root.optJSONObject("headers")
                    ?.optString("error_message")
                    ?.takeIf { it.isNotBlank() }
                error(message ?: "Jamendo từ chối yêu cầu (kiểm tra client_id)")
            }

            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val out = ArrayList<RemoteTrack>(results.length())
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue

                // Jamendo chỉ cho tải khi audiodownload_allowed = true.
                // Nếu không, vẫn dùng được luồng nghe trực tiếp.
                val allowed = item.optBoolean("audiodownload_allowed", false)
                val download = if (allowed) {
                    item.optString("audiodownload").takeIf { it.startsWith("http") }
                } else {
                    null
                }
                val stream = item.optString("audio").takeIf { it.startsWith("http") }
                val url = download ?: stream ?: continue

                out.add(
                    RemoteTrack(
                        id = "jamendo:${item.optString("id")}",
                        title = item.optString("name").ifBlank { "Không rõ tên" },
                        artist = item.optString("artist_name"),
                        album = item.optString("album_name"),
                        durationSec = item.optInt("duration", 0),
                        thumbUrl = item.optString("image").takeIf { it.startsWith("http") },
                        downloadUrl = url,
                        source = MusicSource.JAMENDO,
                    ),
                )
            }
            out
        }

    /** Định dạng thời lượng cho danh sách kết quả. */
    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "--:--"
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
    }
}
