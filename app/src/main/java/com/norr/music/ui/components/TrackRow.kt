package com.norr.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.res.stringResource
import com.norr.music.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.norr.music.data.CoverEditor
import com.norr.music.data.Track
import com.norr.music.ui.theme.Coral
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.Violet
import com.norr.music.util.formatDuration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onEditMetadata: () -> Unit,
    modifier: Modifier = Modifier,
    showMenu: Boolean = true,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    var menuOpen by remember(track.id) { mutableStateOf(false) }

    val background by animateColorAsState(
        targetValue = when {
            selected -> Violet.copy(alpha = 0.14f)
            isCurrent -> Surface2
            else -> Surface2.copy(alpha = 0f)
        },
        animationSpec = tween(160),
        label = "rowBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            TrackArtwork(
                track = track,
                modifier = Modifier.size(50.dp),
                cornerRadius = 14.dp,
                iconSize = 22.dp,
            )
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Ink.copy(alpha = if (selected) 0.55f else 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (selected) Violet else Ink.copy(alpha = 0.6f))
                            .border(
                                width = 1.5.dp,
                                color = if (selected) Violet else TextMuted,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.displayTitle,
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    selected -> Violet
                    isCurrent -> Violet
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isCurrent || selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!selectionMode && isFavorite) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "Yêu thích",
                tint = Coral,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
        }

        if (isCurrent && isPlaying && !selectionMode) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Đang phát",
                tint = Violet,
                modifier = Modifier.size(18.dp),
            )
        } else if (!selectionMode) {
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }

        if (showMenu && !selectionMode) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Tuỳ chọn",
                        tint = TextMuted,
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (isFavorite) "Bỏ khỏi Yêu thích" else "Thêm vào Yêu thích")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Rounded.FavoriteBorder
                                },
                                contentDescription = null,
                                tint = Coral,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleFavorite()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_playlist)) },
                        onClick = {
                            menuOpen = false
                            onAddToPlaylist()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.play_next)) },
                        onClick = {
                            menuOpen = false
                            onPlayNext()
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_title_artist)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Violet) },
                        onClick = {
                            menuOpen = false
                            onEditMetadata()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename_file)) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (CoverEditor.supports(track.file))
                                    stringResource(R.string.change_artwork)
                                else
                                    stringResource(R.string.change_artwork_mp3_only),
                            )
                        },
                        enabled = CoverEditor.supports(track.file),
                        onClick = {
                            menuOpen = false
                            onChangeCover()
                        },
                    )
                    if (track.hasArtwork) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_artwork)) },
                            enabled = CoverEditor.supports(track.file),
                            onClick = {
                                menuOpen = false
                                onRemoveCover()
                            },
                        )
                    }
                }
            }
        }
    }
}
