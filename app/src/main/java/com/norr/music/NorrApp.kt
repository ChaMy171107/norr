package com.norr.music

import android.app.Application
import com.norr.music.data.CrashReporter

/**
 * Điểm vào sớm nhất của tiến trình — cài bộ ghi lỗi trước khi bất cứ thứ gì khác chạy,
 * để bắt được cả lỗi xảy ra trong lúc dựng Activity.
 */
class NorrApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
