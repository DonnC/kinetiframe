package com.donnclab.kinetiframe.ui.components.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.components.canvas.workspaceGestures

/** Block artwork edits in audience coordinates, but retain two-finger workspace navigation.
 * Fitted camera dialogs own their viewport and must never mutate the main editor's transform. */
@Composable
internal fun CameraPreviewInputLayer(
    navigationEnabled: Boolean,
    onIntent: (TimelineIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestIntent by rememberUpdatedState(onIntent)
    Box(
        modifier.fillMaxSize().pointerInput(navigationEnabled) {
            workspaceGestures(
                touchNavigationEnabled = navigationEnabled,
                consumeAllInput = true,
                onNavigation = {},
                onTransform = { pan, zoom, rotation, centre ->
                    latestIntent(TimelineIntent.View.UpdateTransform(pan, zoom, rotation, centre))
                },
            )
        },
    )
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun CameraPreviewInputLayerPreview() {
    CameraPreviewInputLayer(navigationEnabled = true, onIntent = {})
}
