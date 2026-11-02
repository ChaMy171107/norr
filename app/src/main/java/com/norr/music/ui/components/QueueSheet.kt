package com.norr.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.norr.music.ui.AppViewModel
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet

/**
 * Bảng trượt hiển thị hàng đợi phát: bài đang phát và các bài sắp tới.
 * Cho phép nhảy tới bài bất kỳ, đổi thứ tự bằng mũi tên, hoặc bỏ khỏi hàng đợi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    entries: List<AppViewModel.QueueEntry>,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentIndex = entries.indexOfFirst { it.isCurrent }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PlaylistPlay, null, tint = Violet, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hàng đợi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${entries.size} bài" +
                            if (currentIndex >= 0) " · đang ở bài ${currentIndex + 1}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(Modifier.padding(top = 10.dp))

            if (entries.isEmpty()) {
                Text(
                    text = "Chưa có bài nào trong hàng đợi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 22.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(entries, key = { "${it.index}-${it.title}" }) { entry ->
                        QueueRow(
                            entry = entry,
                            canMoveUp = entry.index > 0,
                            canMoveDown = entry.index < entries.lastIndex,
                            canRemove = entries.size > 1,
                            onJump = { onJump(entry.index) },
                            onUp = { onMove(entry.index, entry.index - 1) },
                            onDown = { onMove(entry.index, entry.index + 1) },
                            onRemove = { onRemove(entry.index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: AppViewModel.QueueEntry,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onJump: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (entry.isCurrent) Violet.copy(alpha = 0.12f) else Surface2.copy(alpha = 0f))
            .clickable(onClick = onJump)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.isCurrent) {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.labelSmall,
                    color = Violet,
                )
            } else {
                Text(
                    text = "${entry.index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.isCurrent) Violet else TextPrimary,
                fontWeight = if (entry.isCurrent) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.artist.isNotBlank()) {
                Text(
                    text = entry.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(onClick = onUp, enabled = canMoveUp) {
            Icon(
                Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Lên",
                tint = if (canMoveUp) TextMuted else TextMuted.copy(alpha = 0.25f),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDown, enabled = canMoveDown) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Xuống",
                tint = if (canMoveDown) TextMuted else TextMuted.copy(alpha = 0.25f),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Bỏ khỏi hàng đợi",
                tint = if (canRemove) TextMuted else TextMuted.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
