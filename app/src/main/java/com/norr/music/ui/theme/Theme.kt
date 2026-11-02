package com.norr.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NorrColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color(0xFF120F1F),
    primaryContainer = Color(0xFF2A2450),
    onPrimaryContainer = Color(0xFFDCD6FF),
    secondary = Aqua,
    onSecondary = Color(0xFF022420),
    secondaryContainer = Color(0xFF123B38),
    onSecondaryContainer = Color(0xFFB5FFF6),
    tertiary = Coral,
    onTertiary = Color(0xFF2A0007),
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    error = Coral,
    onError = Color(0xFF2A0007),
)

private val NorrShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun NorrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NorrColors,
        typography = NorrTypography,
        shapes = NorrShapes,
        content = content,
    )
}
