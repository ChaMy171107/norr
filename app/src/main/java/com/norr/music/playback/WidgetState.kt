package com.norr.music.playback

import android.content.Context
import android.graphics.Bitmap

/**
 * Ảnh chụp trạng thái phát hiện tại, dùng cho widget màn hình chính.
 *
 * Widget không thể tự hỏi trình phát một cách đồng bộ, nên service ghi trạng thái
 * vào đây mỗi khi có thay đổi, rồi bảo widget vẽ lại.
 */
object WidgetState {

    @Volatile
    var title: String = ""

    @Volatile
    var artist: String = ""

    @Volatile
    var isPlaying: Boolean = false

    @Volatile
    var hasTrack: Boolean = false

    @Volatile
    var artwork: Bitmap? = null

    fun update(
        title: String,
        artist: String,
        isPlaying: Boolean,
        hasTrack: Boolean,
        artwork: Bitmap?,
    ) {
        this.title = title
        this.artist = artist
        this.isPlaying = isPlaying
        this.hasTrack = hasTrack
        this.artwork = artwork
    }

    fun clear() {
        title = ""
        artist = ""
        isPlaying = false
        hasTrack = false
        artwork = null
    }

    fun refreshWidgets(context: Context) {
        NorrWidgetProvider.refreshAll(context)
    }
}
