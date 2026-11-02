package com.norr.music.ui

import com.norr.music.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.norr.music.data.PlaybackUiState
import com.norr.music.data.Track
import com.norr.music.ui.components.TrackRow
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet
import com.norr.music.ui.theme.VioletDeep
import com.norr.music.util.formatDuration

@Composable
fun PlaylistDetailScreen(
    detail: AppViewModel.PlaylistDetail,
    playback: PlaybackUiState,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onRemove: (Track) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val editable = detail.playlist.id > 0
    val totalMs = detail.tracks.sumOf { it.durationMs }
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Ink, Ink, Ink)),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = buildString {
                        append("${detail.tracks.size} bài")
                        if (totalMs > 0) append(" · ${formatDuration(totalMs)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            if (editable) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Tuỳ chọn", tint = TextMuted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename_playlist)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = {
                                menuOpen = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_playlist)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }

        Button(
            onClick = { if (detail.tracks.isNotEmpty()) onPlay(0) },
            enabled = detail.tracks.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.play_all), fontWeight = FontWeight.SemiBold)
        }

        if (detail.tracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .padding(20.dp),
            ) {
                Text(stringResource(R.string.playlist_empty), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(
                    "Vào thư viện, mở menu của một bài và chọn “Thêm vào danh sách”.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(detail.tracks, key = { _, t -> t.id }) { index, track ->
                    val isCurrent = playback.current?.id == track.id
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TrackRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && playback.isPlaying,
                            isFavorite = false,
                            onClick = { onPlay(index) },
                            onRename = {},
                            onChangeCover = {},
                            onRemoveCover = {},
                            onToggleFavorite = {},
                            onAddToPlaylist = {},
                            onPlayNext = {},
                            onEditMetadata = {},
                            showMenu = false,
                            modifier = Modifier.weight(1f),
                        )
                        if (editable) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onRemove(track) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Bỏ khỏi danh sách",
                                    tint = TextMuted,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
