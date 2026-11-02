package com.norr.music.data

import java.text.Normalizer

/**
 * Bỏ dấu tiếng Việt để tìm kiếm dễ hơn.
 *
 * Người dùng gõ "nhac" phải ra "nhạc", "tinh yeu" phải ra "tình yêu".
 * Cách làm: chuẩn hoá NFD để tách dấu thành ký tự riêng rồi bỏ hết dấu kết hợp,
 * sau đó xử lý riêng chữ "đ" vì NFD không tách được nó.
 */
object TextFold {

    fun fold(input: String): String {
        if (input.isEmpty()) return input
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        var i = 0
        while (i < decomposed.length) {
            val ch = decomposed[i]
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                out.append(ch)
            }
            i++
        }
        return out.toString()
            .lowercase()
            .replace('đ', 'd')
            .replace('Đ', 'd')
    }
}
