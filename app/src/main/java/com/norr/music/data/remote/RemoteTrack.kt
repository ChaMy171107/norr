package com.norr.music.data.remote

/** Nguồn nhạc có cho phép tải về. */
enum class MusicSource(val label: String, val note: String) {
    JAMENDO("Jamendo", "Nhạc Creative Commons, cần client_id miễn phí"),
    ARCHIVE("Internet Archive", "Nhạc public domain, không cần đăng ký"),
}

/**
 * Một bài hát lấy từ nguồn trực tuyến, đã chuẩn hoá để hai nguồn dùng chung.
 */
data class RemoteTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val thumbUrl: String?,
    val downloadUrl: String,
    val source: MusicSource,
) {
    val subtitle: String
        get() = listOf(artist, album)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { source.label }

    /** Tên tệp gợi ý, đã bỏ ký tự không hợp lệ trên bộ nhớ. */
    fun suggestedFileName(): String {
        val base = listOf(artist, title)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { title }
        return sanitize(base) + guessExtension(downloadUrl)
    }

    private fun guessExtension(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".flac") -> ".flac"
            lower.contains(".ogg") || lower.contains(".opus") -> ".ogg"
            lower.contains(".m4a") -> ".m4a"
            else -> ".mp3"
        }
    }

    private fun sanitize(input: String): String {
        val cleaned = input
            .replace(Regex("""[\\/:*?"<>|\r\n\t]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.')
        return cleaned.take(120).ifBlank { "bai-hat" }
    }
}
