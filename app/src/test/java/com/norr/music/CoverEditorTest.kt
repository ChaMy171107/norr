package com.norr.music

import com.mpatric.mp3agic.Mp3File
import com.norr.music.data.CoverEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

class CoverEditorTest {

    // PNG 1x1 trong suốt hợp lệ.
    private val png = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
    )

    private fun copyFixture(): File {
        val file = File.createTempFile("norr-cover-", ".mp3")
        javaClass.getResourceAsStream("/silence.mp3")!!.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    @Test
    fun `writes cover then removes it`() {
        val file = copyFixture()
        try {
            assertTrue(CoverEditor.supports(file))

            CoverEditor.write(file, png, "image/png").getOrThrow()
            val tagged = Mp3File(file)
            assertNotNull(tagged.id3v2Tag?.albumImage)
            assertEquals("image/png", tagged.id3v2Tag?.albumImageMimeType)

            CoverEditor.remove(file).getOrThrow()
            val cleared = Mp3File(file)
            assertNull(cleared.id3v2Tag?.albumImage)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `rejects unsupported extension`() {
        val file = File.createTempFile("norr-cover-", ".wav")
        try {
            assertTrue(CoverEditor.write(file, png, "image/png").isFailure)
        } finally {
            file.delete()
        }
    }
}
