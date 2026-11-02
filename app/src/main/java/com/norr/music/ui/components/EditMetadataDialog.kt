package com.norr.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.norr.music.R
import com.norr.music.data.Track
import com.norr.music.ui.theme.Ink
import com.norr.music.ui.theme.Stroke
import com.norr.music.ui.theme.Surface1
import com.norr.music.ui.theme.Surface2
import com.norr.music.ui.theme.TextMuted
import com.norr.music.ui.theme.TextPrimary
import com.norr.music.ui.theme.Violet

/**
 * Sửa tay tên bài, nghệ sĩ và album.
 *
 * Cần thiết vì các nguồn tra cứu tự động không biết những bản remix hiếm — người dùng
 * phải tự gõ. Với file MP3, thông tin được ghi vào tag; với M4A, file được đổi tên.
 */
@Composable
fun EditMetadataDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: (title: String, artist: String, album: String) -> Unit,
) {
    var title by remember(track.id) { mutableStateOf(track.title.orEmpty()) }
    var artist by remember(track.id) {
        mutableStateOf(track.artist?.takeIf { it != "<unknown>" }.orEmpty())
    }
    var album by remember(track.id) { mutableStateOf(track.album.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(stringResource(R.string.edit_metadata_title), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = track.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                FieldLabel(stringResource(R.string.edit_field_title))
                MetadataField(
                    value = title,
                    placeholder = stringResource(R.string.edit_field_title_hint),
                    onChange = { title = it },
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(stringResource(R.string.edit_field_artist))
                MetadataField(
                    value = artist,
                    placeholder = stringResource(R.string.edit_field_artist_hint),
                    onChange = { artist = it },
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(stringResource(R.string.edit_field_album))
                MetadataField(
                    value = album,
                    placeholder = stringResource(R.string.edit_field_album_hint),
                    onChange = { album = it },
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Bỏ trống phần nào thì phần đó giữ nguyên. File M4A sẽ được đổi tên " +
                        "thành “Nghệ sĩ - Tên bài.m4a”.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), artist.trim(), album.trim()) },
                enabled = title.isNotBlank() || artist.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Violet,
                    contentColor = Ink,
                ),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextMuted) }
        },
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun MetadataField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface2,
            unfocusedContainerColor = Surface2,
            focusedBorderColor = Violet.copy(alpha = 0.6f),
            unfocusedBorderColor = Stroke,
            cursorColor = Violet,
        ),
    )
}
