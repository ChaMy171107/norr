package com.norr.music.data

import java.io.File

data class Track(
    val file: File,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    val hasArtwork: Boolean = false,
    val sizeBytes: Long = 0L,
    val modifiedAt: Long = 0L,
    val albumId: Long = 0L,
    val artworkVersion: Int = 0,
) {
    val id: String get() = file.absolutePath
    val fileName: String get() = file.name

    /**
     * Ưu tiên tiêu đề trong tag, chỉ dùng tên file khi file không có tag.
     *
     * Trước đây luôn lấy tên file — nên tính năng sửa tag chạy mà người dùng không thấy
     * gì thay đổi trên danh sách, vì tiêu đề vẫn là chuỗi rác trong tên file.
     */
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension

    val displayArtist: String get() = artist?.takeIf { it.isNotBlank() } ?: "Không rõ nghệ sĩ"

    val subtitle: String
        get() = listOfNotNull(
            artist?.takeIf { it.isNotBlank() },
            album?.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { "Không rõ nghệ sĩ" }

    /** Chuỗi tìm kiếm dựng sẵn, tránh lowercase lặp lại mỗi lần gõ phím. */
    val searchKey: String by lazy {
        buildString {
            append(fileName.lowercase())
            append(' ')
            title?.let { append(it.lowercase()) }
            append(' ')
            artist?.let { append(it.lowercase()) }
            append(' ')
            album?.let { append(it.lowercase()) }
        }
    }

    /** Bản đã bỏ dấu, dùng cho tìm kiếm tiếng Việt. */
    val searchFolded: String by lazy { TextFold.fold(searchKey) }
}

enum class SortMode(val label: String) {
    NAME("Tên A–Z"),
    RECENT("Mới nhất"),
    ARTIST("Nghệ sĩ"),
    DURATION("Thời lượng"),
}

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long = 0L,
    val trackCount: Int = 0,
)
