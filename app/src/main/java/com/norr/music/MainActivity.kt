package com.norr.music

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.norr.music.data.CrashReporter
import com.norr.music.ui.AppRoot
import com.norr.music.ui.AppViewModel
import com.norr.music.ui.CrashScreen
import com.norr.music.ui.theme.NorrTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cài sớm để bắt cả lỗi xảy ra trong lúc dựng giao diện.
        CrashReporter.install(applicationContext)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            NorrTheme {
                // Nếu lần chạy trước bị lỗi, hiện nguyên văn lỗi đó trước khi vào app.
                var crashReport by remember { mutableStateOf(CrashReporter.last(this)) }

                val report = crashReport
                if (report != null) {
                    CrashScreen(
                        report = report,
                        onCopy = {
                            // Sao chép stack trace vào clipboard để dễ gửi đi hoặc tìm trên mạng.
                            val clip = ClipData.newPlainText("norr_crash", report)
                            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(clip)
                        },
                        onContinue = {
                            CrashReporter.clear(this)
                            crashReport = null
                        },
                    )
                } else {
                    val vm: AppViewModel = viewModel()
                    AppRoot(vm)
                }
            }
        }
    }
}
