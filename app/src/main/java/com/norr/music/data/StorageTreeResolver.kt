package com.norr.music.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/**
 * Đổi URI thư mục mà hệ thống trả về (sau khi người dùng chọn bằng hộp thoại của Android)
 * thành đường dẫn thật trên bộ nhớ.
 *
 * Vì sao cần: thư viện nhạc của app quét theo đường dẫn tệp, nên chỉ khi biết đường dẫn
 * thật thì bài vừa tải mới tự xuất hiện trong thư viện.
 *
 * Định dạng document id của Android:
 *  - `primary:Music/Nhac`  → /storage/emulated/0/Music/Nhac
 *  - `1A2B-3C4D:Music`     → /storage/1A2B-3C4D/Music   (thẻ SD)
 *
 * Thư mục trên dịch vụ đám mây (Google Drive…) không quy ra đường dẫn được —
 * trả về null và lớp gọi sẽ báo cho người dùng chọn lại.
 */
object StorageTreeResolver {

    fun resolve(treeUri: Uri): File? = runCatching {
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val separator = documentId.indexOf(':')
        if (separator < 0) return null

        val volume = documentId.substring(0, separator)
        val relative = documentId.substring(separator + 1)

        val root = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory()
        } else {
            File("/storage/$volume")
        }
        if (!root.exists()) return null

        val target = if (relative.isBlank()) root else File(root, relative)
        if (!target.exists() || !target.isDirectory) return null
        target
    }.getOrNull()

    /** Ghi nhớ quyền truy cập thư mục để lần sau còn dùng được. */
    fun persistPermission(context: Context, treeUri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    /** Thư mục mặc định: Music/Norr Music. */
    fun defaultFolder(): File {
        val music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        return File(music, "Norr Music")
    }
}
