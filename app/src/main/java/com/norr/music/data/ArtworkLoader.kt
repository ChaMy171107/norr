package com.norr.music.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File

/**
 * Đọc ảnh bìa nhúng trong file nhạc và cache lại.
 *
 * Khác bản 1.0 ở ba điểm quan trọng cho máy yếu:
 *  - Cache tính theo **byte** chứ không theo số lượng (bản cũ giữ 48 ảnh 1024px ≈ 190 MB).
 *  - Giải mã đúng kích thước cần dùng: danh sách chỉ cần 256px thay vì 1024px.
 *  - Nhớ luôn cả trường hợp **không có ảnh bìa**, tránh mở lại file mỗi lần cuộn.
 */
object ArtworkLoader {

    private const val BUCKET_THUMB = 256
    private const val BUCKET_FULL = 1024

    private val maxBytes: Int = run {
        val heap = Runtime.getRuntime().maxMemory()
        // Dành tối đa 1/8 heap, chặn trên 32 MB để máy 2 GB RAM không bị OOM.
        minOf(heap / 8, 32L * 1024 * 1024).toInt()
    }

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Đánh dấu những bài đã kiểm tra và xác nhận không có ảnh bìa. */
    private val missing = HashSet<String>(64)

    private fun bucketFor(targetPx: Int): Int =
        if (targetPx <= BUCKET_THUMB) BUCKET_THUMB else BUCKET_FULL

    private fun key(track: Track, bucket: Int): String =
        "${track.file.absolutePath}#${track.artworkVersion}#$bucket"

    /**
     * Trả về bitmap đã giải mã ở kích thước gần nhất với [targetPx].
     * Hàm này chặn luồng gọi — luôn gọi trong Dispatchers.IO.
     */
    fun load(track: Track, targetPx: Int = BUCKET_THUMB): Bitmap? {
        val bucket = bucketFor(targetPx)
        val key = key(track, bucket)

        cache.get(key)?.let { return it }

        val missingKey = "${track.file.absolutePath}#${track.artworkVersion}"
        if (missingKey in missing) return null

        val bitmap = readBitmap(track.file, bucket)
        if (bitmap == null) {
            if (missing.size > 512) missing.clear()
            missing.add(missingKey)
            return null
        }
        cache.put(key, bitmap)
        return bitmap
    }

    fun clear() {
        cache.evictAll()
        missing.clear()
    }

    /** Xoá cache của riêng một bài, dùng sau khi đổi hoặc xoá ảnh bìa. */
    fun invalidate(track: Track) {
        cache.remove(key(track, BUCKET_THUMB))
        cache.remove(key(track, BUCKET_FULL))
        missing.remove("${track.file.absolutePath}#${track.artworkVersion}")
    }

    private fun readBitmap(file: File, bucket: Int): Bitmap? {
        if (!file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val bytes = retriever.embeddedPicture ?: return null
            decodeSampled(bytes, bucket)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeSampled(bytes: ByteArray, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSize && bounds.outHeight / (sample * 2) >= maxSize) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull()
    }
}
