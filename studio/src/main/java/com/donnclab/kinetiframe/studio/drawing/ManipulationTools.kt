package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/** Fixed defaults for new strokes; strength stays in the engine for history replay. */
object ManipulationDefaults {
    const val SMUDGE = 0.82f
    const val BLUR = 0.72f
    fun strength(tool: DrawingTool) = if (tool == DrawingTool.SMUDGE) SMUDGE else BLUR
}

/** Brush-local reusable buffers: no full-frame readback or bitmap allocation per dab. */
abstract class BaseManipulationTool(private val blur: Boolean) {
    private val pixels = ManipulationPixels(blur)
    private val local = ManipulationDab()
    private var input = IntArray(0)
    private var output = IntArray(0)
    private var patch: Bitmap? = null
    private val sourceRect = Rect()
    private val destinationRect = Rect()
    private val replace = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }

    /** Applies [dab], positioned in [bitmap]'s pixels, through [canvas]. */
    fun apply(canvas: Canvas, bitmap: Bitmap, dab: ManipulationDab) {
        val x = dab.x
        val y = dab.y
        val dx = dab.dx
        val dy = dab.dy
        val radius = dab.radius
        val amount = dab.amount
        if (bitmap.isRecycled || amount <= 0f) return
        val kernel = ManipulationPixels.kernelFor(radius)
        // Three box passes need three radii of halo, otherwise a cropped brush leaves seams.
        val halo = if (blur) kernel * 3 + 1 else ceil(hypot(dx, dy)).toInt() + 2
        val left = floor(x - radius - halo).toInt().coerceIn(0, bitmap.width)
        val top = floor(y - radius - halo).toInt().coerceIn(0, bitmap.height)
        val right = ceil(x + radius + halo).toInt().coerceIn(0, bitmap.width)
        val bottom = ceil(y + radius + halo).toInt().coerceIn(0, bitmap.height)
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return
        val count = width * height
        if (input.size < count) {
            input = IntArray(count)
            output = IntArray(count)
        }
        bitmap.getPixels(input, 0, width, left, top, width, height)
        pixels.apply(input, output, width, height, local.set(x - left, y - top, dx, dy, radius, amount))
        val current = patch
        val target = if (current == null || current.width < width || current.height < height) {
            current?.recycle()
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { patch = it }
        } else {
            current
        }
        target.setPixels(output, 0, width, 0, 0, width, height)
        sourceRect.set(0, 0, width, height)
        destinationRect.set(left, top, right, bottom)
        // Replace edited alpha too. SRC_OVER accumulates opacity and cannot soften an opaque edge.
        canvas.drawBitmap(target, sourceRect, destinationRect, replace)
    }

    fun release() {
        patch?.recycle()
        patch = null
        input = IntArray(0)
        output = IntArray(0)
        pixels.clear()
    }
}

class BlurTool : BaseManipulationTool(true)
class SmudgeTool : BaseManipulationTool(false)

/** A blur or smudge brush: which tool, its base width and its strength. */
data class ManipulationBrush(val tool: DrawingTool, val baseWidth: Float, val strength: Float)

/**
 * How [ManipulationStrokeRenderer] replays a stroke: the source segments to draw (all by default),
 * engines to reuse, the dab sampler to continue, and where the target bitmap sits in stroke space.
 */
data class ManipulationRenderOptions(
    val startSourceSegment: Int = 0,
    /** Null draws to the stroke's last segment. */
    val endExclusiveSourceSegment: Int? = null,
    val blurTool: BlurTool? = null,
    val smudgeTool: SmudgeTool? = null,
    /** Null starts a fresh sampler at the brush's base width. */
    val sampler: ManipulationDabSampler? = null,
    val originX: Int = 0,
    val originY: Int = 0,
)

object ManipulationStrokeRenderer {
    fun render(
        canvas: Canvas,
        bitmap: Bitmap,
        points: List<TouchPoint>,
        brush: ManipulationBrush,
        options: ManipulationRenderOptions = ManipulationRenderOptions(),
    ) {
        val (tool, baseWidth, strength) = brush
        val (startSourceSegment, endSegment, blurTool, smudgeTool, givenSampler, originX, originY) = options
        val endExclusiveSourceSegment = endSegment ?: points.lastIndex
        val sampler = givenSampler ?: ManipulationDabSampler(baseWidth)
        val dab = ManipulationDab()
        require(tool == DrawingTool.BLUR || tool == DrawingTool.SMUDGE)
        if (points.isEmpty() || !baseWidth.isFinite() || !strength.isFinite() || strength <= 0f) return
        val owned = if (tool == DrawingTool.BLUR) blurTool == null else smudgeTool == null
        val engine = if (tool == DrawingTool.BLUR) blurTool ?: BlurTool() else smudgeTool ?: SmudgeTool()
        try {
            if (tool == DrawingTool.BLUR &&
                startSourceSegment == 0
            ) {
                sampler.begin(points.first()) { x, y, dx, dy, pressure ->
                    val amount = strength.coerceIn(0f, 1f) * (0.35f + pressure * 0.65f)
                    engine.apply(canvas, bitmap, dab.set(x - originX, y - originY, dx, dy, sampler.radius, amount))
                }
            }
            for (index in startSourceSegment.coerceAtLeast(0) until
                endExclusiveSourceSegment.coerceAtMost(points.lastIndex)) {
                sampler.segment(points[index], points[index + 1]) { x, y, dx, dy, pressure ->
                    val amount = strength.coerceIn(0f, 1f) * (0.35f + pressure * 0.65f)
                    engine.apply(canvas, bitmap, dab.set(x - originX, y - originY, dx, dy, sampler.radius, amount))
                }
            }
        } finally {
            if (owned) engine.release()
        }
    }
}
