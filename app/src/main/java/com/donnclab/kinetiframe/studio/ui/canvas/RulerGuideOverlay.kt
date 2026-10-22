package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Matrix
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.donnclab.kinetiframe.project.model.DocumentCoordinateTransform
import com.donnclab.kinetiframe.project.model.StageDimensions
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Renders and hit-tests guides in document space through the artwork's exact matrix. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RulerGuideOverlay(
    guide: RulerGuide,
    stage: StageDimensions,
    canvasMatrix: Matrix,
    canvasMatrixValues: FloatArray,
    onGuideCommitted: (RulerGuide) -> Unit,
) {
    val coordinateTransform = remember(stage, canvasMatrixValues) {
        DocumentCoordinateTransform(stage, canvasMatrixValues)
    }
    val scale = remember(canvasMatrix) { matrixScale(canvasMatrix).coerceAtLeast(0.001f) }
    var handle by remember { mutableIntStateOf(-1) }
    var lastDoc by remember { mutableStateOf(Offset.Zero) }
    var lastGuideLocal by remember { mutableStateOf(Offset.Zero) }
    var rotationStartDegrees by remember { mutableStateOf(0f) }
    var rotationStartPointerAngle by remember { mutableStateOf(0f) }
    var transient by remember(guide) { mutableStateOf(guide) }
    val color = MaterialTheme.colorScheme.primary.toArgb()
    Canvas(
        Modifier.fillMaxSize().pointerInteropFilter { event ->
            val documentPoint = coordinateTransform.toDocument(Offset(event.x, event.y))
                ?: return@pointerInteropFilter false
            if (!coordinateTransform.contains(documentPoint) && event.actionMasked == MotionEvent.ACTION_DOWN) {
                return@pointerInteropFilter false
            }
            val guidePoint = documentPoint.toGuideLocal(transient)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (transient.locked) {
                        false
                    } else {
                        handle = guideHandle(guidePoint, transient, 42f / scale, 48f / scale)
                        lastDoc = documentPoint
                        lastGuideLocal = guidePoint
                        if (handle == transient.rotationHandleIndex) {
                            rotationStartDegrees = transient.rotation
                            rotationStartPointerAngle = documentPoint.angleAround(transient.center)
                        }
                        handle >= 0
                    }
                }

                MotionEvent.ACTION_MOVE -> if (handle >= 0) {
                    transient = if (handle == transient.rotationHandleIndex) {
                        transient.copy(
                            rotation =
                            rotationStartDegrees + documentPoint.angleAround(transient.center) -
                                rotationStartPointerAngle,
                        )
                    } else {
                        val delta = if (handle ==
                            handles(transient, 48f / scale).lastIndex
                        ) {
                            documentPoint - lastDoc
                        } else {
                            guidePoint -
                                lastGuideLocal
                        }
                        moveGuide(transient, handle, delta)
                    }
                    lastDoc = documentPoint
                    lastGuideLocal = guidePoint
                    true
                } else {
                    false
                }

                MotionEvent.ACTION_UP -> if (handle >= 0) {
                    handle = -1
                    onGuideCommitted(transient)
                    true
                } else {
                    false
                }

                MotionEvent.ACTION_CANCEL -> {
                    handle = -1
                    transient = guide
                    true
                }

                else -> false
            }
        },
    ) {
        drawIntoCanvas { target ->
            val canvas = target.nativeCanvas
            canvas.save()
            canvas.concat(canvasMatrix)
            if (transient.mode == RulerMode.CIRCLE || transient.mode == RulerMode.BOX) {
                canvas.rotate(transient.rotation, transient.center.x, transient.center.y)
            }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f / scale
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f / scale, 8f / scale), 0f)
            }
            when (transient.mode) {
                RulerMode.LINE, RulerMode.MIRROR -> canvas.drawLine(
                    transient.start.x,
                    transient.start.y,
                    transient.end.x,
                    transient.end.y,
                    paint,
                )

                RulerMode.CIRCLE -> canvas.drawOval(
                    transient.center.x - transient.radius.x,
                    transient.center.y - transient.radius.y,
                    transient.center.x + transient.radius.x,
                    transient.center.y + transient.radius.y,
                    paint,
                )

                RulerMode.BOX -> canvas.drawRect(
                    transient.center.x - transient.radius.x,
                    transient.center.y - transient.radius.y,
                    transient.center.x + transient.radius.x,
                    transient.center.y + transient.radius.y,
                    paint,
                )

                RulerMode.NONE -> Unit
            }
            drawGuideHandles(canvas, transient, color, 7f / scale, 48f / scale)
            canvas.restore()
        }
    }
}

private fun guideHandle(point: Offset, guide: RulerGuide, radius: Float, rotationOffset: Float): Int =
    handles(guide, rotationOffset).indexOfFirst {
        (
            it -
                point
            ).getDistance() <=
            radius
    }

private fun handles(guide: RulerGuide, rotationOffset: Float = 0f): List<Offset> = when (guide.mode) {
    RulerMode.LINE, RulerMode.MIRROR -> listOf(guide.start, guide.end, guide.center)

    RulerMode.CIRCLE, RulerMode.BOX -> listOf(
        Offset(guide.center.x - guide.radius.x, guide.center.y),
        Offset(guide.center.x + guide.radius.x, guide.center.y),
        Offset(guide.center.x, guide.center.y - guide.radius.y),
        Offset(
            guide.center.x,
            guide.center.y + guide.radius.y,
        ),
        Offset(guide.center.x, guide.center.y - guide.radius.y - rotationOffset),
        guide.center,
    )

    RulerMode.NONE -> emptyList()
}

private fun drawGuideHandles(
    canvas: android.graphics.Canvas,
    guide: RulerGuide,
    color: Int,
    radius: Float,
    rotationOffset: Float,
) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val points = handles(guide, rotationOffset)
    points.forEachIndexed { index, point ->
        if (index ==
            points.lastIndex
        ) {
            canvas.drawRect(point.x - radius, point.y - radius, point.x + radius, point.y + radius, paint)
        } else {
            canvas.drawCircle(point.x, point.y, radius, paint)
        }
    }
}

private fun matrixScale(matrix: Matrix): Float {
    val values = FloatArray(9)
    matrix.getValues(values)
    return sqrt(values[Matrix.MSCALE_X] * values[Matrix.MSCALE_X] + values[Matrix.MSKEW_Y] * values[Matrix.MSKEW_Y])
}

/** Converts a document point to the guide's unrotated local drawing coordinates. */
private fun Offset.toGuideLocal(guide: RulerGuide): Offset {
    if (guide.mode != RulerMode.CIRCLE && guide.mode != RulerMode.BOX) return this
    val radians = Math.toRadians(-guide.rotation.toDouble())
    val cosine = cos(radians).toFloat()
    val sine = sin(radians).toFloat()
    val dx = x - guide.center.x
    val dy = y - guide.center.y
    return Offset(guide.center.x + dx * cosine - dy * sine, guide.center.y + dx * sine + dy * cosine)
}

private val RulerGuide.rotationHandleIndex: Int
    get() = if (mode == RulerMode.CIRCLE || mode == RulerMode.BOX) 4 else -1

private fun Offset.angleAround(center: Offset): Float =
    Math.toDegrees(atan2(y - center.y, x - center.x).toDouble()).toFloat()
