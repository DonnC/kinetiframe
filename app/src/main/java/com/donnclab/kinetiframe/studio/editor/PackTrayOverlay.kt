// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.library.model.PackTrayItem
import com.donnclab.kinetiframe.library.ui.packs.PackDragEvent
import com.donnclab.kinetiframe.library.ui.packs.PackTray
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import kotlin.math.roundToInt

/**
 * The open pack tray and the drawing being dragged out of it. Drag positions are window pixels,
 * measured against [canvasRootPositionInWindow], the Box that holds the canvas.
 */
@Stable
internal class PackTrayState {
    /** Set by tapping a pack in the library picker. Null when the tray is closed. */
    var manifest by mutableStateOf<LibraryItemManifest?>(null)
    var items by mutableStateOf<List<PackTrayItem>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var dragItem by mutableStateOf<PackTrayItem?>(null)
    var dragPosition by mutableStateOf<Offset?>(null)
    var canvasRootPositionInWindow by mutableStateOf(Offset.Zero)

    suspend fun load(manifest: LibraryItemManifest, storage: EditorStorageViewModel, projectId: String) {
        loading = true
        error = null
        items = emptyList()
        val result = storage.loadPackTray(manifest, projectId)
        loading = false
        result.fold(onSuccess = { items = it }, onFailure = { error = it.message ?: "Could not open this pack" })
    }

    fun close() {
        manifest = null
        items = emptyList()
    }

    fun cancelDrag() {
        dragItem = null
        dragPosition = null
    }

    /**
     * Turns the finished drag's last position into a stage-space stamp. It inverts the canvas's
     * pan and zoom matrix so the drawing lands under the finger at any zoom. A drop outside the
     * stage does nothing: missing the canvas isn't worth an error.
     */
    fun drop(state: StudioState): StudioIntent? {
        val item = dragItem
        val position = dragPosition
        cancelDrag()
        if (item == null || position == null) return null
        val local = position - canvasRootPositionInWindow
        val forward = Matrix().apply { setValues(state.canvas.matrixValues) }
        val inverse = Matrix()
        if (!forward.invert(inverse)) return null
        val points = floatArrayOf(local.x, local.y)
        inverse.mapPoints(points)
        val stageX = points[0]
        val stageY = points[1]
        val stage = state.project.stage
        if (stageX < 0f || stageY < 0f || stageX > stage.width || stageY > stage.height) return null
        return StudioIntent.Selection.StampBitmap(item.thumbnail, stageX, stageY)
    }
}

/**
 * The floating tray, which stays open across drags so a mouth or eye pack can be used many
 * times, and the dragged drawing's ghost. Place it last in the canvas Box so the ghost is never
 * hidden behind the tray or the tool dock.
 */
@Composable
internal fun PackTrayOverlay(
    tray: PackTrayState,
    state: StudioState,
    visible: Boolean,
    onIntent: (StudioIntent) -> Unit,
) {
    tray.manifest?.takeIf { visible }?.let { manifest ->
        PackTray(
            packName = manifest.displayName,
            items = tray.items,
            loading = tray.loading,
            error = null,
            onDismiss = { tray.close() },
            onDrag = { event ->
                when (event) {
                    is PackDragEvent.Start -> {
                        tray.dragItem = event.item
                        tray.dragPosition = event.positionInWindow
                    }

                    is PackDragEvent.Move -> tray.dragPosition = tray.dragPosition?.plus(event.delta)

                    is PackDragEvent.End -> tray.drop(state)?.let(onIntent)

                    is PackDragEvent.Cancel -> tray.cancelDrag()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    tray.dragItem?.let { item ->
        val position = (tray.dragPosition ?: Offset.Zero) - tray.canvasRootPositionInWindow
        Image(
            bitmap = item.thumbnail.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .offset {
                    IntOffset((position.x - 32.dp.toPx()).roundToInt(), (position.y - 32.dp.toPx()).roundToInt())
                }
                .size(64.dp)
                .alpha(0.85f),
        )
    }
}

@Composable
internal fun PackTrayErrorDialog(tray: PackTrayState) {
    tray.error?.let { message ->
        MessageDialog("Pack", message) {
            tray.error = null
            tray.manifest = null
        }
    }
}
