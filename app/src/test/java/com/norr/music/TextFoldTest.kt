package com.norr.music

import com.norr.music.data.TextFold
import org.junit.Assert.assertEquals
import org.junit.Test

class TextFoldTest {

    @Test
    fun `bo dau nguyen am co dau`() {
        assertEquals("nhac", TextFold.fold("nhạc"))
        assertEquals("tinh yeu", TextFold.fold("Tình Yêu"))
        assertEquals("em cua ngay hom qua", TextFold.fold("Em Của Ngày Hôm Qua"))
    }

    @Test
    fun `chu d gach ngang thanh d thuong`() {
        assertEquals("duong", TextFold.fold("Đường"))
        assertEquals("dao duc", TextFold.fold("Đạo Đức"))
    }

    @Test
    fun `giu nguyen chuoi khong dau`() {
        assertEquals("hello world", TextFold.fold("Hello World"))
        assertEquals("abc123", TextFold.fold("ABC123"))
    }

    @Test
    fun `chuoi rong`() {
        assertEquals("", TextFold.fold(""))
    }

    @Test
    fun `tim kiem khong dau khop voi co dau`() {
        val title = TextFold.fold("Nhạc Trịnh Công Sơn")
        assertEquals(true, title.contains(TextFold.fold("trinh")))
        assertEquals(true, title.contains(TextFold.fold("cong son")))
    }
}
