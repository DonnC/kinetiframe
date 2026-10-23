package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.actors.rig.model.PivotPlacement
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.document.StageDimensions

/**
 * Temporary rig-setup surface for one extracted cutout.
 *
 * The bitmap remains owned by [RigAssetManager]; this composable only reads it. Pointer
 * coordinates are converted through the same stage transform used by drawing input.
 */
@Composable
fun PivotPlacementOverlay(
    placement: PivotPlacement,
    bitmap: Bitmap?,
    stage: StageDimensions,
    canvasMatrixValues: FloatArray,
    onPivotMoved: (x: Float, y: Float) -> Unit,
    onConfirmed: () -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transform = remember(stage, canvasMatrixValues) {
        DocumentCoordinateTransform(stage, canvasMatrixValues)
    }
    val density = LocalDensity.current
    val actionColor = MaterialTheme.colorScheme.primary
    val cancelColor = MaterialTheme.colorScheme.error
    val pivotRadius = with(density) { 14.dp.toPx() }
    val hitRadius = with(density) { 44.dp.toPx() }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val outerPivotPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.White.toArgb() } }
    val innerPivotPaint = remember(actionColor) { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = actionColor.toArgb() } }
    val matrix = remember(canvasMatrixValues) {
        android.graphics.Matrix().apply { setValues(canvasMatrixValues) }
    }

    Canvas(
        modifier = modifier.pointerInput(placement, canvasMatrixValues) {
            var dragging = false
            detectDragGestures(
                onDragStart = { viewportPoint ->
                    val documentPoint = transform.toDocument(viewportPoint) ?: return@detectDragGestures
                    val pivotViewport = transform.toViewport(
                        Offset(placement.documentLeft + placement.pivotX, placement.documentTop + placement.pivotY),
                    )
                    dragging = (viewportPoint - pivotViewport).getDistance() <= hitRadius
                    if (dragging) {
                        onPivotMoved(
                            documentPoint.x - placement.documentLeft,
                            documentPoint.y - placement.documentTop,
                        )
                    }
                },
                onDrag = { change, _ ->
                    if (!dragging) return@detectDragGestures
                    change.consume()
                    transform.toDocument(change.position)?.let { documentPoint ->
                        onPivotMoved(
                            documentPoint.x - placement.documentLeft,
                            documentPoint.y - placement.documentTop,
                        )
                    }
                },
                onDragEnd = { dragging = false },
                onDragCancel = { dragging = false },
            )
        },
    ) {
        drawIntoCanvas { target ->
            val native = target.nativeCanvas
            native.save()
            native.concat(matrix)
            bitmap?.let { cutout ->
                if (!cutout.isRecycled) {
                    native.drawBitmap(cutout, placement.documentLeft, placement.documentTop, paint)
                }
            }
            val pivot = Offset(
                placement.documentLeft + placement.pivotX,
                placement.documentTop + placement.pivotY,
            )
            val scale = pivotMatrixScale(matrix).coerceAtLeast(0.001f)
            val radius = pivotRadius / scale
            native.drawCircle(pivot.x, pivot.y, radius, outerPivotPaint)
            native.drawCircle(pivot.x, pivot.y, radius * 0.65f, innerPivotPaint)
            native.restore()
        }
    }

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FloatingActionButton(
            onClick = onConfirmed,
            containerColor = actionColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Lucide.Check, contentDescription = "Confirm pivot")
        }
        FloatingActionButton(
            onClick = onCancelled,
            containerColor = cancelColor,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Icon(Lucide.X, contentDescription = "Cancel pivot")
        }
    }
}

private fun pivotMatrixScale(matrix: android.graphics.Matrix): Float {
    val values = FloatArray(9)
    matrix.getValues(values)
    return kotlin.math.sqrt(
        values[android.graphics.Matrix.MSCALE_X] * values[android.graphics.Matrix.MSCALE_X] +
            values[android.graphics.Matrix.MSKEW_Y] * values[android.graphics.Matrix.MSKEW_Y],
    )
}

@Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun PivotPlacementOverlayPreview() {
    PivotPlacementOverlay(
        placement = PivotPlacement("preview", 160, 120, 80f, 60f, 160f, 120f),
        bitmap = null,
        stage = StageDimensions.LandscapeHd,
        canvasMatrixValues = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
        onPivotMoved = { _, _ -> },
        onConfirmed = {},
        onCancelled = {},
        modifier = Modifier,
    )
}
