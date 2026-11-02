package com.norr.music.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.norr.music.data.WallpaperStyle
import com.norr.music.ui.theme.Ink

/**
 * Nền của toàn app.
 *
 * Bốn kiểu dựng sẵn đều rất tối, vẽ bằng gradient nên không tốn bộ nhớ.
 * Kiểu "ảnh của bạn" vẽ ảnh đã chọn, phủ thêm lớp tối để chữ luôn đọc được.
 * Không dùng blur — tốn GPU trên máy yếu.
 */
@Composable
fun WallpaperBackground(
    style: WallpaperStyle,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val useImage = style == WallpaperStyle.CUSTOM && bitmap != null

    val alpha by animateFloatAsState(
        targetValue = if (useImage) 1f else 0f,
        animationSpec = tween(420),
        label = "wallpaperAlpha",
    )

    Box(modifier = modifier.fillMaxSize().background(Ink)) {
        when (style) {
            WallpaperStyle.BLACK -> Unit

            WallpaperStyle.GRAPHITE -> Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF121215),
                            0.55f to Color(0xFF050506),
                            1f to Color(0xFF000000),
                        ),
                    ),
            )

            WallpaperStyle.MIDNIGHT -> Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF0A1226),
                            0.45f to Color(0xFF05070F),
                            1f to Color(0xFF000000),
                        ),
                    ),
            )

            WallpaperStyle.PLUM -> Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1B0F2B), Color(0xFF000000)),
                            center = Offset(200f, 1400f),
                            radius = 1600f,
                        ),
                    ),
            )

            WallpaperStyle.CUSTOM -> {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = alpha,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.66f),
                                    0.45f to Color.Black.copy(alpha = 0.78f),
                                    1f to Color.Black.copy(alpha = 0.92f),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

/** Màu đại diện cho từng kiểu nền, dùng vẽ ô chọn trong cài đặt. */
fun wallpaperSwatchBrush(style: WallpaperStyle): Brush = when (style) {
    WallpaperStyle.BLACK -> Brush.linearGradient(
        listOf(Color(0xFF0A0A0A), Color(0xFF000000)),
    )
    WallpaperStyle.GRAPHITE -> Brush.linearGradient(
        listOf(Color(0xFF34343C), Color(0xFF000000)),
    )
    WallpaperStyle.MIDNIGHT -> Brush.linearGradient(
        listOf(Color(0xFF1B2C5C), Color(0xFF000000)),
    )
    WallpaperStyle.PLUM -> Brush.linearGradient(
        listOf(Color(0xFF4A2470), Color(0xFF000000)),
    )
    WallpaperStyle.CUSTOM -> Brush.linearGradient(
        listOf(Color(0xFF5A5A66), Color(0xFF141418)),
    )
}
