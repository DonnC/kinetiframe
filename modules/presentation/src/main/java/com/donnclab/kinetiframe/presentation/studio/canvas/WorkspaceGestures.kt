package com.donnclab.kinetiframe.presentation.studio.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType

/** A two-finger touch this short and this still is a tap, not a pinch or a pan. */
private const val TAP_MAX_MS = 250L
private const val DOUBLE_TAP_GAP_MS = 350L

/**
 * Shared navigation policy: two fingers pan/zoom/rotate; stylus plus palm stays an edit. Two quick
 * two-finger taps call [onTwoFingerDoubleTap] (Fill and Fit); one finger would draw a dot instead.
 */
suspend fun PointerInputScope.workspaceGestures(
    touchNavigationEnabled: Boolean = true,
    consumeAllInput: Boolean = false,
    onNavigation: () -> Unit,
    onTransform: (Offset, Float, Float, Offset) -> Unit,
    onTwoFingerDoubleTap: (() -> Unit)? = null,
) {
    var lastTapAt = 0L
    awaitEachGesture {
        var startedAt = -1L
        var fingers = 0
        var travel = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val now = event.changes.maxOf { it.uptimeMillis }
            if (startedAt < 0) startedAt = now
            fingers = maxOf(fingers, event.changes.count { it.pressed && it.type == PointerType.Touch })
            travel += event.calculatePan().getDistance()
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
            if (event.changes.all { !it.pressed }) {
                val tap = fingers >= 2 && now - startedAt <= TAP_MAX_MS && travel < viewConfiguration.touchSlop
                if (tap && onTwoFingerDoubleTap != null) {
                    if (now - lastTapAt <= DOUBLE_TAP_GAP_MS) {
                        lastTapAt = 0L
                        onTwoFingerDoubleTap()
                    } else {
                        lastTapAt = now
                    }
                }
                break
            }
        }
    }
}
