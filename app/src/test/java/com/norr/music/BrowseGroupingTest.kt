package com.norr.music

import com.norr.music.data.Track
import com.norr.music.ui.groupAlbums
import com.norr.music.ui.groupArtists
import com.norr.music.ui.groupFolders
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BrowseGroupingTest {

    private fun track(
        path: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
    ) = Track(
        file = File(path),
        title = title,
        artist = artist,
        album = album,
    )

    @Test
    fun `gom bai cung album`() {
        val tracks = listOf(
            track("/m/a1.mp3", artist = "Sơn Tùng", album = "Album A"),
            track("/m/a2.mp3", artist = "Sơn Tùng", album = "Album A"),
            track("/m/b1.mp3", artist = "Khác", album = "Album B"),
        )
        val albums = groupAlbums(tracks)
        assertEquals(2, albums.size)
        assertEquals("Album A", albums[0].name)
        assertEquals(2, albums[0].tracks.size)
    }

    @Test
    fun `album lay nghe si xuat hien nhieu nhat`() {
        val tracks = listOf(
            track("/m/1.mp3", artist = "A", album = "Hop Tuyen"),
            track("/m/2.mp3", artist = "B", album = "Hop Tuyen"),
            track("/m/3.mp3", artist = "B", album = "Hop Tuyen"),
        )
        assertEquals("B", groupAlbums(tracks).first().artist)
    }

    @Test
    fun `bai khong co album vao nhom khong ro`() {
        val albums = groupAlbums(listOf(track("/m/x.mp3", artist = "A")))
        assertEquals("Không rõ album", albums.first().name)
    }

    @Test
    fun `bai khong co nghe si vao nhom khong ro`() {
        val artists = groupArtists(listOf(track("/m/x.mp3", title = "Bài")))
        assertEquals("Không rõ nghệ sĩ", artists.first().name)
    }

    @Test
    fun `ten nghe si unknown bi coi la khong ro`() {
        val artists = groupArtists(listOf(track("/m/x.mp3", artist = "<unknown>")))
        assertEquals("Không rõ nghệ sĩ", artists.first().name)
    }

    @Test
    fun `gom bai cung thu muc`() {
        val tracks = listOf(
            track("/m/nhac/1.mp3"),
            track("/m/nhac/2.mp3"),
            track("/m/khac/3.mp3"),
        )
        val folders = groupFolders(tracks)
        assertEquals(2, folders.size)
        assertEquals("khac", folders[0].name)
        assertEquals("nhac", folders[1].name)
        assertEquals(2, folders[1].tracks.size)
    }

    @Test
    fun `tieu de uu tien tag hon ten file`() {
        val t = track("/m/Artemas _ i like the way _Audio_128k.mp3", title = "i like the way you kiss me")
        assertEquals("i like the way you kiss me", t.displayTitle)
    }

    @Test
    fun `khong co tag thi dung ten file`() {
        val t = track("/m/Bài Hát.mp3")
        assertEquals("Bài Hát", t.displayTitle)
    }
}
