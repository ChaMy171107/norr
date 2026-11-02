package com.norr.music.data.remote

import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gọi HTTP tối giản bằng HttpURLConnection của Android.
 *
 * Cố ý không dùng OkHttp/Retrofit: app vốn nhẹ, chỉ cần vài lời gọi GET,
 * và tránh thêm thư viện vào bản build offline.
 */
object Http {

    private const val USER_AGENT = "NorrMusic/3.9 (Android)"

    fun getJson(url: String, timeoutMs: Int = 15_000): JSONObject {
        val body = getText(url, timeoutMs)
        return JSONObject(body)
    }

    fun getText(url: String, timeoutMs: Int = 15_000): String {
        val connection = open(url, timeoutMs)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Máy chủ trả về mã $code")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun getBytes(url: String, timeoutMs: Int = 20_000): ByteArray {
        val connection = open(url, timeoutMs)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Máy chủ trả về mã $code")
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Tải một tệp về đĩa. Trả về số byte đã ghi.
     * [onProgress] được gọi mỗi khi nhận thêm dữ liệu, tính theo byte đã tải.
     */
    fun downloadToFile(
        url: String,
        target: File,
        timeoutMs: Int = 30_000,
        onProgress: (received: Long, total: Long) -> Unit = { _, _ -> },
    ): Long {
        val connection = open(url, timeoutMs)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Máy chủ trả về mã $code")

            val total = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            target.parentFile?.mkdirs()

            var received = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        onProgress(received, total)
                    }
                }
            }
            if (received == 0L) throw IOException("Tệp tải về rỗng")
            received
        } catch (t: Throwable) {
            target.delete()
            throw t
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, timeoutMs: Int): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "*/*")
        }
}
