package com.norr.music.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.norr.music.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cho phép người dùng dùng ảnh của mình làm biểu tượng.
 *
 * **Giới hạn của Android:** không thể đặt ảnh tuỳ ý làm icon gốc của app — icon phải là
 * tài nguyên khai báo sẵn trong manifest. Cách duy nhất hợp lệ là tạo một **lối tắt ghim
 * ra màn hình chính** mang ảnh người dùng; lối tắt này mở thẳng ứng dụng nên dùng như icon.
 *
 * Ảnh được cắt vuông ở giữa, bo tròn, thu về 512 px rồi lưu vào bộ nhớ riêng của app.
 */
object CustomAppIcon {

    private const val SHORTCUT_ID = "norr_custom_icon"
    private const val FILE_NAME = "app_icon.png"
    private const val SIZE = 512

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun has(context: Context): Boolean = file(context).exists()

    suspend fun delete(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            file(context).delete()
            true
        }.getOrDefault(false)
    }

    /** Đọc ảnh người dùng chọn, cắt vuông giữa, bo tròn và lưu lại. */
    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val source = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@runCatching false

            val side = minOf(source.width, source.height)
            if (side <= 0) return@runCatching false

            val square = Bitmap.createBitmap(
                source,
                (source.width - side) / 2,
                (source.height - side) / 2,
                side,
                side,
            )
            val scaled = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)

            val output = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)

            file(context).outputStream().use { out ->
                output.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (scaled !== square) scaled.recycle()
            if (square !== source) square.recycle()
            output.recycle()
            true
        }.getOrDefault(false)
    }

    suspend fun load(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val f = file(context)
            if (!f.exists()) null else BitmapFactory.decodeFile(f.absolutePath)
        }.getOrNull()
    }

    /**
     * Xin hệ thống ghim lối tắt mang ảnh người dùng ra màn hình chính.
     * Trả về false nếu launcher trên máy không hỗ trợ.
     */
    fun pin(context: Context, bitmap: Bitmap): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false

        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel("Norr Music")
            .setLongLabel("Norr Music")
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
            )
            .build()

        // Đẩy thành lối tắt động trước: nhờ vậy nếu người dùng đổi ảnh lần nữa,
        // lối tắt đã ghim cũng được cập nhật theo trên phần lớn launcher.
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }

        return runCatching {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }.getOrDefault(false)
    }

    fun isPinSupported(context: Context): Boolean =
        runCatching { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }
            .getOrDefault(false)
}
