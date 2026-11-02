package com.norr.music.ui

import com.norr.music.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.norr.music.data.ImportUiState
import com.norr.music.data.remote.JamendoSource
import com.norr.music.data.remote.MusicSource
import com.norr.music.data.remote.RemoteTrack
import com.norr.music.ui.theme.Amber
import com.norr.music.ui.theme.Aqua
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Outline
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.Surface3
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet
import com.norr.music.ui.theme.VioletDeep

@Composable
fun ImportScreen(
    state: ImportUiState,
    jamendoConfigured: Boolean,
    onSourceChange: (MusicSource) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onToggle: (RemoteTrack) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDownload: () -> Unit,
    onOpenScan: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "head") {
                Column {
                    Text(
                        text = "KHO NHẠC MỞ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Aqua,
                        letterSpacing = 2.6.sp,
                    )
                    Text(
                        text = "Tải nhạc",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Chỉ những nguồn cho phép tải mới có ở đây. Tải xong là nghe được offline.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            item(key = "scan") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface1)
                        .border(0.5.dp, Outline, RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenScan)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Violet.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = Violet,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Quét playlist YouTube",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            "Dán link để xem playlist có bài nào, bạn đã có bài nào",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            item(key = "sources") {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MusicSource.entries.forEach { source ->
                        SourceChip(
                            label = source.label,
                            selected = state.source == source,
                            modifier = Modifier.weight(1f),
                            onClick = { onSourceChange(source) },
                        )
                    }
                }
            }

            if (state.source == MusicSource.JAMENDO && !jamendoConfigured) {
                item(key = "jamendo-hint") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Amber.copy(alpha = 0.10f))
                            .border(0.5.dp, Amber.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            "Jamendo cần client_id",
                            style = MaterialTheme.typography.titleSmall,
                            color = Amber,
                        )
                        Text(
                            "Đăng ký miễn phí tại devportal.jamendo.com rồi dán vào Cài đặt. " +
                                "Trong lúc đó bạn dùng Internet Archive — không cần đăng ký gì.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.import_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        Row {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, null, tint = TextMuted)
                                }
                            }
                            IconButton(onClick = onSearch, enabled = !state.searching) {
                                if (state.searching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Violet,
                                    )
                                } else {
                                    Icon(Icons.Rounded.Search, "Tìm", tint = Violet)
                                }
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

            state.error?.let { message ->
                item(key = "error") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface1)
                            .border(0.5.dp, Outline, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onSearch) { Text(stringResource(R.string.retry)) }
                    }
                }
            }

            if (state.downloading || state.lastOutcome != null) {
                item(key = "progress") {
                    DownloadStatusCard(state = state)
                }
            }

            if (state.results.isNotEmpty()) {
                item(key = "actions") {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${state.results.size} kết quả · đã chọn ${state.selected.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (state.selected.size == state.results.size) "Bỏ chọn" else "Chọn tất cả",
                            style = MaterialTheme.typography.labelMedium,
                            color = Violet,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (state.selected.size == state.results.size) {
                                        onClearSelection()
                                    } else {
                                        onSelectAll()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (state.results.isEmpty() && !state.searching && state.error == null) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Violet.copy(alpha = 0.22f), Aqua.copy(alpha = 0.10f)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = Violet,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Tìm nhạc để tải về",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tìm theo tên bài hoặc nghệ sĩ. Với nhạc Việt, thử " +
                                "“nhac viet”, “bolero”, hoặc tên ca sĩ — Internet Archive " +
                                "có nhiều bản do cộng đồng tải lên.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            items(
                items = state.results,
                key = { it.id },
                contentType = { "remote" },
            ) { track ->
                RemoteTrackRow(
                    track = track,
                    selected = track.id in state.selected,
                    onClick = { onToggle(track) },
                )
            }
        }

        if (state.selected.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Violet)
                    .clickable(enabled = !state.downloading, onClick = onDownload)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Ink,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Đang tải ${state.completed}/${state.total}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Icon(Icons.Rounded.CloudDownload, null, tint = Ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Tải ${state.selected.size} bài",
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Violet.copy(alpha = 0.16f) else Surface1)
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = if (selected) Violet.copy(alpha = 0.5f) else Outline,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Violet else TextMuted,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RemoteTrackRow(
    track: RemoteTrack,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Violet.copy(alpha = 0.12f) else Surface2.copy(alpha = 0f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) Violet else Ink.copy(alpha = 0.5f))
                .border(
                    width = 1.5.dp,
                    color = if (selected) Violet else Outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Violet else TextPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = JamendoSource.formatDuration(track.durationSec),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }
}

@Composable
private fun DownloadStatusCard(state: ImportUiState) {
    val fraction = if (state.total > 0) {
        (state.completed + state.failed).toFloat() / state.total.toFloat()
    } else {
        0f
    }
    val animated by animateFloatAsState(fraction, tween(240), label = "dl")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(0.5.dp, Outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        if (state.downloading) {
            Text(
                text = "Đang tải ${state.completed}/${state.total}",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            if (state.currentTitle.isNotBlank()) {
                Text(
                    text = state.currentTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animated.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                color = Violet,
                trackColor = Surface3,
            )
            if (state.failed > 0) {
                Text(
                    text = "${state.failed} bài lỗi",
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            Text(
                text = state.lastOutcome.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
            )
        }
    }
}
