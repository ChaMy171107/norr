package com.norr.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.norr.music.ui.theme.Amber
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Outline
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet

/**
 * Màn hình hiện lỗi của lần chạy trước.
 *
 * Mục đích: người dùng chụp màn hình gửi đi là đủ để sửa, không cần adb hay máy tính.
 */
@Composable
fun CrashScreen(
    report: String,
    onCopy: () -> Unit,
    onContinue: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    // Dòng đầu tiên chứa tên lớp lỗi — hiện to để chỉ cần chụp một tấm là đủ thông tin.
    val headline = report.lineSequence()
        .firstOrNull { it.contains("Exception") || it.contains("Error") }
        ?.trim()
        ?.take(180)
        ?: "Không xác định"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        Text(
            text = "Lần chạy trước bị lỗi",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Amber.copy(alpha = 0.12f))
                .border(0.5.dp, Amber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = Amber,
                fontWeight = FontWeight.Medium,
            )
        }

        Text(
            text = "Chụp màn hình này hoặc bấm Sao chép để gửi đi. Nội dung được lưu " +
                "trong bộ nhớ riêng của app, không chia sẻ ra ngoài.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface1)
                .border(0.5.dp, Outline, RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            SelectionContainer {
                Text(
                    text = report,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CrashButton(
                label = "Sao chép lỗi",
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    clipboard.setText(AnnotatedString(report))
                    onCopy()
                },
            )
            CrashButton(
                label = "Thử mở lại",
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = onContinue,
            )
        }
    }
}

@Composable
private fun CrashButton(
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) Violet else Surface2)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (primary) Ink else TextPrimary,
        )
    }
}
