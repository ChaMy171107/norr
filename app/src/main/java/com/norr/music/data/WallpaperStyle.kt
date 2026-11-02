package com.norr.music.data

/**
 * Kiểu nền của ứng dụng.
 *
 * Bốn kiểu dựng sẵn đều rất tối để hợp giao diện đen, cộng thêm lựa chọn dùng ảnh của
 * người dùng. Dựng sẵn bằng gradient nên không tốn bộ nhớ và không cần tệp ảnh nào.
 */
enum class WallpaperStyle(
    val id: String,
    val label: String,
    val description: String,
) {
    BLACK("black", "Đen tuyền", "Nền đen phẳng, tiết kiệm pin nhất"),
    GRAPHITE("graphite", "Than chì", "Xám chì chuyển sắc nhẹ"),
    MIDNIGHT("midnight", "Xanh đêm", "Xanh navy rất tối ở góc trên"),
    PLUM("plum", "Tím mờ", "Ánh tím chìm ở đáy"),
    CUSTOM("custom", "Ảnh của bạn", "Dùng ảnh bạn tự chọn"),
    ;

    companion object {
        fun fromId(id: String?): WallpaperStyle =
            entries.firstOrNull { it.id == id } ?: BLACK
    }
}
