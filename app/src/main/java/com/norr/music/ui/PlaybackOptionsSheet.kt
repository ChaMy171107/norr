package com.norr.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.norr.music.data.PlaybackUiState
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Outline
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
private val SLEEP_OPTIONS = listOf(10, 20, 30, 60)

/** Bảng tuỳ chọn phát: tốc độ, hẹn giờ tắt, equalizer. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaybackOptionsSheet(
    playback: PlaybackUiState,
    onDismiss: () -> Unit,
    onSpeed: (Float) -> Unit,
    onSleep: (Int?) -> Unit,
    onEqEnabled: (Boolean) -> Unit,
    onEqPreset: (Int) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface1,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                text = "Tuỳ chọn phát",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )

            // -------------------------------------------------- tốc độ
            Spacer(Modifier.height(18.dp))
            SectionLabel("Tốc độ phát")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SPEEDS.forEach { speed ->
                    OptionChip(
                        label = if (speed == 1f) "1×" else "${speed}×",
                        selected = kotlin.math.abs(playback.speed - speed) < 0.01f,
                        onClick = { onSpeed(speed) },
                    )
                }
            }

            // -------------------------------------------------- hẹn giờ
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Hẹn giờ tắt")
                playback.sleepMinutesLeft?.let { left ->
                    Text(
                        text = "  · còn $left phút",
                        style = MaterialTheme.typography.labelSmall,
                        color = Violet,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SLEEP_OPTIONS.forEach { minutes ->
                    OptionChip(
                        label = "$minutes phút",
                        selected = playback.sleepMinutesLeft != null &&
                            playback.sleepMinutesLeft <= minutes &&
                            playback.sleepMinutesLeft > minutes - 10,
                        onClick = { onSleep(minutes) },
                    )
                }
                OptionChip(
                    label = "Tắt hẹn giờ",
                    selected = playback.sleepMinutesLeft == null,
                    onClick = { onSleep(null) },
                )
            }

            // -------------------------------------------------- equalizer
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = if (playback.eqPresets.isEmpty()) {
                            "Thiết bị không cung cấp preset"
                        } else {
                            "Chọn preset có sẵn của thiết bị"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
                Switch(
                    checked = playback.eqEnabled,
                    onCheckedChange = onEqEnabled,
                    enabled = playback.eqPresets.isNotEmpty(),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink,
                        checkedTrackColor = Violet,
                        uncheckedTrackColor = Surface2,
                        uncheckedBorderColor = Outline,
                    ),
                )
            }

            if (playback.eqPresets.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    playback.eqPresets.forEachIndexed { index, name ->
                        OptionChip(
                            label = name,
                            selected = playback.eqPreset == index,
                            onClick = { onEqPreset(index) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, softWrap = false) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Surface2,
            labelColor = TextMuted,
            selectedContainerColor = Violet.copy(alpha = 0.18f),
            selectedLabelColor = Violet,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Outline,
            selectedBorderColor = Violet.copy(alpha = 0.45f),
        ),
    )
}
