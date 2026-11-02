package com.norr.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.norr.music.data.Playlist
import com.norr.music.data.PlaylistsUiState
import com.norr.music.data.Track
import com.norr.music.ui.components.NorrFab
import com.norr.music.ui.theme.Aqua
import com.norr.music.ui.theme.Coral
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet
import com.norr.music.ui.theme.VioletDeep

@Composable
fun PlaylistsScreen(
    state: PlaylistsUiState,
    tracksForPaths: (List<String>) -> List<Track>,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSmart: (Boolean) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
) {
    val favorites = remember(state.favorites) { tracksForPaths(state.favorites.toList()) }
    val recent = remember(state.recent) { tracksForPaths(state.recent) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "title") {
                Column {
                    Text(
                        text = "BỘ SƯU TẬP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Aqua,
                        letterSpacing = 2.4.sp,
                    )
                    Text(
                        text = "Danh sách",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Chia nhạc theo tâm trạng, thể loại hoặc bất cứ thứ gì bạn muốn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            item(key = "smart") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmartCard(
                        title = "Yêu thích",
                        count = favorites.size,
                        icon = { Icon(Icons.Rounded.Favorite, null, tint = Coral, modifier = Modifier.size(20.dp)) },
                        colors = listOf(Coral.copy(alpha = 0.22f), Surface1),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSmart(true) },
                        onPlay = { onPlayAll(favorites) },
                    )
                    SmartCard(
                        title = "Nghe gần đây",
                        count = recent.size,
                        icon = { Icon(Icons.Rounded.History, null, tint = Aqua, modifier = Modifier.size(20.dp)) },
                        colors = listOf(Aqua.copy(alpha = 0.18f), Surface1),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSmart(false) },
                        onPlay = { onPlayAll(recent) },
                    )
                }
            }

            item(key = "label") {
                Text(
                    text = "Danh sách của bạn",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
            }

            if (state.playlists.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Surface1)
                            .padding(20.dp),
                    ) {
                        Text(
                            text = "Chưa có danh sách nào",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Bấm nút + để tạo danh sách đầu tiên, rồi thêm bài từ thư viện.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                val rows = state.playlists.chunked(2)
                items(rows.size, key = { "row${it}" }) { rowIndex ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows[rowIndex].forEach { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpen(playlist.id) },
                            )
                        }
                        if (rows[rowIndex].size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        NorrFab(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Tạo danh sách", tint = Ink)
        }
    }
}

@Composable
private fun SmartCard(
    title: String,
    count: Int,
    icon: @Composable () -> Unit,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlay: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = TextPrimary, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "$count bài",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Violet.copy(alpha = 0.30f), VioletDeep.copy(alpha = 0.14f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = Violet,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = TextPrimary,
        )
        Text(
            text = "${playlist.trackCount} bài",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }
}
