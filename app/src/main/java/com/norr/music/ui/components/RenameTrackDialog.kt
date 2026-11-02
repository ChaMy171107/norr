package com.norr.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.norr.music.R
import com.norr.music.data.FileNameRules
import com.norr.music.data.Track
import com.norr.music.ui.theme.TextMuted
import java.io.File

@Composable
fun RenameTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember {
        mutableStateOf(TextFieldValue(track.fileName, selection = TextRange(0, track.file.nameWithoutExtension.length)))
    }

    val check = remember(text.text) {
        FileNameRules.validate(track.fileName, text.text) { candidate ->
            File(track.file.parentFile, candidate).exists()
        }
    }
    val unchanged = text.text.trim() == track.fileName

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.rename_current, track.fileName),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.new_name)) },
                    isError = check.error != null,
                    supportingText = {
                        Text(check.error ?: stringResource(R.string.rename_extension_hint))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { check.name?.let(onConfirm) },
                enabled = check.error == null && !unchanged,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
