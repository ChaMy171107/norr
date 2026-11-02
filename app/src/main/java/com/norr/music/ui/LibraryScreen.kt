package com.norr.music.ui

import com.norr.music.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.norr.music.data.LibraryUiState
import com.norr.music.data.PlaybackUiState
import com.norr.music.data.SortMode
import com.norr.music.data.Track
import com.norr.music.ui.components.TrackRow
import com.norr.music.ui.theme.Aqua
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Outline
import com.norr.music.ui.theme.Stroke
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface3
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet
import com.norr.music.ui.theme.VioletDeep
import com.norr.music.util.formatDuration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    library: LibraryUiState,
    playback: PlaybackUiState,
    hasStorageAccess: Boolean,
    selection: Set<String>,
    onRequestAccess: () -> Unit,
    onScan: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
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
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    favorites: Set<String>,
) {
    val listState = rememberLazyListState()
    val totalDuration = remember(library.tracks) { library.tracks.sumOf { it.durationMs } }
    val selectionMode = selection.isNotEmpty()

    var mode by rememberSaveable { mutableStateOf(BrowseMode.SONGS) }
    var openAlbum by remember { mutableStateOf<AlbumGroup?>(null) }
    var openArtist by remember { mutableStateOf<ArtistGroup?>(null) }
    var openFolder by remember { mutableStateOf<FolderGroup?>(null) }

    // Nhóm trên danh sách đã lọc, để gõ tìm kiếm thì cả ba chế độ duyệt cùng thu hẹp theo.
    val albums = remember(library.visible) { groupAlbums(library.visible) }
    val artists = remember(library.visible) { groupArtists(library.visible) }
    val folders = remember(library.visible) { groupFolders(library.visible) }

    val detailTracks = openAlbum?.tracks ?: openArtist?.tracks ?: openFolder?.tracks
    if (detailTracks != null) {
        val detailTitle = openAlbum?.name ?: openArtist?.name ?: openFolder?.name.orEmpty()
        val detailSubtitle = when {
            openAlbum != null -> openAlbum!!.artist.ifBlank { "${detailTracks.size} bài" }
            openArtist != null -> "${detailTracks.size} bài"
            else -> openFolder?.path.orEmpty()
        }
        GroupDetailList(
            title = detailTitle,
            subtitle = detailSubtitle,
            tracks = detailTracks,
            playback = playback,
            selection = selection,
            favorites = favorites,
            onBack = {
                openAlbum = null
                openArtist = null
                openFolder = null
            },
            onPlay = onPlay,
            onRename = onRename,
            onCover = onCover,
            onRemoveCover = onRemoveCover,
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylist = onAddToPlaylist,
            onPlayNext = onPlayNext,
            onEditMetadata = onEditMetadata,
            onLongPress = onLongPress,
            onToggleSelection = onToggleSelection,
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "header", contentType = "header") {
            AnimatedVisibility(
                visible = !selectionMode,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
                Header(
                    trackCount = library.tracks.size,
                    totalDurationMs = totalDuration,
                    scanning = library.scanning,
                    canScan = hasStorageAccess,
                    onScan = onScan,
                )
            }
            AnimatedVisibility(
                visible = selectionMode,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
                SelectionHeader(
                    count = selection.size,
                    onSelectAll = onSelectAll,
                    onClear = onClearSelection,
                )
            }
        }

        if (!selectionMode) {
            item(key = "search", contentType = "search") {
                OutlinedTextField(
                    value = library.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    placeholder = { Text(stringResource(R.string.library_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        if (library.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Xoá tìm kiếm",
                                    tint = TextMuted,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Surface1,
                        unfocusedContainerColor = Surface1,
                        focusedBorderColor = Violet.copy(alpha = 0.6f),
                        unfocusedBorderColor = Outline,
                        cursorColor = Violet,
                    ),
                )
            }

            item(key = "mode", contentType = "mode") {
                BrowseModeRow(current = mode, onSelect = { mode = it })
            }

            if (mode == BrowseMode.SONGS) item(key = "sort", contentType = "sort") {
                // FlowRow thay cho Row: màn hình 384dp (như Samsung A035F) không đủ chỗ cho
                // 4 chip trên một hàng, chip cuối bị xuống dòng và tràn khỏi khung.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SortMode.entries.forEach { mode ->
                        val selected = library.sort == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onSortChange(mode) },
                            label = {
                                Text(mode.label, maxLines = 1, softWrap = false)
                            },
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
        }

        if (library.scanning && library.progressTotal > 0) {
            item(key = "progress", contentType = "progress") {
                val fraction by animateFloatAsState(
                    targetValue = library.progress.toFloat() / library.progressTotal.toFloat(),
                    animationSpec = tween(240),
                    label = "scanProgress",
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface1)
                        .border(0.5.dp, Outline, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        text = "Đang cập nhật ${library.progress}/${library.progressTotal} tệp",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                        color = Violet,
                        trackColor = Surface3,
                    )
                }
            }
        }

        if (!hasStorageAccess && !selectionMode) {
            item(key = "permission", contentType = "permission") {
                PermissionCard(onRequestAccess = onRequestAccess)
            }
        }

        library.error?.let { message ->
            item(key = "error", contentType = "error") {
                ErrorCard(message = message, onRetry = onScan)
            }
        }

        if (library.loading && library.tracks.isEmpty()) {
            item(key = "loading", contentType = "loading") { ScanningCard() }
        }

        if (hasStorageAccess && library.visible.isEmpty() && !library.loading && !library.scanning) {
            item(key = "empty", contentType = "empty") {
                EmptyState(
                    query = library.query,
                    hasScanned = library.scanned,
                    onScan = onScan,
                )
            }
        }

        when (mode) {
            BrowseMode.SONGS -> items(
                items = library.visible,
                key = { it.id },
                contentType = { "track" },
            ) { track ->
            val isCurrent = playback.current?.id == track.id
            TrackRow(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isCurrent && playback.isPlaying,
                isFavorite = track.id in favorites,
                selected = track.id in selection,
                selectionMode = selectionMode,
                onClick = {
                    if (selectionMode) onToggleSelection(track) else onPlay(track, library.visible)
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

            BrowseMode.ALBUMS -> items(albums, key = { it.name }) { album ->
                GroupRow(
                    title = album.name,
                    subtitle = album.artist.ifBlank { "${album.tracks.size} bài" },
                    count = album.tracks.size,
                    cover = album.cover,
                    onClick = { openAlbum = album },
                )
            }

            BrowseMode.ARTISTS -> items(artists, key = { it.name }) { artist ->
                GroupRow(
                    title = artist.name,
                    subtitle = "${artist.tracks.size} bài",
                    count = artist.tracks.size,
                    cover = null,
                    icon = {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Violet,
                            modifier = Modifier.size(26.dp),
                        )
                    },
                    onClick = { openArtist = artist },
                )
            }

            BrowseMode.FOLDERS -> items(folders, key = { it.path }) { folder ->
                GroupRow(
                    title = folder.name,
                    subtitle = folder.path,
                    count = folder.tracks.size,
                    cover = null,
                    icon = {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = Aqua,
                            modifier = Modifier.size(26.dp),
                        )
                    },
                    onClick = { openFolder = folder },
                )
            }
        }
    }
}

@Composable
private fun Header(
    trackCount: Int,
    totalDurationMs: Long,
    scanning: Boolean,
    canScan: Boolean,
    onScan: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Violet, VioletDeep))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NORR MUSIC",
                    style = MaterialTheme.typography.labelSmall,
                    color = Aqua,
                    letterSpacing = 2.6.sp,
                )
                Text(
                    text = "Thư viện",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
            }

            IconButton(onClick = onScan, enabled = canScan && !scanning) {
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Violet,
                    )
                } else {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Quét lại thư viện",
                        tint = TextMuted,
                    )
                }
            }
        }

        Text(
            text = buildString {
                append("$trackCount bài hát")
                if (totalDurationMs > 0) append(" · ${formatDuration(totalDurationMs)}")
                append("  ·  nhấn giữ để chọn nhiều")
            },
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SelectionHeader(
    count: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Violet.copy(alpha = 0.12f))
            .border(0.5.dp, Violet.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Checklist, contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Đã chọn $count",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Chọn tất cả",
            style = MaterialTheme.typography.labelMedium,
            color = Violet,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onSelectAll)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Bỏ chọn",
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(onRequestAccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface1)
            .border(0.5.dp, Outline, RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Aqua.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = Aqua)
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.need_storage_permission), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Để phát nhạc, đổi tên file và đổi ảnh bìa trên máy hoặc thẻ SD, " +
                "Norr Music cần quyền “Truy cập tất cả file”.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRequestAccess,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
        ) {
            Text(stringResource(R.string.grant_permission), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(0.5.dp, Outline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun ScanningCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(0.5.dp, Outline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.5.dp,
            color = Violet,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.scanning_library), style = MaterialTheme.typography.titleSmall)
            Text(
                "Lần đầu sẽ lâu hơn, các lần sau gần như tức thì.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun EmptyState(query: String, hasScanned: Boolean, onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Violet.copy(alpha = 0.22f), Aqua.copy(alpha = 0.10f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = Violet,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (query.isNotBlank()) "Không có kết quả" else "Chưa có bài hát nào",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (query.isNotBlank()) {
                "Thử từ khoá khác hoặc xoá bộ lọc tìm kiếm."
            } else {
                "Nhạc trong bộ nhớ máy và thẻ SD sẽ xuất hiện ở đây sau khi quét."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
        if (query.isBlank() && hasScanned) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onScan,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
            ) {
                Text(stringResource(R.string.rescan))
            }
        }
    }
}
