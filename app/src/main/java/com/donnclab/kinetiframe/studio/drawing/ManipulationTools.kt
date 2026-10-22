package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.TouchPoint
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
    private val pixels = ManipulationPixels()
    private var input = IntArray(0)
    private var output = IntArray(0)
    private var patch: Bitmap? = null
    private val sourceRect = Rect()
    private val destinationRect = Rect()
    private val replace = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }

    fun apply(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, dx: Float, dy: Float, radius: Float, amount: Float) {
        if (bitmap.isRecycled || amount <= 0f) return
        val kernel = ceil(radius * 0.12f).toInt().coerceAtLeast(1)
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
        pixels.apply(input, output, width, height, x - left, y - top, radius, dx, dy, amount, blur, kernel)
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

object ManipulationStrokeRenderer {
    fun render(
        canvas: Canvas,
        bitmap: Bitmap,
        points: List<TouchPoint>,
        tool: DrawingTool,
        baseWidth: Float,
        strength: Float,
        startSourceSegment: Int = 0,
        endExclusiveSourceSegment: Int = points.lastIndex,
        blurTool: BlurTool? = null,
        smudgeTool: SmudgeTool? = null,
        sampler: ManipulationDabSampler = ManipulationDabSampler(baseWidth),
        originX: Int = 0,
        originY: Int = 0,
    ) {
        require(tool == DrawingTool.BLUR || tool == DrawingTool.SMUDGE)
        if (points.isEmpty() || !baseWidth.isFinite() || !strength.isFinite() || strength <= 0f) return
        val owned = if (tool == DrawingTool.BLUR) blurTool == null else smudgeTool == null
        val engine = if (tool == DrawingTool.BLUR) blurTool ?: BlurTool() else smudgeTool ?: SmudgeTool()
        try {
            if (tool == DrawingTool.BLUR &&
                startSourceSegment == 0
            ) {
                sampler.begin(points.first()) { x, y, dx, dy, pressure ->
                    engine.apply(
                        canvas,
                        bitmap,
                        x - originX,
                        y - originY,
                        dx,
                        dy,
                        sampler.radius,
                        strength.coerceIn(0f, 1f) * (0.35f + pressure * 0.65f),
                    )
                }
            }
            for (index in startSourceSegment.coerceAtLeast(0) until
                endExclusiveSourceSegment.coerceAtMost(points.lastIndex)) {
                sampler.segment(points[index], points[index + 1]) { x, y, dx, dy, pressure ->
                    engine.apply(
                        canvas,
                        bitmap,
                        x - originX,
                        y - originY,
                        dx,
                        dy,
                        sampler.radius,
                        strength.coerceIn(0f, 1f) * (0.35f + pressure * 0.65f),
                    )
                }
            }
        } finally {
            if (owned) engine.release()
        }
    }
}
