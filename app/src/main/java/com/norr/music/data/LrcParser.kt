package com.norr.music.data

/**
 * Bộ phân tích file LRC — thuần Kotlin, không phụ thuộc Android
 * nên chạy được trong unit test.
 */
object LrcParser {

    private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    /**
     * Tách nội dung file LRC thành danh sách dòng có mốc thời gian, sắp xếp tăng dần.
     * Một dòng có thể mang nhiều mốc thời gian (`[00:12.00][00:45.00]điệp khúc`).
     */
    fun parse(text: String): List<LyricLine> {
        val out = ArrayList<LyricLine>(64)
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            val stamps = TIMESTAMP.findAll(line).toList()
            if (stamps.isEmpty()) continue

            val contentStart = stamps.last().range.last + 1
            val content = if (contentStart <= line.length) {
                line.substring(contentStart).trim()
            } else {
                ""
            }

            for (stamp in stamps) {
                val minutes = stamp.groupValues[1].toLongOrNull() ?: continue
                val seconds = stamp.groupValues[2].toLongOrNull() ?: continue
                val fraction = stamp.groupValues[3]
                val millis = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.take(3).toLong()
                }
                out.add(
                    LyricLine(
                        timeMs = minutes * 60_000L + seconds * 1_000L + millis,
                        text = content,
                    ),
                )
            }
        }
        out.sortBy { it.timeMs }
        return out
    }

    /** Chỉ số dòng đang hát ứng với vị trí phát, hoặc -1 nếu chưa tới dòng nào. */
    fun indexAt(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
