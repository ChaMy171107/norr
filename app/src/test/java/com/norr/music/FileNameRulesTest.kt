package com.norr.music

import com.norr.music.data.FileNameRules
import com.norr.music.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FileNameRulesTest {

    private fun exists(candidate: String): Boolean =
        candidate.equals("taken.mp3", ignoreCase = true)

    @Test
    fun `adds original extension when omitted`() {
        val check = FileNameRules.validate("Song.mp3", "Bài mới", ::exists)
        assertEquals("Bài mới.mp3", check.name)
        assertNull(check.error)
    }

    @Test
    fun `keeps explicit audio extension`() {
        val check = FileNameRules.validate("a.mp3", "b.flac", ::exists)
        assertEquals("b.flac", check.name)
        assertNull(check.error)
    }

    @Test
    fun `dotted name without audio extension is kept whole`() {
        val check = FileNameRules.validate("a.mp3", "Mr. Brightside", ::exists)
        assertEquals("Mr. Brightside.mp3", check.name)
        assertNull(check.error)
    }

    @Test
    fun `rejects path separators`() {
        assertNotNull(FileNameRules.validate("a.mp3", "../evil", ::exists).error)
        assertNotNull(FileNameRules.validate("a.mp3", "folder/song", ::exists).error)
        assertNotNull(FileNameRules.validate("a.mp3", "folder\\song", ::exists).error)
    }

    @Test
    fun `rejects invalid characters`() {
        assertNotNull(FileNameRules.validate("a.mp3", "bad:name", ::exists).error)
        assertNotNull(FileNameRules.validate("a.mp3", "bad*name", ::exists).error)
        assertNotNull(FileNameRules.validate("a.mp3", "bad\"name", ::exists).error)
    }

    @Test
    fun `rejects reserved windows names`() {
        assertNotNull(FileNameRules.validate("a.mp3", "CON", ::exists).error)
        assertNotNull(FileNameRules.validate("a.mp3", "lpt1.mp3", ::exists).error)
    }

    @Test
    fun `rejects existing file`() {
        assertNotNull(FileNameRules.validate("a.mp3", "taken.mp3", ::exists).error)
    }

    @Test
    fun `allows case only rename`() {
        val check = FileNameRules.validate("song.mp3", "Song.mp3", ::exists)
        assertEquals("Song.mp3", check.name)
        assertNull(check.error)
    }

    @Test
    fun `trims trailing dots and spaces`() {
        val check = FileNameRules.validate("a.mp3", "hello. . ", ::exists)
        assertEquals("hello.mp3", check.name)
    }

    @Test
    fun `rejects empty name`() {
        assertNotNull(FileNameRules.validate("a.mp3", "   ", ::exists).error)
    }
}

class FormatTest {

    @Test
    fun `formats durations`() {
        assertEquals("--:--", formatDuration(0))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("59:59", formatDuration(3_599_000))
        assertEquals("1:02:05", formatDuration(3_725_000))
    }
}
