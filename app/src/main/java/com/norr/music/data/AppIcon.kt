package com.norr.music.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Các kiểu biểu tượng (avatar) của ứng dụng trên màn hình chính.
 *
 * Android không cho đặt một ảnh bất kỳ làm icon app. Cách hợp lệ duy nhất là khai báo
 * sẵn nhiều `activity-alias` trong manifest, mỗi alias mang một icon riêng, rồi bật/tắt
 * để chọn cái đang dùng. Mỗi alias đều trỏ về cùng một MainActivity nên dữ liệu và
 * hành vi của app không đổi.
 *
 * Lựa chọn được lưu ngay trong trạng thái component của hệ thống, nên vẫn còn sau khi
 * khởi động lại máy — đúng ý "giữ mãi đến khi đổi".
 */
enum class AppIcon(
    val alias: String,
    val label: String,
) {
    VIOLET("LauncherViolet", "Tím"),
    TEAL("LauncherTeal", "Ngọc"),
    CORAL("LauncherCoral", "San hô"),
    AMBER("LauncherAmber", "Hổ phách"),
    MONO("LauncherMono", "Đen trắng"),
}

object AppIcons {

    fun current(context: Context): AppIcon =
        AppIcon.entries.firstOrNull { isEnabled(context, it) } ?: AppIcon.VIOLET

    /**
     * Đổi icon. Trả về true nếu thao tác thành công.
     *
     * Thứ tự quan trọng: bật alias mới **trước** rồi mới tắt các alias cũ, để không có
     * khoảnh khắc ứng dụng biến mất khỏi màn hình chính.
     */
    fun apply(context: Context, icon: AppIcon): Boolean = runCatching {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            component(context, icon),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        AppIcon.entries.forEach { other ->
            if (other != icon) {
                pm.setComponentEnabledSetting(
                    component(context, other),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
        true
    }.getOrDefault(false)

    private fun isEnabled(context: Context, icon: AppIcon): Boolean {
        val pm = context.packageManager
        val component = component(context, icon)
        return when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            // Chưa từng bật/tắt: lấy theo khai báo trong manifest.
            else -> runCatching { pm.getActivityInfo(component, 0).enabled }.getOrDefault(false)
        }
    }

    private fun component(context: Context, icon: AppIcon): ComponentName =
        ComponentName(context.packageName, "${context.packageName}.${icon.alias}")
}
