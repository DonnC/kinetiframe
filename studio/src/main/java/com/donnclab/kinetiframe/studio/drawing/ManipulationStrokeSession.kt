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

/** Replacement pixels for one layer, never a source-over decal over the composited scene. */
data class ManipulationPreview(val bitmap: Bitmap, val left: Int, val top: Int)

/** One local working surface, grown in tiles instead of allocating on every pointer event. */
class ManipulationStrokeSession(
    private val source: Bitmap,
    private val tool: DrawingTool,
    private val baseWidth: Float,
    private val strength: Float,
    private val pending: List<ManipulationPreview> = emptyList(),
) {
    private var working: Bitmap? = null
    private var canvas: Canvas? = null
    private var left = 0
    private var top = 0
    private var rendered = 0
    private var finished = false
    private var initialized = false
    private val blur = BlurTool()
    private val smudge = SmudgeTool()
    private val sampler = ManipulationDabSampler(baseWidth)
    private val replace = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }

    fun preview(): ManipulationPreview? = working?.let { ManipulationPreview(it, left, top) }
    fun append(points: List<TouchPoint>) {
        if (finished || points.isEmpty() || source.isRecycled) return
        val end = points.lastIndex
        if (initialized && rendered >= end) return
        ensureRegion(points, rendered, end)
        val bitmap = working ?: return
        ManipulationStrokeRenderer.render(
            requireNotNull(canvas), bitmap, points, tool, baseWidth, strength,
            rendered, end, blur, smudge, sampler, left, top,
        )
        rendered = end
        initialized = true
    }
    fun finish(points: List<TouchPoint>) {
        if (!finished) {
            append(points)
            finished = true
        }
    }

    private fun ensureRegion(points: List<TouchPoint>, start: Int, end: Int) {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (i in start..end) {
            val p = points[i]
            if (!p.x.isFinite() || !p.y.isFinite()) continue
            minX = minOf(minX, p.x)
            minY = minOf(minY, p.y)
            maxX = maxOf(maxX, p.x)
            maxY = maxOf(maxY, p.y)
        }
        if (!minX.isFinite()) return
        val margin = ceil(sampler.radius * 1.5f + 8f).toInt()
        val tile = 16
        val l = (floor((minX - margin) / tile).toInt() * tile).coerceIn(0, source.width)
        val t = (floor((minY - margin) / tile).toInt() * tile).coerceIn(0, source.height)
        val r = (ceil((maxX + margin) / tile).toInt() * tile).coerceIn(0, source.width)
        val b = (ceil((maxY + margin) / tile).toInt() * tile).coerceIn(0, source.height)
        val old = working
        if (old != null && l >= left && t >= top && r <= left + old.width && b <= top + old.height) return
        val nl = if (old == null) l else minOf(l, left)
        val nt = if (old == null) t else minOf(t, top)
        val nr = if (old == null) r else maxOf(r, left + old.width)
        val nb = if (old == null) b else maxOf(b, top + old.height)
        if (nr <= nl || nb <= nt) return
        val next = Bitmap.createBitmap(nr - nl, nb - nt, Bitmap.Config.ARGB_8888)
        val target = Canvas(next)
        target.drawBitmap(source, Rect(nl, nt, nr, nb), Rect(0, 0, next.width, next.height), replace)
        for (preview in pending) {
            if (!preview.bitmap.isRecycled) {
                target.drawBitmap(preview.bitmap, (preview.left - nl).toFloat(), (preview.top - nt).toFloat(), replace)
            }
        }
        old?.let { target.drawBitmap(it, (left - nl).toFloat(), (top - nt).toFloat(), replace) }
        // A queued display list may still reference the prior surface; do not recycle it here.
        working = next
        canvas = target
        left = nl
        top = nt
    }
    fun release() {
        blur.release()
        smudge.release()
        canvas = null
    }
    fun releasePreview() {
        working = null
    }
    fun copyResult(): Bitmap = source.copy(Bitmap.Config.ARGB_8888, true).also { result ->
        val destination = Canvas(result)
        for (preview in pending) {
            if (!preview.bitmap.isRecycled) {
                destination.drawBitmap(preview.bitmap, preview.left.toFloat(), preview.top.toFloat(), replace)
            }
        }
        working?.let { destination.drawBitmap(it, left.toFloat(), top.toFloat(), replace) }
    }
}
