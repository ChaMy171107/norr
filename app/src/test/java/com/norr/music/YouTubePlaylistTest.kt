package com.norr.music

import com.norr.music.data.remote.YouTubePlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubePlaylistTest {

    @Test
    fun `lay ma playlist tu link day du`() {
        assertEquals(
            "PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf",
            YouTubePlaylist.extractPlaylistId(
                "https://www.youtube.com/playlist?list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf",
            ),
        )
    }

    @Test
    fun `lay ma playlist khi link co nhieu tham so`() {
        assertEquals(
            "PLabc123_-XYZ",
            YouTubePlaylist.extractPlaylistId(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLabc123_-XYZ&index=4",
            ),
        )
    }

    @Test
    fun `lay ma playlist tu link rut gon`() {
        assertEquals(
            "PLabc123_-XYZ",
            YouTubePlaylist.extractPlaylistId("https://youtu.be/dQw4w9WgXcQ?list=PLabc123_-XYZ"),
        )
    }

    @Test
    fun `chap nhan chuoi chi co ma`() {
        assertEquals("PLabc123_-XYZ", YouTubePlaylist.extractPlaylistId("PLabc123_-XYZ"))
    }

    @Test
    fun `tra ve null khi link khong co ma playlist`() {
        assertNull(YouTubePlaylist.extractPlaylistId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertNull(YouTubePlaylist.extractPlaylistId(""))
        assertNull(YouTubePlaylist.extractPlaylistId("abc"))
    }

    @Test
    fun `bo phan trong ngoac khi doi chieu ten`() {
        assertEquals(
            "em cua ngay hom qua",
            YouTubePlaylist.matchKey("Em Của Ngày Hôm Qua (Official Video)"),
        )
    }

    @Test
    fun `bo tu dem khi doi chieu ten`() {
        assertEquals("shape of you", YouTubePlaylist.matchKey("Shape Of You [Official Lyric Video]"))
        assertEquals("nhac trang", YouTubePlaylist.matchKey("Nhạc Trắng - Lyrics"))
    }

    @Test
    fun `giu lai ten binh thuong`() {
        assertEquals("tinh yeu mau nang", YouTubePlaylist.matchKey("Tình Yêu Màu Nắng"))
    }
}
