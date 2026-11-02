package com.norr.music.data

import android.util.Log
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Ghi tag cho các định dạng mà mp3agic không làm được: FLAC, M4A, OGG, OPUS.
 *
 * Vì sao cần: rất nhiều file tải từ YouTube thực chất là **M4A** (container MP4) nhưng
 * bị đặt đuôi `.mp3`. mp3agic đọc không được nên trước đây chỉ còn cách đổi tên file.
 * Với jaudiotagger thì ghi được cả tag lẫn ảnh bìa cho đúng định dạng.
 *
 * MP3 vẫn đi qua [CoverEditor] (mp3agic) — đường đó đã chạy ổn định, không đổi.
 */
object TagWriter {

    private const val TAG = "NorrMusic"

    private val EXTENSIONS = setOf("flac", "m4a", "mp4", "ogg", "oga", "opus")

    /** jaudiotagger ghi log rất nhiều qua java.util.logging — tắt cho sạch logcat. */
    private val loggerReady: Boolean by lazy {
        runCatching {
            Logger.getLogger("org.jaudiotagger").level = Level.OFF
            Logger.getLogger("org.jaudiotagger.audio").level = Level.OFF
            Logger.getLogger("org.jaudiotagger.tag").level = Level.OFF
        }
        true
    }

    fun supports(file: File): Boolean = file.extension.lowercase() in EXTENSIONS

    fun write(
        file: File,
        title: String,
        artist: String,
        album: String,
        artwork: ByteArray?,
    ): Result<Unit> = runCatching {
        loggerReady

        val audio = AudioFileIO.read(file)
        val tag = audio.tag ?: audio.createDefaultTag().also { audio.tag = it }

        if (title.isNotBlank()) tag.setField(FieldKey.TITLE, title)
        if (artist.isNotBlank()) tag.setField(FieldKey.ARTIST, artist)
        if (album.isNotBlank()) tag.setField(FieldKey.ALBUM, album)

        if (artwork != null && artwork.isNotEmpty()) {
            runCatching {
                tag.deleteArtworkField()
                val art = ArtworkFactory.getNew()
                art.binaryData = artwork
                art.mimeType = "image/jpeg"
                art.isLinked = false
                // setField(Artwork) — không phải setField(FieldKey.COVER_ART, …) vì
                // bản đó nhận String.
                tag.setField(art)
            }.onFailure { Log.w(TAG, "Không nhúng được ảnh bìa cho ${file.name}: ${it.message}") }
        }

        audio.commit()
    }.onFailure {
        Log.w(TAG, "jaudiotagger lỗi với ${file.name}: ${it::class.java.simpleName}: ${it.message}")
    }
}
