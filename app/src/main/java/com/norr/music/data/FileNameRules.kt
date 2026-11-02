package com.norr.music.data

/**
 * Kiểm tra tên file mới trước khi đổi tên thật trên đĩa.
 * Tách khỏi Android để có thể viết unit test thuần JVM.
 */
object FileNameRules {

    val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "m4a", "ogg", "oga", "opus", "wav", "aac", "aif", "aiff", "wma",
    )

    private val INVALID_CHARS = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    private val RESERVED = buildSet {
        addAll(listOf("CON", "PRN", "AUX", "NUL"))
        for (i in 1..9) {
            add("COM$i")
            add("LPT$i")
        }
    }

    data class Check(val name: String?, val error: String?)

    fun validate(originalName: String, rawInput: String, exists: (String) -> Boolean): Check {
        val input = rawInput.trim().trimEnd('.', ' ')
        if (input.isEmpty()) return Check(null, "Tên file không được để trống")
        if (input.any { it in INVALID_CHARS || it.code < 32 }) {
            return Check(null, "Không dùng được ký tự < > : \" / \\ | ? *")
        }
        if (input.length > 180) return Check(null, "Tên file quá dài")

        val originalExt = originalName.substringAfterLast('.', "").ifEmpty { "mp3" }
        val dotIndex = input.lastIndexOf('.')
        val typedExt = if (dotIndex >= 0) input.substring(dotIndex + 1).lowercase() else ""
        val hasAudioExt = typedExt in AUDIO_EXTENSIONS

        var base = if (hasAudioExt) input.substring(0, dotIndex) else input
        base = base.trim().trimEnd('.', ' ').trim()
        if (base.isEmpty()) return Check(null, "Tên file không được để trống")
        if (base.uppercase() in RESERVED) {
            return Check(null, "Tên này hệ thống giữ riêng, hãy chọn tên khác")
        }

        val ext = if (hasAudioExt) typedExt else originalExt.lowercase()
        val candidate = "$base.$ext"

        if (!originalName.equals(candidate, ignoreCase = true) && exists(candidate)) {
            return Check(null, "Đã có file cùng tên trong thư mục")
        }
        return Check(candidate, null)
    }
}
