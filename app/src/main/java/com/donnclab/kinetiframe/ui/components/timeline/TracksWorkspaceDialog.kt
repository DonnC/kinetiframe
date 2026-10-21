package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.ui.TimelineIntent

/** An explicit editing workspace: opening lanes must never remeasure the drawing viewport. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TracksWorkspaceDialog(
    isPlaying: Boolean,
    onIntent: (TimelineIntent) -> Unit,
    onDismiss: () -> Unit,
    assetPicker: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxSize(),
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Asset tracks",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
                PlaybackControls(isPlaying, onIntent)
                assetPicker?.invoke()
                IconButton(onClick = onDismiss) { Icon(Lucide.X, "Close asset tracks") }
            }
            content()
        }
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Composable
private fun TracksWorkspacePreview() {
    TracksWorkspaceDialog(false, {}, {}) { Text("Track workspace", Modifier.padding(16.dp)) }
}
