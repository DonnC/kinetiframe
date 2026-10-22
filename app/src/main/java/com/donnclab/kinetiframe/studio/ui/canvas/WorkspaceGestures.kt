package com.donnclab.kinetiframe.studio.ui.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEach
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType

/** Shared navigation policy: two fingers pan/zoom/rotate; stylus plus palm stays an edit. */
suspend fun PointerInputScope.workspaceGestures(
    touchNavigationEnabled: Boolean = true,
    consumeAllInput: Boolean = false,
    onNavigation: () -> Unit,
    onTransform: (Offset, Float, Float, Offset) -> Unit,
) {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (touchNavigationEnabled && ViewportGesturePolicy.shouldTransform(
                    event.changes.count { it.pressed },
                    event.changes.any { it.pressed && it.type == PointerType.Stylus },
                )
            ) {
                onNavigation()
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val rotation = event.calculateRotation()
                if (zoom != 1f || pan != Offset.Zero || rotation != 0f) {
                    onTransform(pan, zoom, rotation, event.calculateCentroid(useCurrent = true))
                }
                event.changes.forEach { it.consume() }
            }
            // Read-only previews still observe transforms before swallowing editing input.
            if (consumeAllInput) event.changes.forEach { it.consume() }
            if (event.changes.all { !it.pressed }) break
        }
    }
}
