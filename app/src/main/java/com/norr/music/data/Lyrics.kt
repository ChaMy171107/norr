package com.norr.music.data

import android.util.LruCache
import java.io.File

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

/**
 * Đọc lời bài hát từ file `.lrc` nằm cạnh file nhạc.
 *
 * Quy ước: cùng thư mục, cùng tên, khác phần mở rộng (không phân biệt hoa thường).
 * Ví dụ `Em Cua Ngay Hom Qua.mp3` → `Em Cua Ngay Hom Qua.lrc`.
 * Hoàn toàn offline, không cần mạng.
 */
object LyricsLoader {

    private val cache = LruCache<String, List<LyricLine>>(12)

    fun load(track: Track): List<LyricLine> {
        val key = "${track.id}#${track.modifiedAt}"
        cache.get(key)?.let { return it }
        val lines = runCatching { read(track.file) }.getOrDefault(emptyList())
        cache.put(key, lines)
        return lines
    }

    fun clear() = cache.evictAll()

    fun indexAt(lines: List<LyricLine>, positionMs: Long): Int =
        LrcParser.indexAt(lines, positionMs)

    private fun read(audio: File): List<LyricLine> {
        val lrc = findLrc(audio) ?: return emptyList()
        if (lrc.length() > 512 * 1024) return emptyList()
        val text = lrc.readText(Charsets.UTF_8)
        return LrcParser.parse(text)
    }

    private fun findLrc(audio: File): File? {
        val parent = audio.parentFile ?: return null
        val base = audio.nameWithoutExtension
        val siblings = parent.listFiles() ?: return null
        return siblings.firstOrNull { file ->
            file.isFile &&
                file.extension.equals("lrc", ignoreCase = true) &&
                file.nameWithoutExtension.equals(base, ignoreCase = true)
        }
    }
}
