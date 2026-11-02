package com.norr.music.ui

import com.norr.music.R
import androidx.compose.ui.res.stringResource
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.norr.music.data.ArtworkLoader
import com.norr.music.data.CoverEditor
import com.norr.music.data.LyricLine
import com.norr.music.data.LyricsLoader
import com.norr.music.data.PlaybackUiState
import com.norr.music.data.Track
import com.norr.music.ui.theme.Coral
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet
import com.norr.music.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    playback: PlaybackUiState,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSeek: (Long) -> Unit,
    onRename: (Track) -> Unit,
    onPickCover: (Track) -> Unit,
    onRemoveCover: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    isFavorite: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit,
) {
    val track = playback.current ?: return

    // Chỉ cập nhật vị trí khi màn hình này đang mở — tiết kiệm pin và CPU.
    DisposableEffect(Unit) {
        onStartTracking()
        onDispose { onStopTracking() }
    }

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = track.id,
        key2 = track.artworkVersion,
    ) {
        value = withContext(Dispatchers.IO) { ArtworkLoader.load(track, targetPx = 1024) }
    }

    val lyrics by produceState<List<LyricLine>>(
        initialValue = emptyList(),
        key1 = track.id,
    ) {
        value = withContext(Dispatchers.IO) { LyricsLoader.load(track) }
    }

    var accent by remember(track.id, track.artworkVersion) { mutableStateOf(Violet) }
    LaunchedEffect(bitmap) {
        val image = bitmap
        accent = if (image == null) {
            Violet
        } else {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(image).generate()
                val picked = palette.getVibrantColor(0).takeIf { it != 0 }
                    ?: palette.getMutedColor(0).takeIf { it != 0 }
                    ?: 0
                if (picked != 0) Color(picked) else Violet
            }
        }
    }

    var menuOpen by remember(track.id) { mutableStateOf(false) }
    var dragPosition by remember(track.id) { mutableStateOf<Float?>(null) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    val duration = playback.durationMs.coerceAtLeast(0L)
    val position = playback.positionMs.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
    val fraction = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val sliderValue = dragPosition ?: fraction

    val lyricsAvailable = lyrics.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.34f),
                        0.42f to accent.copy(alpha = 0.08f),
                        1f to Ink,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Đóng",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                if (lyricsAvailable) {
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = if (showLyrics) "Ẩn lời bài hát" else "Hiện lời bài hát",
                            tint = if (showLyrics) accent else TextMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.Rounded.PlaylistPlay,
                        contentDescription = "Hàng đợi",
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Tuỳ chọn", tint = TextPrimary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playback_options)) },
                            leadingIcon = { Icon(Icons.Rounded.Tune, null) },
                            onClick = {
                                menuOpen = false
                                onOpenOptions()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename_file)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = {
                                menuOpen = false
                                onRename(track)
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (CoverEditor.supports(track.file)) "Đổi ảnh bìa"
                                    else "Đổi ảnh bìa (chỉ MP3)",
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Image, null) },
                            enabled = CoverEditor.supports(track.file),
                            onClick = {
                                menuOpen = false
                                onPickCover(track)
                            },
                        )
                        if (track.hasArtwork) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove_artwork)) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                                enabled = CoverEditor.supports(track.file),
                                onClick = {
                                    menuOpen = false
                                    onRemoveCover(track)
                                },
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (showLyrics && lyricsAvailable) {
                    LyricsPanel(
                        lines = lyrics,
                        positionMs = position,
                        accent = accent,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ArtworkCard(bitmap = bitmap, accent = accent)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                IconButton(onClick = { onToggleFavorite(track) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (isFavorite) Coral else TextMuted,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }

            Slider(
                value = sliderValue.coerceIn(0f, 1f),
                onValueChange = { dragPosition = it },
                onValueChangeFinished = {
                    onSeek((sliderValue * duration).toLong())
                    dragPosition = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                ),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(((sliderValue * duration).toLong()).coerceAtLeast(0L)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallControl(
                    icon = Icons.Rounded.Shuffle,
                    active = playback.shuffle,
                    contentDescription = "Phát ngẫu nhiên",
                    onClick = onToggleShuffle,
                )
                SmallControl(
                    icon = Icons.Rounded.SkipPrevious,
                    active = false,
                    contentDescription = "Bài trước",
                    onClick = onPrevious,
                    size = 32.dp,
                )

                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Tạm dừng" else "Phát",
                        tint = Ink,
                        modifier = Modifier.size(33.dp),
                    )
                }

                SmallControl(
                    icon = Icons.Rounded.SkipNext,
                    active = false,
                    contentDescription = "Bài tiếp",
                    onClick = onNext,
                    size = 32.dp,
                )
                SmallControl(
                    icon = if (playback.repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    active = playback.repeatOne,
                    contentDescription = "Lặp lại",
                    onClick = onToggleLoop,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ArtworkCard(bitmap: Bitmap?, accent: Color) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Chọn cạnh ngắn hơn để ảnh luôn vuông và không tràn ra ngoài khung.
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        Box(
            modifier = Modifier
                .size(side)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), Surface1)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(text = "♪", fontSize = 56.sp, color = TextMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * Lời bài hát cuộn theo nhạc.
 *
 * Dùng `Column` + `verticalScroll` chứ không dùng LazyColumn: màn hình phát đã nằm trong
 * một cột cuộn dọc, lồng hai thành phần cuộn cùng chiều sẽ làm Compose báo lỗi đo đạc.
 * Mỗi dòng cao cố định nên tính được vị trí cuộn chính xác mà không cần đo lại.
 */
@Composable
private fun LyricsPanel(
    lines: List<LyricLine>,
    positionMs: Long,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val lineHeight = 36.dp
    val lineHeightPx = with(density) { lineHeight.toPx() }

    val currentIndex = LyricsLoader.indexAt(lines, positionMs)

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            val target = ((currentIndex - 3).coerceAtLeast(0) * lineHeightPx).toInt()
            scrollState.animateScrollTo(target)
        }
    }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))
        lines.forEachIndexed { index, line ->
            val isCurrent = index == currentIndex
            Text(
                text = line.text.ifBlank { "♪" },
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    isCurrent -> accent
                    index < currentIndex -> TextMuted.copy(alpha = 0.45f)
                    else -> TextMuted
                },
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lineHeight)
                    .padding(horizontal = 8.dp),
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SmallControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 25.dp,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) Violet else TextMuted,
            modifier = Modifier.size(size),
        )
    }
}
