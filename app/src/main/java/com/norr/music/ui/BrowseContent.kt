package com.norr.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.norr.music.data.Track
import com.norr.music.ui.components.TrackArtwork
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Outline
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet

/** Cách duyệt thư viện. */
enum class BrowseMode(val label: String) {
    SONGS("Bài hát"),
    ALBUMS("Album"),
    ARTISTS("Nghệ sĩ"),
    FOLDERS("Thư mục"),
}

data class AlbumGroup(
    val name: String,
    val artist: String,
    val tracks: List<Track>,
) {
    val cover: Track? get() = tracks.firstOrNull { it.hasArtwork } ?: tracks.firstOrNull()
}

data class ArtistGroup(val name: String, val tracks: List<Track>)

data class FolderGroup(val path: String, val name: String, val tracks: List<Track>)

private const val UNKNOWN_ALBUM = "Không rõ album"
private const val UNKNOWN_ARTIST = "Không rõ nghệ sĩ"

private fun Track.cleanArtist(): String? =
    artist?.trim()?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }

private fun Track.cleanAlbum(): String? =
    album?.trim()?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }

fun groupAlbums(tracks: List<Track>): List<AlbumGroup> =
    tracks.groupBy { it.cleanAlbum() ?: UNKNOWN_ALBUM }
        .map { (name, list) ->
            // Nghệ sĩ hiển thị cho album: lấy tên xuất hiện nhiều nhất trong album đó.
            val artist = list.mapNotNull { it.cleanArtist() }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                .orEmpty()
            AlbumGroup(name = name, artist = artist, tracks = list)
        }
        .sortedBy { it.name.lowercase() }

fun groupArtists(tracks: List<Track>): List<ArtistGroup> =
    tracks.groupBy { it.cleanArtist() ?: UNKNOWN_ARTIST }
        .map { (name, list) -> ArtistGroup(name = name, tracks = list) }
        .sortedBy { it.name.lowercase() }

fun groupFolders(tracks: List<Track>): List<FolderGroup> =
    tracks.groupBy { it.file.parent.orEmpty() }
        .map { (path, list) ->
            FolderGroup(
                path = path,
                // Dùng parentFile.name thay vì tách chuỗi theo '/', để đúng trên cả
                // Windows (dấu \) lẫn Android (dấu /).
                name = list.first().file.parentFile?.name?.ifBlank { null } ?: path,
                tracks = list,
            )
        }
        .sortedBy { it.name.lowercase() }

/** Bốn chip chọn cách duyệt. Dùng FlowRow vì màn hình 384dp không đủ một hàng. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowseModeRow(
    current: BrowseMode,
    onSelect: (BrowseMode) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseMode.entries.forEach { mode ->
            val selected = mode == current
            FilterChip(
                selected = selected,
                onClick = { onSelect(mode) },
                label = { Text(mode.label, maxLines = 1, softWrap = false) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Surface1,
                    labelColor = TextMuted,
                    selectedContainerColor = Violet.copy(alpha = 0.16f),
                    selectedLabelColor = Violet,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = Outline,
                    selectedBorderColor = Violet.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** Một dòng nhóm: ảnh bìa hoặc biểu tượng, tên, dòng phụ, số bài. */
@Composable
fun GroupRow(
    title: String,
    subtitle: String,
    count: Int,
    cover: Track?,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                icon()
            } else {
                TrackArtwork(track = cover, modifier = Modifier.fillMaxSize(), cornerRadius = 16.dp)
            }
        }

        Spacer(Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Tiêu đề của màn hình chi tiết một nhóm, có nút quay lại. */
@Composable
fun GroupDetailHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Quay lại",
                tint = TextPrimary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Danh sách bài trong một nhóm. Tái dùng TrackRow của thư viện. */
@Composable
fun GroupDetailList(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    playback: com.norr.music.data.PlaybackUiState,
    selection: Set<String>,
    favorites: Set<String>,
    onBack: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onRename: (Track) -> Unit,
    onCover: (Track) -> Unit,
    onRemoveCover: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onEditMetadata: (Track) -> Unit,
    onLongPress: (Track) -> Unit,
    onToggleSelection: (Track) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "head") {
            GroupDetailHeader(title = title, subtitle = subtitle, onBack = onBack)
        }
        items(tracks, key = { it.id }) { track ->
            val isCurrent = playback.current?.id == track.id
            com.norr.music.ui.components.TrackRow(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isCurrent && playback.isPlaying,
                isFavorite = track.id in favorites,
                selected = track.id in selection,
                selectionMode = selection.isNotEmpty(),
                onClick = {
                    if (selection.isNotEmpty()) onToggleSelection(track) else onPlay(track, tracks)
                },
                onLongClick = { onLongPress(track) },
                onRename = { onRename(track) },
                onChangeCover = { onCover(track) },
                onRemoveCover = { onRemoveCover(track) },
                onToggleFavorite = { onToggleFavorite(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onPlayNext = { onPlayNext(track) },
                onEditMetadata = { onEditMetadata(track) },
            )
        }
    }
}
