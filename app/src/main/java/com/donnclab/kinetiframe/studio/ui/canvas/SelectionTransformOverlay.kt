package com.donnclab.kinetiframe.studio.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donnclab.kinetiframe.studio.SelectionInteractionMode

@Composable
fun SelectionHUD(transform: SelectionTransformMath.TransformInfo, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(8.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pos: ", color = Color.Gray, fontSize = 10.sp)
                Text(
                    "${transform.translation.x.toInt()}, ${transform.translation.y.toInt()}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scale: ", color = Color.Gray, fontSize = 10.sp)
                Text(
                    "${(transform.scale.x * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Angle: ", color = Color.Gray, fontSize = 10.sp)
                Text(
                    "${transform.rotation.toInt()}°",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * A single gesture state machine for the selection.
 * Follows the pointer directly; cancellation restores the gesture-start snapshot.
 * Absolute mapping ensures zero drift and pinpoint accuracy.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SelectionTransformOverlay(
    bounds: Rect,
    transformState: SelectionTransformState,
    canvasMatrixValues: FloatArray,
    activeStroke: ActiveStroke?,
    fitScale: Float,
    gestureScale: Float,
    onTapInside: () -> Unit,
    onTapOutside: () -> Unit,
    onDragEnd: () -> Unit = {},
    lockAspectRatio: Boolean = false,
    skewMode: Boolean = false,
    interactionMode: SelectionInteractionMode = SelectionInteractionMode.TRANSFORM,
) {
    var targetTouchDoc by remember { mutableStateOf<Offset?>(null) }
    val aspectLock by rememberUpdatedState(lockAspectRatio)
    val skew by rememberUpdatedState(skewMode)
    val interactionScale = gestureScale / androidx.compose.ui.platform.LocalDensity.current.density
    val viewport = remember(canvasMatrixValues) {
        Matrix().apply {
            values[0] = canvasMatrixValues[0]
            values[4] = canvasMatrixValues[1]
            values[12] = canvasMatrixValues[2]
            values[1] = canvasMatrixValues[3]
            values[5] = canvasMatrixValues[4]
            values[13] = canvasMatrixValues[5]
        }
    }
    val inverseViewport = remember(viewport) { Matrix(viewport.values.copyOf()).apply { invert() } }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            withTransform({ transform(viewport) }) {
                val visualMatrix = transformState.matrix
                val corners = corners(bounds, visualMatrix)
                val sides = sidePoints(bounds, visualMatrix)
                val pivotDoc = visualMatrix.map(bounds.center + transformState.pivotOffset)
                val rotationHandle = rotationHandle(bounds, visualMatrix, interactionScale)
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f / interactionScale, 10f / interactionScale), 0f)
                val strokeWidthPx = 2f / interactionScale

                // Draw Box
                corners.forEachIndexed { index, point ->
                    drawLine(
                        Color(0xFF4285F4),
                        point * fitScale,
                        corners[(index + 1) % 4] * fitScale,
                        strokeWidthPx,
                        pathEffect = dash,
                    )
                }

                if (interactionMode == SelectionInteractionMode.MOVE) {
                    moveHandle(visualMatrix.map(bounds.center) * fitScale, interactionScale)
                } else {
                    // Draw Handles
                    val outer = 12f / interactionScale
                    val inner = 8f / interactionScale
                    corners.forEach { handle(it * fitScale, outer, inner) }
                    sides.forEach { handle(it * fitScale, outer * .8f, inner * .8f) }

                    // Draw Rotation Handle
                    drawLine(Color(0xFF4285F4), sides[0] * fitScale, rotationHandle * fitScale, strokeWidthPx)
                    handle(rotationHandle * fitScale, outer, inner)

                    // Draw Tilt Line (Dashed line from pivot to touch point when rotating)
                    if (transformState.isTransforming && transformState.activeHandle == 5) {
                        targetTouchDoc?.let { touch ->
                            drawLine(
                                color = Color(0xFF4285F4).copy(alpha = 0.5f),
                                start = pivotDoc * fitScale,
                                end = touch * fitScale,
                                strokeWidth = strokeWidthPx,
                                pathEffect = dash,
                            )
                        }
                    }

                    // Draw Pivot
                    drawCircle(Color.White, 12f / interactionScale, pivotDoc * fitScale)
                    drawCircle(
                        Color(0xFF4285F4),
                        8f / interactionScale,
                        pivotDoc * fitScale,
                        style = Stroke(2f / interactionScale),
                    )
                    drawCircle(Color(0xFF4285F4), 3f / interactionScale, pivotDoc * fitScale)
                }
            }
        }

        if (transformState.isTransforming) {
            val info = SelectionTransformMath.getTransformInfo(transformState.matrix)
            SelectionHUD(
                transform = info,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
            )
        }

        Box(
            Modifier.fillMaxSize()
                .pointerInput(transformState, bounds, canvasMatrixValues, gestureScale, interactionMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startTouchDoc = inverseViewport.map(down.position)

                        val handle = nearestHandle(
                            startTouchDoc,
                            bounds,
                            transformState.matrix,
                            transformState.pivotOffset,
                            interactionScale,
                            interactionMode,
                        )

                        val inside = containsSelectionQuad(corners(bounds, transformState.matrix), startTouchDoc)
                        val startMatrix = Matrix(transformState.matrix.values.copyOf())
                        val startPivot = transformState.pivotOffset
                        val inverseSelection = Matrix(startMatrix.values.copyOf()).apply { invert() }
                        transformState.activeHandle = handle
                        transformState.isTransforming = handle != -1 || inside
                        targetTouchDoc = startTouchDoc
                        var dragTriggered = false
                        // Direct absolute mapping gives exact endpoints without a lagging ticker.
                        val completed = try {
                            drag(down.id) { change ->
                                if (handle != -1 || inside) {
                                    change.consume()
                                    val touchDoc = inverseViewport.map(change.position)
                                    targetTouchDoc = touchDoc
                                    if (handle == 4) {
                                        // Preserve the initial contact offset: touching the ring must
                                        // not snap its center under the finger/stylus and cause a jump.
                                        transformState.pivotOffset = startPivot +
                                            (inverseSelection.map(touchDoc) - inverseSelection.map(startTouchDoc))
                                    } else {
                                        transformState.matrix = SelectionTransformMath.compute(
                                            startMatrix, bounds, handle, startTouchDoc, touchDoc,
                                            gestureScale, startMatrix.map(bounds.center + startPivot), aspectLock, skew,
                                        )
                                    }
                                    activeStroke?.invalidate()
                                    dragTriggered = true
                                }
                            }
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            transformState.matrix = startMatrix
                            transformState.pivotOffset = startPivot
                            targetTouchDoc = null
                            transformState.activeHandle = -1
                            transformState.isTransforming = false
                            throw cancelled
                        }
                        if (!completed) {
                            transformState.matrix = startMatrix
                            transformState.pivotOffset = startPivot
                        } else if (dragTriggered) {
                            onDragEnd()
                        } else if (handle == -1) {
                            if (inside) onTapInside() else onTapOutside()
                        }

                        targetTouchDoc = null
                        transformState.activeHandle = -1
                        transformState.isTransforming = false
                    }
                },
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.handle(point: Offset, outer: Float, inner: Float) {
    drawCircle(Color.White, outer, point)
    drawCircle(Color(0xFF4285F4), inner, point)
}

private fun corners(bounds: Rect, matrix: Matrix) = listOf(
    matrix.map(Offset(bounds.left, bounds.top)),
    matrix.map(Offset(bounds.right, bounds.top)),
    matrix.map(Offset(bounds.right, bounds.bottom)),
    matrix.map(Offset(bounds.left, bounds.bottom)),
)

private fun sidePoints(bounds: Rect, matrix: Matrix) = listOf(
    matrix.map(Offset(bounds.center.x, bounds.top)),
    matrix.map(Offset(bounds.right, bounds.center.y)),
    matrix.map(Offset(bounds.center.x, bounds.bottom)),
    matrix.map(Offset(bounds.left, bounds.center.y)),
)

private fun nearestHandle(
    pointDoc: Offset,
    bounds: Rect,
    matrix: Matrix,
    pivotOffset: Offset,
    gestureScale: Float,
    mode: SelectionInteractionMode,
): Int {
    val pivotDoc = matrix.map(bounds.center + pivotOffset)
    val rotationHandleDoc = rotationHandle(bounds, matrix, gestureScale)
    val cornersDoc = corners(bounds, matrix)
    val sidesDoc = sidePoints(bounds, matrix)

    // Choose the nearest handle, not the first overlapping hit area.
    val handles = cornersDoc.mapIndexed { index, point -> index to point } +
        sidesDoc.mapIndexed { index, point -> index + 6 to point } +
        listOf(5 to rotationHandleDoc)
    return SelectionHandleHitTest.resolve(
        pointDoc,
        handles,
        pivotDoc,
        matrix.map(bounds.center),
        mode,
        gestureScale,
    )
}

/** Constant-size four-way arrows replace the pivot while move-only mode is selected. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.moveHandle(center: Offset, scale: Float) {
    val unit = 1f / scale.coerceAtLeast(.001f)
    drawCircle(Color.White, 15f * unit, center)
    val color = Color(0xFF4285F4)
    val directions = listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f))
    directions.forEach { direction ->
        val end = center + direction * (11f * unit)
        val perpendicular = Offset(-direction.y, direction.x)
        drawLine(color, center, end, 2f * unit)
        drawLine(color, end, end - direction * (4f * unit) + perpendicular * (4f * unit), 2f * unit)
        drawLine(color, end, end - direction * (4f * unit) - perpendicular * (4f * unit), 2f * unit)
    }
}

internal fun containsSelectionQuad(points: List<Offset>, point: Offset): Boolean {
    if (points.size < 3) return false
    var sign = 0f
    for (index in points.indices) {
        val a = points[index]
        val b = points[(index + 1) % points.size]
        val cross = (b.x - a.x) * (point.y - a.y) - (b.y - a.y) * (point.x - a.x)
        if (cross != 0f) {
            if (sign == 0f) {
                sign = cross
            } else if (sign * cross < 0f) {
                return false
            }
        }
    }
    return sign != 0f
}

private fun rotationHandle(bounds: Rect, matrix: Matrix, scale: Float): Offset {
    val top = matrix.map(Offset(bounds.center.x, bounds.top))
    val outward = top - matrix.map(bounds.center)
    val distance = outward.getDistance()
    val direction = if (distance > .001f) outward / distance else Offset(0f, -1f)
    return top + direction * (48f / scale.coerceAtLeast(.001f))
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun SelectionTransformOverlayPreview() {
    SelectionTransformOverlay(
        bounds = Rect(80f, 60f, 280f, 220f),
        transformState = remember { SelectionTransformState() },
        canvasMatrixValues = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
        activeStroke = null,
        fitScale = 1f,
        gestureScale = 1f,
        onTapInside = {},
        onTapOutside = {},
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun SelectionMoveOverlayPreview() {
    SelectionTransformOverlay(
        bounds = Rect(180f, 120f, 188f, 128f),
        transformState = remember { SelectionTransformState() },
        canvasMatrixValues = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
        activeStroke = null, fitScale = 1f, gestureScale = 1f,
        onTapInside = {}, onTapOutside = {}, interactionMode = SelectionInteractionMode.MOVE,
    )
}
