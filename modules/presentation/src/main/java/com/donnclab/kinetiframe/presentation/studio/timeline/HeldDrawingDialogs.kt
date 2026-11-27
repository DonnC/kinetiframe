package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens
import com.donnclab.kinetiframe.presentation.R

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
            Column(verticalArrangement = Arrangement.spacedBy(TimelineTokens.contextActionPadding)) {
                Text(stringResource(R.string.linked_drawing_prompt), style = MaterialTheme.typography.bodySmall)
                // Scope labels are essential decisions, not optional toolbar captions.
                TextButton(onClick = onLocal) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(Dimens.spaceS))
                    Text(
                        stringResource(R.string.linked_drawing_this_frame),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                TextButton(onClick = onShared) {
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(Dimens.spaceS))
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
private fun LinkedDrawingScopePreview() {
    MaterialTheme { LinkedDrawingScopeDialog(24, {}, {}, {}, {}) }
}
