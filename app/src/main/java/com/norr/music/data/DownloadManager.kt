package com.norr.music.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.norr.music.data.remote.Http
import com.norr.music.data.remote.RemoteTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tải nhiều bài cùng lúc và lưu vào thư mục nhạc của máy.
 *
 * Giới hạn 3 luồng tải song song: đủ nhanh mà không làm nghẽn mạng hay nóng máy —
 * quan trọng với thiết bị yếu.
 */
class DownloadManager(private val context: Context) {

    data class Progress(
        val total: Int,
        val completed: Int,
        val failed: Int,
        val currentTitle: String,
    )

    data class Outcome(
        val saved: Int,
        val failed: Int,
        val directory: String,
    )

    suspend fun downloadAll(
        tracks: List<RemoteTrack>,
        targetDirectory: File? = null,
        onProgress: (Progress) -> Unit,
    ): Outcome = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext Outcome(0, 0, "")

        val directory = resolveDirectory(targetDirectory)
        val semaphore = Semaphore(MAX_PARALLEL)
        val completed = AtomicInteger(0)
        val failed = AtomicInteger(0)
        val total = tracks.size

        onProgress(Progress(total, 0, 0, tracks.first().title))

        coroutineScope {
            tracks.map { track ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val ok = runCatching {
                            val target = uniqueFile(directory, track.suggestedFileName())
                            Http.downloadToFile(track.downloadUrl, target)
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(target.absolutePath),
                                null,
                                null,
                            )
                            true
                        }.getOrDefault(false)

                        if (ok) completed.incrementAndGet() else failed.incrementAndGet()
                        onProgress(
                            Progress(
                                total = total,
                                completed = completed.get(),
                                failed = failed.get(),
                                currentTitle = track.title,
                            ),
                        )
                    }
                }
            }.awaitAll()
        }

        Outcome(
            saved = completed.get(),
            failed = failed.get(),
            directory = directory.absolutePath,
        )
    }

    /**
     * Chọn thư mục đích theo thứ tự ưu tiên:
     * 1. Thư mục người dùng đã chọn (nếu còn tồn tại và ghi được)
     * 2. Mặc định Music/Norr Music
     * 3. Bộ nhớ riêng của app (phương án cuối, luôn ghi được)
     */
    private fun resolveDirectory(preferred: File?): File {
        if (preferred != null) {
            runCatching { if (!preferred.exists()) preferred.mkdirs() }
            if (preferred.exists() && preferred.canWrite()) return preferred
        }

        val fallbackPublic = StorageTreeResolver.defaultFolder()
        runCatching { if (!fallbackPublic.exists()) fallbackPublic.mkdirs() }
        if (fallbackPublic.exists() && fallbackPublic.canWrite()) return fallbackPublic

        val privateDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
            FOLDER,
        )
        runCatching { privateDir.mkdirs() }
        return privateDir
    }

    private fun uniqueFile(directory: File, name: String): File {
        var candidate = File(directory, name)
        if (!candidate.exists()) return candidate

        val base = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.', "")
        var index = 2
        while (candidate.exists()) {
            val nextName = if (extension.isEmpty()) "$base ($index)" else "$base ($index).$extension"
            candidate = File(directory, nextName)
            index++
        }
        return candidate
    }

    private companion object {
        const val MAX_PARALLEL = 3
        const val FOLDER = "Norr Music"
    }
}
