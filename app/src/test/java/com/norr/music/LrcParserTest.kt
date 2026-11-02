package com.norr.music

import com.norr.music.data.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `doc moc thoi gian co ban`() {
        val lines = LrcParser.parse(
            """
            [00:12.30]Dòng đầu tiên
            [00:15.00]Dòng thứ hai
            """.trimIndent(),
        )
        assertEquals(2, lines.size)
        assertEquals(12_300L, lines[0].timeMs)
        assertEquals("Dòng đầu tiên", lines[0].text)
        assertEquals(15_000L, lines[1].timeMs)
    }

    @Test
    fun `mot dong nhieu moc thoi gian`() {
        val lines = LrcParser.parse("[00:10.00][00:40.00]Điệp khúc")
        assertEquals(2, lines.size)
        assertEquals(10_000L, lines[0].timeMs)
        assertEquals(40_000L, lines[1].timeMs)
        assertEquals("Điệp khúc", lines[0].text)
        assertEquals("Điệp khúc", lines[1].text)
    }

    @Test
    fun `cac dinh dang phan le giay`() {
        val oneDigit = LrcParser.parse("[00:05.5]a")
        val twoDigit = LrcParser.parse("[00:05.50]b")
        val threeDigit = LrcParser.parse("[00:05.500]c")
        assertEquals(5_500L, oneDigit[0].timeMs)
        assertEquals(5_500L, twoDigit[0].timeMs)
        assertEquals(5_500L, threeDigit[0].timeMs)
    }

    @Test
    fun `sap xep tang dan du file lon xon`() {
        val lines = LrcParser.parse(
            """
            [00:30.00]ba
            [00:10.00]mot
            [00:20.00]hai
            """.trimIndent(),
        )
        assertEquals(listOf("mot", "hai", "ba"), lines.map { it.text })
    }

    @Test
    fun `bo qua dong khong co moc thoi gian`() {
        val lines = LrcParser.parse(
            """
            [ar:Nghệ sĩ]
            [ti:Tên bài]
            [00:01.00]Lời thật
            """.trimIndent(),
        )
        assertEquals(1, lines.size)
        assertEquals("Lời thật", lines[0].text)
    }

    @Test
    fun `dong trong van duoc giu`() {
        val lines = LrcParser.parse("[00:01.00]\n[00:02.00]chữ")
        assertEquals(2, lines.size)
        assertEquals("", lines[0].text)
    }

    @Test
    fun `indexAt tra ve dong dang hat`() {
        val lines = LrcParser.parse(
            """
            [00:00.00]mot
            [00:10.00]hai
            [00:20.00]ba
            """.trimIndent(),
        )
        assertEquals(0, LrcParser.indexAt(lines, 0L))
        assertEquals(0, LrcParser.indexAt(lines, 9_999L))
        assertEquals(1, LrcParser.indexAt(lines, 10_000L))
        assertEquals(2, LrcParser.indexAt(lines, 25_000L))
    }

    @Test
    fun `indexAt truoc dong dau tien`() {
        val lines = LrcParser.parse("[00:05.00]mot")
        assertEquals(-1, LrcParser.indexAt(lines, 1_000L))
    }

    @Test
    fun `danh sach rong`() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertEquals(-1, LrcParser.indexAt(emptyList(), 5_000L))
    }
}
