package com.norr.music.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Ghi lại lỗi làm app thoát đột ngột, để lần mở sau hiện lên màn hình cho người dùng đọc.
 *
 * Vì sao cần: khi app thoát trong một giây, người dùng không có cách nào lấy log —
 * không adb, không màn hình lỗi. Bộ ghi này lưu stack trace vào bộ nhớ máy, lần mở
 * kế tiếp hiện nguyên văn để chụp màn hình gửi đi.
 */
object CrashReporter {

    private const val PREFS = "norr_crash"
    private const val KEY_LAST = "last_crash"
    private const val FILE_NAME = "norr-crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { save(app, throwable) }
            // Chuyển tiếp cho handler cũ để hệ thống vẫn xử lý như bình thường.
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun last(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST, null)
            ?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST)
            .apply()
        runCatching { crashFile(context)?.delete() }
    }

    private fun crashFile(context: Context): File? = runCatching {
        // Ghi vào bộ nhớ riêng của app, không ra thư mục Download dùng chung —
        // stack trace có thể chứa đường dẫn file và tên bài của người dùng.
        // Người dùng vẫn xem được qua màn hình lỗi và nút Sao chép.
        File(context.filesDir, FILE_NAME)
    }.getOrNull()

    private fun save(context: Context, throwable: Throwable) {
        val report = buildString {
            appendLine("=== NORR MUSIC — BÁO CÁO LỖI ===")
            appendLine("Thiết bị : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android  : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App      : ${versionName(context)}")
            appendLine("Thời điểm: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine()
            append(Log.getStackTraceString(throwable))
        }

        // commit() đồng bộ — trong lúc sắp chết, apply() có thể chưa kịp ghi.
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST, report)
                .commit()
        }

        // Thêm một bản ra bộ nhớ riêng để lần sau còn đọc lại được.
        runCatching { crashFile(context)?.writeText(report) }
    }

    private fun versionName(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    }.getOrDefault("?")
}
