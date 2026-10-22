package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import com.donnclab.kinetiframe.project.model.DrawCommand
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.TextNode
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.studio.EraserType
import com.donnclab.kinetiframe.studio.drawing.BrushRenderers
import com.donnclab.kinetiframe.studio.drawing.BrushSnapshot
import com.donnclab.kinetiframe.studio.drawing.ManipulationPreview
import com.donnclab.kinetiframe.studio.drawing.ManipulationStrokeSession
import com.donnclab.kinetiframe.studio.drawing.PressureStrokeRenderer
import com.donnclab.kinetiframe.studio.drawing.StrokeGeometry
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

fun drawStrokeHardware(canvas: Canvas, command: DrawCommand) {
    if (command.points.isEmpty()) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = command.color
        alpha = (command.alpha * 255).toInt().coerceIn(0, 255)
        style = Paint.Style.STROKE
        strokeWidth = command.width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    BrushRenderers.draw(canvas, command.points, command.width, paint, command.brush)
}

fun drawSelectedStroke(canvas: Canvas, command: DrawCommand, transform: Matrix) {
    if (command.points.isEmpty()) return

    val androidMatrix = android.graphics.Matrix()
    val v = transform.values

    // Fixed column-major to row-major mapping for 3x3 projection
    androidMatrix.setValues(
        floatArrayOf(
            v[0], v[4], v[12],
            v[1], v[5], v[13],
            v[3], v[7], v[15],
        ),
    )

    canvas.save()
    canvas.concat(androidMatrix)
    drawStrokeHardware(canvas, command)
    canvas.restore()
}

fun textHit(
    node: TextNode,
    point: Offset,
    density: Float,
    fontRegistry: com.donnclab.kinetiframe.studio.drawing.FontRegistry?,
): Boolean {
    val typeface = fontRegistry?.availableFonts?.find { it.fileName == node.fontFileName }?.typeface
        ?: android.graphics.Typeface.DEFAULT

    val textBounds = com.donnclab.kinetiframe.studio.drawing.MultilineTextRenderer.getBounds(node, typeface, density)

    val halfW = textBounds.width / 2f
    val halfH = textBounds.height / 2f

    // Transform point to local node space (reverse rotation and scale)
    if (!node.isVisible) return false
    val inverse = android.graphics.Matrix().apply { setValues(node.selectionTransform.androidValues()) }
    if (!inverse.invert(inverse)) return false
    val untransformed = floatArrayOf(point.x, point.y)
    inverse.mapPoints(untransformed)
    val local = rotateVector(Offset(untransformed[0], untransformed[1]) - Offset(node.x, node.y), -node.rotation)

    if (kotlin.math.abs(node.scaleX) < .0001f || kotlin.math.abs(node.scaleY) < .0001f) return false
    val hitX = local.x / node.scaleX
    val hitY = local.y / node.scaleY

    return hitX in (-halfW)..(halfW) && hitY in (-halfH)..(halfH)
}

fun rotateVector(value: Offset, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    val cosine = kotlin.math.cos(radians).toFloat()
    val sine = kotlin.math.sin(radians).toFloat()
    return Offset(
        value.x * cosine - value.y * sine,
        value.x * sine + value.y * cosine,
    )
}

fun drawVariableWidthStroke(
    canvas: Canvas,
    stroke: ActiveStroke,
    baseWidth: Float,
    paint: Paint,
    activeTool: DrawingTool = DrawingTool.BRUSH,
    brush: BrushSnapshot = BrushSnapshot(),
    sourceBitmap: Bitmap? = null,
    strength: Float = 1f,
) {
    drawVariableWidthStroke(
        canvas = canvas,
        points = stroke.points,
        predictedPoint = stroke.predictedTouchPoint,
        baseWidth = baseWidth,
        paint = paint,
        activeTool = activeTool,
        brush = brush,
        sourceBitmap = sourceBitmap,
        strength = strength,
    )
}

fun drawVariableWidthStroke(
    canvas: Canvas,
    points: List<TouchPoint>,
    predictedPoint: TouchPoint? = null,
    baseWidth: Float,
    paint: Paint,
    activeTool: DrawingTool = DrawingTool.BRUSH,
    brush: BrushSnapshot = BrushSnapshot(),
    sourceBitmap: Bitmap? = null,
    strength: Float = 1f,
) {
    if (points.isEmpty()) return

    val allPoints = if (predictedPoint != null) {
        points + predictedPoint
    } else {
        points
    }

    if (activeTool != DrawingTool.BLUR && activeTool != DrawingTool.SMUDGE) {
        BrushRenderers.draw(canvas, allPoints, baseWidth, paint, brush)
        return
    }
    // Manipulation is deliberately not replayed here. This helper is called by Compose draw
    // code, where scratch allocation and raster mutation cause jank. The gesture-scoped
    // ManipulationStrokeSession supplies the precomputed preview instead.
}

/** A completed live stroke kept onscreen until the identical baked bitmap is available. */
data class SettledStroke(
    val points: List<TouchPoint>,
    val tool: DrawingTool,
    val eraserType: EraserType,
    val width: Float,
    val strength: Float,
    val paint: Paint,
    val sourceBitmap: Bitmap?,
    val brush: BrushSnapshot,
    /** A pre-rasterized manipulation delta. It is never replayed from a Compose draw callback. */
    val manipulationPreview: ManipulationPreview? = null,
)

class ActiveStroke {
    init {
        PressureStrokeRenderer.prepareThread()
        com.donnclab.kinetiframe.studio.drawing.PenStrokeRenderer.prepareThread()
    }
    var isStylusContact: Boolean = true
    val points = mutableListOf<TouchPoint>()
    val mirroredPoints = mutableListOf<TouchPoint>()
    var predictedTouchPoint: TouchPoint? = null
    var predictedMirroredTouchPoint: TouchPoint? = null
    var drawTrigger by mutableStateOf(0L)
    var settledStrokes by mutableStateOf<List<SettledStroke>>(emptyList())
        private set

    private val penInput = com.donnclab.kinetiframe.studio.drawing.NaturalStrokeInput()
    private var pressureSensitive = false
    private var inputDensity = 1f
    private var lastFilterSample: TouchPoint? = null
    private var lastAcceptedTimestamp: Long? = null
    private var lastVelocitySample: TouchPoint? = null
    private var filteredPressure: Float? = null
    private val penGeometry = com.donnclab.kinetiframe.studio.drawing.IncrementalPenGeometry()

    // Manipulation is prepared from pointer events, never from Canvas.draw. The session owns a
    // mutable active-layer working surface and an overlay delta for this one gesture.
    private var manipulationSession: ManipulationStrokeSession? = null

    fun configureInput(displayDensity: Float = 1f, pressureSensitive: Boolean = false) {
        inputDensity = if (displayDensity.isFinite()) displayDensity.coerceIn(0.75f, 4f) else 1f
        this.pressureSensitive = pressureSensitive
        penInput.reset(inputDensity)
    }

    fun appendSample(
        sample: TouchPoint,
        screenSample: TouchPoint = sample,
        constraint: ((TouchPoint) -> TouchPoint)? = null,
    ): TouchPoint? {
        // Historical batches should be ordered.  Reject duplicates/backwards samples rather
        // than allowing a negative interval to inject a discontinuity into the filter.
        if (lastAcceptedTimestamp?.let { sample.timestamp <= it } == true) return null
        if (manipulationSession == null) {
            val prepared = penInput.update(sample, screenSample, isStylusContact) ?: return null
            val physical = requireNotNull(penInput.screenPoint)
            val previous = lastFilterSample
            val changed =
                previous == null || hypot(physical.x - previous.x, physical.y - previous.y) >= 0.12f * inputDensity ||
                    (pressureSensitive && kotlin.math.abs(physical.pressure - previous.pressure) >= 0.004f)
            if (!changed) return null
            lastAcceptedTimestamp = sample.timestamp
            lastFilterSample = physical
            val constrained = constraint?.invoke(prepared) ?: prepared
            points.add(constrained)
            return constrained
        }
        val lastFilter = lastFilterSample

        // Blur/smudge keep their established distance-sampled manipulation response.
        val contactPressure = sample.pressure
        val physicalSample = screenSample.copy(
            pressure = contactPressure,
            tilt = sample.tilt,
            orientation = sample.orientation,
        )
        if (!com.donnclab.kinetiframe.studio.drawing.InputFilter.adaptiveThreshold(
                physicalSample,
                lastFilter,
                considerDynamics = false,
            )
        ) {
            return null
        }
        lastFilterSample = physicalSample

        lastAcceptedTimestamp = sample.timestamp
        val velocitySample = sample
        val velocity = lastVelocitySample?.let { previous ->
            val seconds = ((sample.timestamp - previous.timestamp) / 1000f).coerceAtLeast(0.001f)
            hypot(velocitySample.x - previous.x, velocitySample.y - previous.y) / seconds
        } ?: 0f
        lastVelocitySample = velocitySample
        val smoothPressure = filteredPressure?.let { it + (contactPressure.coerceIn(0f, 1f) - it) * 0.35f }
            ?: contactPressure.coerceIn(0f, 1f)
        filteredPressure = smoothPressure
        val prepared = sample.copy(pressure = smoothPressure, velocity = velocity)
        val constrained = constraint?.invoke(prepared) ?: prepared
        points.add(constrained)
        manipulationSession?.append(points)
        return constrained
    }

    /**
     * Processes the final contact with the same causal filter used while drawing.
     * A smoothed brush must not teleport to raw coordinates on lift.
     */
    fun flushSample(
        sample: TouchPoint,
        constraint: ((TouchPoint) -> TouchPoint)? = null,
        screenSample: TouchPoint = sample,
    ): TouchPoint {
        // Lift events may report zero force. Keep the last real contact's width and speed.
        val previous = points.lastOrNull()
        if (manipulationSession == null) {
            // Keep final intentional travel, but never append a raw lift wobble/zero-force tip.
            return appendSample(
                sample.copy(pressure = previous?.pressure ?: sample.pressure),
                screenSample,
                constraint,
            ) ?: previous ?: sample
        }
        val flushed = if (manipulationSession != null) {
            val velocity = lastVelocitySample?.let { prior ->
                hypot(sample.x - prior.x, sample.y - prior.y) /
                    ((sample.timestamp - prior.timestamp) / 1000f).coerceAtLeast(0.001f)
            } ?: 0f
            sample.copy(pressure = sample.pressure.coerceIn(0f, 1f), velocity = velocity)
        } else {
            sample
        }
        val constrained = constraint?.invoke(flushed) ?: flushed
        points.add(constrained)
        manipulationSession?.append(points)
        return constrained
    }

    fun appendMirroredSample(sample: TouchPoint) {
        mirroredPoints.add(sample)
    }

    fun invalidate() {
        drawTrigger++
    }

    /**
     * Retains immutable midpoint quadratics and replaces only the final half-segment.
     * Strokes the combined native path once, preserving alpha at intersections.
     */
    fun drawIncrementalPen(canvas: Canvas, baseWidth: Float, paint: Paint) {
        penGeometry.draw(canvas, points, baseWidth, paint)
    }

    fun beginManipulation(source: Bitmap, tool: DrawingTool, baseWidth: Float, strength: Float) {
        check(tool == DrawingTool.BLUR || tool == DrawingTool.SMUDGE)
        discardManipulation()
        manipulationSession = ManipulationStrokeSession(
            source,
            tool,
            baseWidth,
            strength,
            settledStrokes.mapNotNull { it.manipulationPreview },
        )
    }

    fun manipulationPreview(): ManipulationPreview? = manipulationSession?.preview()

    fun settle(
        points: List<TouchPoint>,
        tool: DrawingTool,
        eraserType: EraserType,
        width: Float,
        strength: Float,
        paint: Paint,
        sourceBitmap: Bitmap?,
        brush: BrushSnapshot,
    ) {
        if (points.isEmpty()) return
        manipulationSession?.finish(points)
        val preview = manipulationSession?.preview()
        manipulationSession?.release()
        manipulationSession = null
        settledStrokes = settledStrokes + SettledStroke(
            points = ArrayList(points),
            tool = tool,
            eraserType = eraserType,
            width = width,
            strength = strength,
            paint = Paint(paint),
            sourceBitmap = sourceBitmap,
            brush = brush,
            manipulationPreview = preview,
        )
        invalidate()
    }

    /** Unrelated bitmap updates cannot dismiss pending ink. */
    fun acknowledgeSettledStrokes(seeds: Set<Long>) {
        val remaining = settledStrokes.filterNot { it.brush.seed in seeds }
        if (remaining.size != settledStrokes.size) {
            settledStrokes = remaining
            invalidate()
        }
    }

    fun clearSettledStrokes() {
        if (settledStrokes.isNotEmpty()) {
            settledStrokes = emptyList()
            invalidate()
        }
    }

    private fun discardManipulation() {
        manipulationSession?.let { session ->
            session.release()
            session.releasePreview()
        }
        manipulationSession = null
    }

    fun reset() {
        points.clear()
        lastFilterSample = null
        lastAcceptedTimestamp = null
        lastVelocitySample = null
        filteredPressure = null
        penInput.reset(inputDensity)
        predictedTouchPoint = null
        predictedMirroredTouchPoint = null
        mirroredPoints.clear()
        penGeometry.reset()
        discardManipulation()
        invalidate()
    }
}
