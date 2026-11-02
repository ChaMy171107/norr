package com.norr.music.ui

import com.norr.music.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.norr.music.data.ScanUiState
import com.norr.music.ui.theme.Amber
import com.norr.music.ui.theme.Aqua
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Outline
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet

/**
 * Quét một playlist YouTube: dán link, app đọc tên toàn bộ bài rồi đối chiếu với
 * nhạc đã có trong máy.
 *
 * Màn hình này **chỉ đọc tên bài**, không tải âm thanh — nên không có nút tải nào ở đây.
 */
@Composable
fun PlaylistScanScreen(
    state: ScanUiState,
    hasApiKey: Boolean,
    onLinkChange: (String) -> Unit,
    onScan: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = TextPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QUÉT PLAYLIST",
                    style = MaterialTheme.typography.labelSmall,
                    color = Aqua,
                    letterSpacing = 2.2.sp,
                )
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "input") {
                Column {
                    OutlinedTextField(
                        value = state.link,
                        onValueChange = onLinkChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.youtube_link_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.PlaylistPlay, null, tint = TextMuted)
                        },
                        trailingIcon = {
                            IconButton(onClick = onScan, enabled = !state.scanning) {
                                if (state.scanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Violet,
                                    )
                                } else {
                                    Icon(Icons.Rounded.Search, "Quét", tint = Violet)
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

                    Text(
                        text = "Chỉ đọc tên bài và đối chiếu với nhạc trong máy. " +
                            "Không tải âm thanh từ YouTube.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )

                    if (!hasApiKey) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Amber.copy(alpha = 0.10f))
                                .border(0.5.dp, Amber.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenSettings)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Chưa có API key — chỉ đọc được 15 bài gần nhất. " +
                                    "Nhập key trong Cài đặt để đọc trọn playlist.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = Amber,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
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
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (state.tracks.isNotEmpty()) {
                item(key = "summary") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface1)
                            .border(0.5.dp, Outline, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "${state.tracks.size} bài trong playlist",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Đã có ${state.ownedCount} bài trong máy · " +
                                "còn thiếu ${state.missingCount} bài",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.missingCount > 0) Amber else Aqua,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        if (state.limited) {
                            Text(
                                text = "Đang dùng RSS nên chỉ thấy 15 bài gần nhất. " +
                                    "Nhập API key để thấy toàn bộ.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }

                items(state.tracks, key = { it.videoId }) { track ->
                    val owned = track.videoId in state.owned
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface2.copy(alpha = 0f))
                            .clickable { onOpenUrl(track.watchUrl) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (owned) Aqua.copy(alpha = 0.18f) else Surface2)
                                .border(
                                    0.5.dp,
                                    if (owned) Aqua.copy(alpha = 0.5f) else Outline,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (owned) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Aqua,
                                    modifier = Modifier.size(14.dp),
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (owned) TextPrimary else TextPrimary.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (owned) "Đã có trong máy" else track.channel.ifBlank { "Chưa có" },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (owned) Aqua else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = "Mở trong YouTube",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }

                item(key = "hint") {
                    Text(
                        text = "Chạm vào một bài để mở trong YouTube.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            }

            if (state.tracks.isEmpty() && !state.scanning && state.error == null) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Violet.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.PlaylistPlay,
                                contentDescription = null,
                                tint = Violet,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Dán link playlist để quét",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "App sẽ đọc toàn bộ tên bài trong playlist rồi cho biết " +
                                "bạn đã có bài nào trong máy và còn thiếu bài nào.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
