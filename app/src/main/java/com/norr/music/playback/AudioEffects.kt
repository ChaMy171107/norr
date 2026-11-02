package com.norr.music.playback

import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Equalizer gắn vào phiên âm thanh của ExoPlayer.
 *
 * Vì sao là singleton: Equalizer phải gắn vào đúng `audioSessionId` của trình phát,
 * mà trình phát sống trong [PlaybackService]. Giao diện chỉ cầm MediaController nên
 * không với tới được — cả hai cùng một tiến trình, nên một object dùng chung là cách
 * đơn giản nhất.
 *
 * Lưu ý: `audioSessionId` có thể bằng 0 lúc mới tạo trình phát. Phải chờ
 * `onAudioSessionIdChanged` rồi mới gắn, nếu không Equalizer sẽ ném lỗi.
 */
object AudioEffects {

    private const val TAG = "NorrMusic"

    private var equalizer: Equalizer? = null
    private var sessionId: Int = 0
    private var enabled: Boolean = false
    private var preset: Short = 0

    val isAvailable: Boolean get() = equalizer != null

    val isEnabled: Boolean get() = enabled
    val currentPreset: Short get() = preset

    /** Tên các preset có sẵn trên thiết bị (khác nhau tuỳ hãng). */
    fun presetNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return runCatching {
            (0 until eq.numberOfPresets.toInt()).map { index ->
                runCatching { eq.getPresetName(index.toShort()) }.getOrDefault("Preset $index")
            }
        }.getOrDefault(emptyList())
    }

    fun attach(id: Int) {
        if (id == 0 || id == sessionId) return
        release()
        sessionId = id
        runCatching {
            val eq = Equalizer(0, id)
            eq.enabled = false
            equalizer = eq
            // Khôi phục trạng thái người dùng đã chọn trước đó.
            if (preset in 0 until eq.numberOfPresets) {
                runCatching { eq.usePreset(preset) }
            }
            eq.enabled = enabled
        }.onFailure { Log.w(TAG, "Không tạo được Equalizer: ${it.message}") }
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        sessionId = 0
    }

    fun setEnabled(value: Boolean): Boolean {
        val eq = equalizer ?: return false
        return runCatching {
            eq.enabled = value
            enabled = value
            true
        }.getOrDefault(false)
    }

    fun setPreset(index: Short): Boolean {
        val eq = equalizer ?: return false
        return runCatching {
            eq.usePreset(index)
            preset = index
            true
        }.getOrDefault(false)
    }
}
