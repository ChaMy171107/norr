package com.norr.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.norr.music.MainActivity
import java.io.File
import java.util.concurrent.Executors

/**
 * Service phát nhạc nền (Media3).
 *
 * Nhờ service này nhạc vẫn phát khi tắt màn hình, khi vuốt app khỏi đa nhiệm,
 * và các nút trên thanh thông báo hoạt động. Ảnh bìa trên thông báo do
 * [AudioFileBitmapLoader] đọc trực tiếp từ file nhạc.
 *
 * Service cũng là nguồn dữ liệu cho widget màn hình chính: mỗi khi trình phát đổi
 * trạng thái, nó ghi vào [WidgetState] rồi bảo widget vẽ lại.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    private val artExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "norr-widget-art").apply { isDaemon = true }
    }

    @Volatile
    private var currentArtKey: String? = null

    private val widgetListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishWidgetState()
        }
    }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(AudioFileBitmapLoader(this))
            .build()

        player.addListener(widgetListener)
        player.addListener(audioSessionListener)
        // Trình phát có thể đã có sẵn session id ngay sau khi build.
        if (player.audioSessionId != 0) AudioEffects.attach(player.audioSessionId)
        publishWidgetState()
    }

    /** Equalizer chỉ gắn được khi trình phát đã có audio session id. */
    private val audioSessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            AudioEffects.attach(audioSessionId)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * Khi người dùng vuốt app khỏi đa nhiệm: nếu đang phát thì giữ service sống
     * để nhạc không bị cắt giữa chừng.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.player?.removeListener(widgetListener)
        mediaSession?.player?.removeListener(audioSessionListener)
        AudioEffects.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        artExecutor.shutdown()
        super.onDestroy()
    }

    // --------------------------------------------------------------- widget

    private fun publishWidgetState() {
        val player = mediaSession?.player ?: return
        val item = player.currentMediaItem
        val meta = item?.mediaMetadata
        val title = meta?.title?.toString().orEmpty()
        val artist = meta?.artist?.toString().orEmpty()
        val playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED
        val artUri = meta?.artworkUri
        val key = artUri?.toString().orEmpty()

        WidgetState.update(
            title = title,
            artist = artist,
            isPlaying = playing,
            hasTrack = item != null,
            artwork = WidgetState.artwork,
        )
        NorrWidgetProvider.refreshAll(this)

        if (key == currentArtKey) return
        currentArtKey = key
        if (key.isEmpty()) {
            WidgetState.artwork = null
            NorrWidgetProvider.refreshAll(this)
            return
        }

        artExecutor.execute {
            val bitmap = loadSmallArtwork(artUri)
            // Chỉ áp dụng nếu bài chưa đổi trong lúc đang đọc ảnh.
            if (currentArtKey == key) {
                WidgetState.artwork = bitmap
                NorrWidgetProvider.refreshAll(this)
            }
        }
    }

    /** Đọc ảnh bìa nhúng và thu nhỏ còn 128 px cho widget (bitmap lớn sẽ vượt giới hạn binder). */
    private fun loadSmallArtwork(uri: Uri?): Bitmap? {
        if (uri == null) return null
        return runCatching {
            val path = if (uri.scheme == "file") uri.path else null
            val bytes = if (path != null) {
                val file = File(path)
                if (!file.exists()) return null
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    retriever.embeddedPicture
                } finally {
                    runCatching { retriever.release() }
                }
            } else {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 128 && bounds.outHeight / (sample * 2) >= 128) {
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
    }
}
