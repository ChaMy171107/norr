package com.norr.music.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cài đặt nhẹ dùng SharedPreferences: hình nền, kiểu sắp xếp, bài đang phát dở.
 * Hình nền được **sao chép** vào bộ nhớ riêng của app nên vẫn còn sau khi khởi động lại,
 * kể cả khi ảnh gốc bị xoá hay thẻ SD rút ra.
 */
class AppSettings(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("norr_settings", Context.MODE_PRIVATE)

    var sortMode: SortMode
        get() = runCatching { SortMode.valueOf(prefs.getString(KEY_SORT, null) ?: "") }
            .getOrDefault(SortMode.NAME)
        set(value) = prefs.edit().putString(KEY_SORT, value.name).commit()

    var lastTrackPath: String?
        get() = prefs.getString(KEY_LAST_TRACK, null)
        set(value) = prefs.edit().putString(KEY_LAST_TRACK, value).commit()

    var lastPositionMs: Long
        get() = prefs.getLong(KEY_LAST_POS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_POS, value).commit()

    var wallpaperPath: String?
        get() = prefs.getString(KEY_WALLPAPER, null)
        set(value) = prefs.edit().putString(KEY_WALLPAPER, value).commit()

    /** Kiểu nền đang dùng: một trong các kiểu dựng sẵn, hoặc ảnh của người dùng. */
    var wallpaperStyle: WallpaperStyle
        get() = WallpaperStyle.fromId(prefs.getString(KEY_WALLPAPER_STYLE, null))
        set(value) = prefs.edit().putString(KEY_WALLPAPER_STYLE, value.id).commit()

    /** client_id của Jamendo, người dùng tự đăng ký miễn phí. Để trống thì nguồn đó báo lỗi. */
    var jamendoClientId: String
        get() = prefs.getString(KEY_JAMENDO, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_JAMENDO, value.trim()).commit()

    /** Thư mục nhận nhạc tải về. null = dùng mặc định Music/Norr Music. */
    var downloadFolderPath: String?
        get() = prefs.getString(KEY_DOWNLOAD_FOLDER, null)
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_FOLDER, value).commit()

    /** API key YouTube, dùng để đọc trọn playlist. Để trống thì chỉ đọc 15 bài qua RSS. */
    var youtubeApiKey: String
        get() = prefs.getString(KEY_YOUTUBE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_YOUTUBE, value.trim()).commit()

    val wallpaperFile: File
        get() = File(app.filesDir, "wallpaper.img")

    fun hasWallpaper(): Boolean = wallpaperPath != null && wallpaperFile.exists()

    /** Sao chép ảnh người dùng chọn vào bộ nhớ app. Trả về true nếu thành công. */
    suspend fun saveWallpaper(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val target = wallpaperFile
            app.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            } ?: return@runCatching false
            wallpaperPath = target.absolutePath
            wallpaperStyle = WallpaperStyle.CUSTOM
            true
        }.getOrDefault(false)
    }

    suspend fun clearWallpaper(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            wallpaperFile.delete()
            wallpaperPath = null
            wallpaperStyle = WallpaperStyle.BLACK
            true
        }.getOrDefault(false)
    }

    /** Đặt nền dựng sẵn (không xoá ảnh cũ, phòng khi người dùng muốn quay lại). */
    fun usePreset(style: WallpaperStyle) {
        if (style == WallpaperStyle.CUSTOM && !hasWallpaper()) return
        wallpaperStyle = style
    }

    /**
     * Giải mã hình nền ở kích thước vừa đủ cho màn hình điện thoại.
     * Không giữ bitmap trong bộ nhớ lâu — chỉ dùng khi vẽ nền.
     */
    suspend fun loadWallpaper(maxWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (!hasWallpaper()) return@withContext null
        runCatching {
            val file = wallpaperFile
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0) return@runCatching null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        }.getOrNull()
    }

    private companion object {
        const val KEY_SORT = "sort_mode"
        const val KEY_LAST_TRACK = "last_track"
        const val KEY_LAST_POS = "last_position"
        const val KEY_WALLPAPER = "wallpaper_path"
        const val KEY_WALLPAPER_STYLE = "wallpaper_style"
        const val KEY_JAMENDO = "jamendo_client_id"
        const val KEY_DOWNLOAD_FOLDER = "download_folder_path"
        const val KEY_YOUTUBE = "youtube_api_key"
    }
}
