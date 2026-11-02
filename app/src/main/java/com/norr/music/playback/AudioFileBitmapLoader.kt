package com.norr.music.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Nạp ảnh bìa cho thanh thông báo.
 *
 * Mẹo: `artworkUri` của mỗi bài được trỏ thẳng vào **file nhạc** (`file://…/bai.mp3`).
 * Bộ nạp này nhận ra URI đó là file âm thanh nên đọc ảnh nhúng bên trong bằng
 * MediaMetadataRetriever. Nhờ vậy không phải trích xuất ảnh trước cho cả thư viện —
 * chỉ trích khi thực sự cần hiển thị.
 */
class AudioFileBitmapLoader(private val context: Context) : BitmapLoader {

    private val executor = MoreExecutors.listeningDecorator(
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "norr-artwork").apply { isDaemon = true }
        },
    )

    override fun supportsMimeType(mimeType: String): Boolean =
        mimeType.startsWith("image/") ||
            mimeType.startsWith("audio/") ||
            mimeType == "application/octet-stream"

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit(Callable { decode(data) ?: placeholder() })

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        executor.submit(Callable { load(uri) ?: placeholder() })

    private fun load(uri: Uri): Bitmap? = runCatching {
        when (uri.scheme) {
            "file" -> uri.path?.let { path ->
                val file = File(path)
                if (file.exists() && file.extension.lowercase() in AUDIO_EXT) {
                    embeddedFromAudio(file)
                } else {
                    BitmapFactory.decodeFile(path)
                }
            }

            "content" -> context.contentResolver.openInputStream(uri)?.use { input ->
                decode(input.readBytes())
            }

            else -> null
        }
    }.getOrNull()

    private fun embeddedFromAudio(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val bytes = retriever.embeddedPicture ?: return null
            decode(bytes)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decode(bytes: ByteArray): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) {
            sample *= 2
        }
        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            },
        )
    }.getOrNull()

    /** Ảnh trong suốt 1×1 để Media3 không phải xử lý lỗi khi bài không có bìa. */
    private fun placeholder(): Bitmap = placeholderBitmap

    private companion object {
        val AUDIO_EXT = setOf("mp3", "m4a", "flac", "ogg", "opus", "wav", "aac", "aiff", "wma")

        val placeholderBitmap: Bitmap by lazy {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }
}
