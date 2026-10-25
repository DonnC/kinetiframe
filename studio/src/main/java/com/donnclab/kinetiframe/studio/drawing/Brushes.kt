package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Paint
import com.donnclab.kinetiframe.core.document.BrushId
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.roundToInt

/** Pure recipe math that is safe to validate without Android graphics. */
object BrushDynamics {
    fun inkWidthMultiplier(pressure: Float): Float {
        val normalized = pressure.coerceIn(0f, 1f)
        return 0.62f + normalized * normalized * 0.88f
    }

    fun highlighterAlpha(baseAlpha: Int): Int = (baseAlpha.coerceIn(0, 255) * 0.42f).roundToInt().coerceIn(0, 255)

    fun airbrushAlpha(baseAlpha: Int): Int = (baseAlpha.coerceIn(0, 255) * 0.11f).roundToInt().coerceIn(1, 255)

    /** Stable pseudo-random value used by stamp recipes in every rendering target. */
    fun deterministicUnit(seed: Long, index: Int, channel: Int = 0): Float {
        val mixed = seed xor ((index.toLong() * 31L + channel.toLong()) * -7046029254386353131L)
        return ((mixed xor (mixed ushr 33)) and 0xFFFF).toFloat() / 65535f
    }

    fun symmetricNoise(seed: Long, index: Int, channel: Int = 0): Float =
        deterministicUnit(seed, index, channel) * 2f - 1f

    fun crayonClusterCoverage(pressure: Float): Float = (0.52f + pressure.coerceIn(0f, 1f) * 0.35f).coerceIn(0f, 1f)

    fun crayonRadiusMultiplier(noise: Float): Float = 0.35f + noise.coerceIn(0f, 1f) * 0.95f

    /** Feather Crayon endpoints without removing the intentional mark made by a tap. */
    fun crayonEndpointOpacity(distanceFromEdge: Float, width: Float): Float {
        val normalized = (distanceFromEdge.coerceAtLeast(0f) / (width.coerceAtLeast(1f) * 0.95f))
            .coerceIn(0f, 1f)
        return 0.32f + normalized * 0.68f
    }
}

interface BrushRenderer {
    fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot)
}

abstract class BaseBrushRenderer : BrushRenderer {

    protected fun calculatePointWidth(index: Int, total: Int, point: TouchPoint, baseWidth: Float): Float {
        // Contact force and real movement shape the nib. Sample count is not physical distance:
        // forcing needle ends here made the same mark thinner on faster digitizers.
        return StrokeGeometry.calculateDynamicWidth(baseWidth, point.pressure, point.velocity)
    }

    protected class PaintState(paint: Paint) {
        private val alpha = paint.alpha
        private val style = paint.style
        private val strokeWidth = paint.strokeWidth
        private val strokeCap = paint.strokeCap
        private val strokeJoin = paint.strokeJoin
        private val pathEffect = paint.pathEffect
        private val shader = paint.shader
        private val maskFilter = paint.maskFilter
        private val xfermode = paint.xfermode

        fun restore(paint: Paint) {
            paint.alpha = alpha
            paint.style = style
            paint.strokeWidth = strokeWidth
            paint.strokeCap = strokeCap
            paint.strokeJoin = strokeJoin
            paint.pathEffect = pathEffect
            paint.shader = shader
            paint.maskFilter = maskFilter
            paint.xfermode = xfermode
        }
    }
}

object BrushRenderers {
    private val renderers = mapOf(
        BrushId.PEN to PenRenderer(),
        BrushId.PENCIL to ParticleBrushRenderer(PencilTip),
        BrushId.PAINT to SoftBrushRenderer(SoftBrushTip),
        BrushId.AIRBRUSH to AirbrushRenderer(AirbrushTip),
        BrushId.HIGHLIGHTER to HighlighterRenderer(),
        BrushId.CRAYON to ParticleBrushRenderer(CrayonTip),
        BrushId.INK to InkRenderer(),
        BrushId.HAIR to StrandBrushRenderer(HairProfile),
        BrushId.FUR to StrandBrushRenderer(FurProfile),
        BrushId.SKETCH to SketchRenderer(),
        BrushId.CALLIGRAPHY to CalligraphyRenderer(),
        BrushId.SPRAY to PhaseParticleRenderer(SprayProfile),
        BrushId.STIPPLE to PhaseParticleRenderer(StippleProfile),
        BrushId.ROUGH_PEN to RoughPenRenderer(),
        BrushId.GRAIN_PEN to GrainPenRenderer(),
        BrushId.DASH to DashRenderer(),
        BrushId.GEAR to GearRenderer(),
        BrushId.SAW to SawRenderer(),
    )

    fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        renderers[snapshot.id]?.draw(canvas, points, width, paint, snapshot)
    }
}
