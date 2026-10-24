package com.donnclab.kinetiframe.studio.ui.timeline

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
import androidx.compose.runtime.toString
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.presentation.common.CompactTextField

@Composable
fun HoldDrawingDialog(firstFrame: Int, totalFrames: Int, onDismiss: () -> Unit, onHold: (Int) -> Unit) {
    var end by remember(firstFrame, totalFrames) { mutableStateOf(totalFrames.toString()) }
    val last = end.toIntOrNull()?.minus(1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hold drawing", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Frame ${firstFrame + 1} →", style = MaterialTheme.typography.labelMedium)
                CompactTextField(end, {
                    end = it.filter(Char::isDigit).take(4)
                }, singleLine = true, label = { Text("Last frame") })
                Text(
                    "Replaces this layer’s drawings in the range. Other layers stay unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            IconButton(
                enabled = last != null && last > firstFrame && last < totalFrames,
                onClick = { last?.let(onHold) },
            ) { Icon(Icons.Default.Check, "Hold range") }
        },
        dismissButton = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel") } },
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
        title = { Text("Drawing · $linkedFrames linked frames", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Choose where your next edits apply.", style = MaterialTheme.typography.bodySmall)
                // Scope labels are essential decisions, not optional toolbar captions.
                TextButton(onClick = onLocal) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("This frame only", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onShared) {
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(8.dp))
                    Text("All linked frames", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = { IconButton(onClick = onHold) { Icon(Icons.Default.Link, "Hold another range") } },
        dismissButton = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel") } },
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
