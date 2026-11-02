package com.norr.music.ui

import com.norr.music.R
import androidx.compose.ui.res.stringResource
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.norr.music.data.AppIcon
import com.norr.music.data.FixUiState
import com.norr.music.data.WallpaperStyle
import com.norr.music.ui.components.wallpaperSwatchBrush
import com.norr.music.ui.theme.Amber
import com.norr.music.ui.theme.Aqua
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Outline
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet
import com.norr.music.ui.theme.VioletDeep

@Composable
fun SettingsScreen(
    trackCount: Int,
    wallpaperStyle: WallpaperStyle,
    hasCustomImage: Boolean,
    scanning: Boolean,
    appIcon: AppIcon,
    customIcon: Bitmap?,
    pinSupported: Boolean,
    jamendoConfigured: Boolean,
    youtubeConfigured: Boolean,
    fixState: FixUiState,
    downloadFolderPath: String,
    isCustomDownloadFolder: Boolean,
    onPickWallpaperStyle: (WallpaperStyle) -> Unit,
    onPickWallpaper: () -> Unit,
    onPickAppIcon: (AppIcon) -> Unit,
    onPickCustomIcon: () -> Unit,
    onPinCustomIcon: () -> Unit,
    onClearCustomIcon: () -> Unit,
    onEditJamendoKey: () -> Unit,
    onEditYoutubeKey: () -> Unit,
    onRunFix: () -> Unit,
    onPickDownloadFolder: () -> Unit,
    onResetDownloadFolder: () -> Unit,
    onRescan: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text(
                    text = "TUỲ CHỈNH",
                    style = MaterialTheme.typography.labelSmall,
                    color = Aqua,
                    letterSpacing = 2.6.sp,
                )
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Apps)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Biểu tượng ứng dụng",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            "Avatar của Norr Music trên màn hình chính",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon.entries.forEach { icon ->
                        IconSwatch(
                            icon = icon,
                            selected = icon == appIcon,
                            modifier = Modifier.weight(1f),
                            onClick = { onPickAppIcon(icon) },
                        )
                    }
                }

                Text(
                    text = "Đang dùng: ${appIcon.label}. Lựa chọn được lưu lại và giữ nguyên " +
                        "cho tới khi bạn đổi.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Outline)
                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Surface2)
                            .border(0.5.dp, Outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val preview = customIcon
                        if (preview != null) {
                            Image(
                                bitmap = preview.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dùng ảnh của bạn",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            if (customIcon != null) {
                                "Ảnh đã lưu. Ghim ra màn hình chính để dùng."
                            } else {
                                "Chọn một ảnh để làm biểu tượng riêng"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = if (customIcon != null) "Đổi ảnh" else "Chọn ảnh",
                        primary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onPickCustomIcon,
                    )
                    if (customIcon != null) {
                        ActionButton(
                            text = "Ghim ra màn hình",
                            primary = false,
                            modifier = Modifier.weight(1.3f),
                            enabled = pinSupported,
                            onClick = onPinCustomIcon,
                        )
                    }
                }

                if (customIcon != null) {
                    Spacer(Modifier.height(8.dp))
                    ActionButton(
                        text = "Xoá ảnh biểu tượng",
                        primary = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClearCustomIcon,
                    )
                }

                Text(
                    text = "Android không cho thay icon gốc của app bằng ảnh tuỳ ý — icon phải " +
                        "là tài nguyên biên dịch sẵn. Vì vậy thao tác này tạo một lối tắt có ảnh " +
                        "của bạn trên màn hình chính; bấm vào lối tắt đó sẽ mở Norr Music. " +
                        "Icon mặc định vẫn còn trong ngăn kéo ứng dụng.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )

                if (!pinSupported) {
                    Text(
                        text = "Launcher trên máy này không hỗ trợ ghim lối tắt tự động. " +
                            "Bạn có thể tự tạo lối tắt bằng trình quản lý màn hình chính.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Image)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Hình nền trong app",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            wallpaperStyle.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WallpaperStyle.entries.forEach { style ->
                        SwatchCell(
                            style = style,
                            selected = style == wallpaperStyle,
                            hasImage = hasCustomImage,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (style == WallpaperStyle.CUSTOM && !hasCustomImage) {
                                    onPickWallpaper()
                                } else {
                                    onPickWallpaperStyle(style)
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = wallpaperStyle.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Violet,
                    fontWeight = FontWeight.Medium,
                )

                if (wallpaperStyle == WallpaperStyle.CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    ActionButton(
                        text = if (hasCustomImage) "Đổi ảnh khác" else "Chọn ảnh từ máy",
                        primary = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPickWallpaper,
                    )
                }

                Text(
                    text = "Ảnh tự chọn được sao chép vào bộ nhớ riêng của app nên vẫn giữ " +
                        "nguyên sau khi khởi động lại, kể cả khi xoá ảnh gốc.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Refresh)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.library_section), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "$trackCount bài hát đã lưu",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    if (scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Violet,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                ActionButton(
                    text = if (scanning) "Đang quét…" else "Quét lại thư viện",
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !scanning,
                    onClick = onRescan,
                )
                Text(
                    text = "Chỉ những file mới hoặc vừa thay đổi mới được đọc lại.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.CloudDownload)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nguồn tải nhạc",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            if (jamendoConfigured) {
                                "Jamendo đã có client_id · Internet Archive luôn sẵn sàng"
                            } else {
                                "Internet Archive sẵn sàng · Jamendo cần client_id"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                ActionButton(
                    text = if (jamendoConfigured) "Đổi client_id Jamendo" else "Nhập client_id Jamendo",
                    primary = !jamendoConfigured,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEditJamendoKey,
                )
                Text(
                    text = "Internet Archive không cần đăng ký. Jamendo cần client_id miễn phí " +
                        "để tìm nhạc Creative Commons.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.AutoFixHigh)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dọn thư viện",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            "Sửa tên bài, nghệ sĩ và thêm ảnh bìa",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }

                if (fixState.running) {
                    Spacer(Modifier.height(12.dp))
                    val fraction = if (fixState.total > 0) {
                        fixState.done.toFloat() / fixState.total.toFloat()
                    } else {
                        0f
                    }
                    Text(
                        text = "Đang xử lý ${fixState.done}/${fixState.total}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Violet,
                    )
                    if (fixState.current.isNotBlank()) {
                        Text(
                            text = fixState.current,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                        color = Violet,
                        trackColor = Surface2,
                    )
                }

                fixState.summary?.let { summary ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fixState.fixed > 0) Aqua else Amber,
                    )
                    if (fixState.unmatched.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Không tra được thông tin (ví dụ):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                        fixState.unmatched.take(6).forEach { name ->
                            Text(
                                text = "• $name",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (fixState.failedNames.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Không ghi được tag (ví dụ):",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber,
                        )
                        fixState.failedNames.take(5).forEach { name ->
                            Text(
                                text = "• $name",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                ActionButton(
                    text = if (fixState.running) "Đang sửa…" else "Sửa tên toàn bộ thư viện",
                    primary = !fixState.running,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !fixState.running,
                    onClick = onRunFix,
                )
                Text(
                    text = "Tra thông tin qua Deezer (miễn phí, không cần key), lấy ảnh bìa " +
                        "1000×1000. Ghi được tag cho MP3, FLAC, M4A và OGG; file M4A bị đặt " +
                        "sai đuôi .mp3 sẽ được đổi tên cho đúng.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.PlaylistPlay)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "API key YouTube",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            if (youtubeConfigured) {
                                "Đã có key — quét được toàn bộ playlist"
                            } else {
                                "Chưa có key — chỉ quét được 15 bài gần nhất"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                ActionButton(
                    text = if (youtubeConfigured) "Đổi API key" else "Nhập API key",
                    primary = !youtubeConfigured,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEditYoutubeKey,
                )
                Text(
                    text = "Chỉ dùng để đọc tên bài trong playlist (YouTube Data API v3). " +
                        "Không dùng để tải âm thanh. Tạo key miễn phí ở Google Cloud Console, " +
                        "hạn mức 10.000 lượt/ngày.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.FolderOpen)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Thư mục tải nhạc",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            if (isCustomDownloadFolder) "Thư mục bạn tự chọn" else "Mặc định: Music/Norr Music",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = downloadFolderPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = Violet,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = "Chọn thư mục",
                        primary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onPickDownloadFolder,
                    )
                    if (isCustomDownloadFolder) {
                        ActionButton(
                            text = "Về mặc định",
                            primary = false,
                            modifier = Modifier.weight(1f),
                            onClick = onResetDownloadFolder,
                        )
                    }
                }

                Text(
                    text = "Nhạc tải về sẽ tự vào đúng thư mục này và hiện ngay trong thư viện. " +
                        "Chọn được thư mục trên bộ nhớ máy hoặc thẻ SD.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Save)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Sao lưu danh sách phát",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            "Giữ lại công sắp xếp khi đổi máy",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = "Sao lưu",
                        primary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onExport,
                    )
                    ActionButton(
                        text = "Khôi phục",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onImport,
                    )
                }
                Text(
                    text = "File JSON chỉ chứa tên danh sách và đường dẫn bài. " +
                        "Bài nào không còn trên máy sẽ được bỏ qua khi khôi phục.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Violet, VioletDeep))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("N", style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "Phiên bản ${com.norr.music.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
                Text(
                    text = "Trình phát nhạc offline: không quảng cáo, không tài khoản, không cần mạng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface1)
            .border(0.5.dp, Outline, RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content,
    )
}

private fun iconColors(icon: AppIcon): Pair<Color, Color> = when (icon) {
    AppIcon.VIOLET -> Color(0xFF171426) to Color(0xFF9B8CFF)
    AppIcon.TEAL -> Color(0xFF06201F) to Color(0xFF4EDDD0)
    AppIcon.CORAL -> Color(0xFF241016) to Color(0xFFFF6B81)
    AppIcon.AMBER -> Color(0xFF241A0B) to Color(0xFFFFB86B)
    AppIcon.MONO -> Color(0xFF000000) to Color(0xFFFFFFFF)
}

@Composable
private fun IconSwatch(
    icon: AppIcon,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val (background, accent) = iconColors(icon)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(background)
                .border(
                    width = if (selected) 2.dp else 0.5.dp,
                    color = if (selected) accent else Outline,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "♪", fontSize = 18.sp, color = accent)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = icon.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else TextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun SwatchCell(
    style: WallpaperStyle,
    selected: Boolean,
    hasImage: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(14.dp))
            .background(wallpaperSwatchBrush(style))
            .border(
                width = if (selected) 1.6.dp else 0.5.dp,
                color = if (selected) Violet else Outline,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Violet),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(13.dp),
                )
            }
        } else if (style == WallpaperStyle.CUSTOM && !hasImage) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun ActionButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !enabled -> Surface2.copy(alpha = 0.5f)
                    primary -> Violet
                    else -> Surface2
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when {
                !enabled -> TextMuted
                primary -> Ink
                else -> TextPrimary
            },
            textAlign = TextAlign.Center,
        )
    }
}
