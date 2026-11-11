package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.CompactTextField

@Composable
fun HoldDrawingDialog(firstFrame: Int, totalFrames: Int, onDismiss: () -> Unit, onHold: (Int) -> Unit) {
    var end by remember(firstFrame, totalFrames) { mutableStateOf(totalFrames.toString()) }
    val last = end.toIntOrNull()?.minus(1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.hold_drawing_title), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.hold_drawing_from_frame, firstFrame + 1),
                    style = MaterialTheme.typography.labelMedium,
                )
                CompactTextField(end, {
                    end = it.filter(Char::isDigit).take(4)
                }, singleLine = true, label = { Text(stringResource(R.string.hold_drawing_last_frame)) })
                Text(
                    stringResource(R.string.hold_drawing_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            IconButton(
                enabled = last != null && last > firstFrame && last < totalFrames,
                onClick = { last?.let(onHold) },
            ) { Icon(Icons.Default.Check, stringResource(R.string.hold_drawing_confirm)) }
        },
        dismissButton = {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun LinkedDrawingScopeDialog(
    linkedFrames: Int,
    onDismiss: () -> Unit,
    onLocal: () -> Unit,
    onShared: () -> Unit,
    onHold: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                pluralStringResource(R.plurals.linked_drawing_title, linkedFrames, linkedFrames),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.linked_drawing_prompt), style = MaterialTheme.typography.bodySmall)
                // Scope labels are essential decisions, not optional toolbar captions.
                TextButton(onClick = onLocal) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.linked_drawing_this_frame),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                TextButton(onClick = onShared) {
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.linked_drawing_all_frames),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        confirmButton = {
            IconButton(onClick = onHold) {
                Icon(Icons.Default.Link, stringResource(R.string.linked_drawing_hold_another))
            }
        },
        dismissButton = {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.action_cancel)) }
        },
    )
}

@Preview
@Composable
private fun HeldDrawingPreview() {
    MaterialTheme { HoldDrawingDialog(0, 24, {}, {}) }
}

@Preview
@Composable
private fun LinkedDrawingScopePreview() {
    MaterialTheme { LinkedDrawingScopeDialog(24, {}, {}, {}, {}) }
}
