package com.norr.music.data

import com.mpatric.mp3agic.ID3v23Tag
import com.mpatric.mp3agic.Mp3File
import java.io.File

/** Chỉnh ảnh bìa nhúng (ID3v2 APIC). Hiện hỗ trợ file MP3. */
object CoverEditor {

    fun supports(file: File): Boolean = file.extension.equals("mp3", ignoreCase = true)

    fun write(file: File, data: ByteArray, mime: String): Result<Unit> = runCatching {
        check(supports(file)) { "Hiện chỉ hỗ trợ đổi ảnh bìa cho file MP3" }
        val mp3 = Mp3File(file)
        val tag = mp3.id3v2Tag ?: ID3v23Tag()
        tag.setAlbumImage(data, mime)
        mp3.id3v2Tag = tag
        saveInPlace(mp3, file)
    }

    fun remove(file: File): Result<Unit> = runCatching {
        check(supports(file)) { "Hiện chỉ hỗ trợ xóa ảnh bìa cho file MP3" }
        val mp3 = Mp3File(file)
        val tag = mp3.id3v2Tag ?: return@runCatching
        tag.clearAlbumImage()
        mp3.id3v2Tag = tag
        saveInPlace(mp3, file)
    }

    /**
     * Ghi lại tiêu đề, nghệ sĩ, album và (nếu có) ảnh bìa.
     *
     * Dùng cho tính năng dọn thư viện: file tải từ công cụ ngoài thường có tên rác
     * và tag trống, nên phải ghi đè cả bốn trường cùng lúc.
     */
    fun writeMetadata(
        file: File,
        title: String,
        artist: String,
        album: String,
        artwork: ByteArray?,
    ): Result<Unit> = runCatching {
        check(supports(file)) { "Chỉ ghi được tag cho file MP3" }
        val mp3 = Mp3File(file)
        val tag = mp3.id3v2Tag ?: ID3v23Tag()
        if (title.isNotBlank()) tag.title = title
        if (artist.isNotBlank()) tag.artist = artist
        if (album.isNotBlank()) tag.album = album
        if (artwork != null && artwork.isNotEmpty()) {
            tag.setAlbumImage(artwork, "image/jpeg")
        }
        mp3.id3v2Tag = tag
        saveInPlace(mp3, file)
    }

    /**
     * mp3agic chỉ có save(đường dẫn), nên ghi ra file tạm rồi thay thế bản gốc.
     * Giữ backup để không mất file nếu bước thay thế thất bại (thẻ SD bị tháo...).
     */
    private fun saveInPlace(mp3: Mp3File, file: File) {
        val parent = file.parentFile ?: error("Không xác định được thư mục chứa file")
        val temp = File(parent, "${file.nameWithoutExtension}.norr-tmp.mp3")
        val backup = File(parent, "${file.name}.norr-bak")
        if (temp.exists()) temp.delete()
        if (backup.exists()) backup.delete()

        val modified = file.lastModified()
        try {
            mp3.save(temp.absolutePath)

            if (!temp.renameTo(file)) {
                // Một số hệ thống không cho ghi đè khi đổi tên: đổi bản gốc sang backup trước.
                if (!file.renameTo(backup)) {
                    error("Không ghi được vào thẻ nhớ (file đang được sử dụng?)")
                }
                if (!temp.renameTo(file)) {
                    backup.renameTo(file)
                    error("Không ghi được vào thẻ nhớ (file đang được sử dụng?)")
                }
                backup.delete()
            }
            file.setLastModified(modified)
        } finally {
            if (temp.exists()) temp.delete()
            if (backup.exists()) backup.delete()
        }
    }
}
