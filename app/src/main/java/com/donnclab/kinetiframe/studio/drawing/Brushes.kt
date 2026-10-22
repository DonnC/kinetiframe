package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import com.donnclab.kinetiframe.studio.drawing.StrokeGeometry
import com.donnclab.kinetiframe.project.model.TouchPoint
import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
enum class BrushId(val displayName: String) {
    PEN("Pen"),
    PENCIL("Pencil"),
    PAINT("Paint Brush"),
    AIRBRUSH("Airbrush"),
    HIGHLIGHTER("Highlighter"),
    CRAYON("Crayon"),
    INK("Ink"),
    HAIR("Hair"),
    FUR("Fur"),
    SKETCH("Sketch"),
    CALLIGRAPHY("Calligraphy"),
    SPRAY("Spray"),
    STIPPLE("Stipple"),
    ROUGH_PEN("Rough Pen"),
    GRAIN_PEN("Grain Pen"),
    DASH("Dash"),
    GEAR("Gear"),
    SAW("Saw")
}

/** Immutable recipe carried by every completed stroke for deterministic replay. */
@Serializable
data class BrushSnapshot(
    val id: BrushId = BrushId.PEN,
    val rendererVersion: Int = 1,
    val seed: Long = 0L
)

object BrushCatalog {
    val drawingBrushes: List<BrushId> = listOf(
        BrushId.PEN, BrushId.PENCIL, BrushId.PAINT, BrushId.AIRBRUSH,
        BrushId.HIGHLIGHTER, BrushId.CRAYON, BrushId.INK,
        BrushId.HAIR, BrushId.FUR, BrushId.SKETCH, BrushId.CALLIGRAPHY,
        BrushId.SPRAY, BrushId.STIPPLE, BrushId.ROUGH_PEN, BrushId.GRAIN_PEN,
        BrushId.DASH, BrushId.GEAR, BrushId.SAW
    )
}

/** Pure recipe math that is safe to validate without Android graphics. */
object BrushDynamics {
    fun inkWidthMultiplier(pressure: Float): Float {
        val normalized = pressure.coerceIn(0f, 1f)
        return 0.62f + normalized * normalized * 0.88f
    }

    fun highlighterAlpha(baseAlpha: Int): Int =
        (baseAlpha.coerceIn(0, 255) * 0.42f).roundToInt().coerceIn(0, 255)

    fun airbrushAlpha(baseAlpha: Int): Int =
        (baseAlpha.coerceIn(0, 255) * 0.11f).roundToInt().coerceIn(1, 255)

    /** Stable pseudo-random value used by stamp recipes in every rendering target. */
    fun deterministicUnit(seed: Long, index: Int, channel: Int = 0): Float {
        val mixed = seed xor ((index.toLong() * 31L + channel.toLong()) * -7046029254386353131L)
        return ((mixed xor (mixed ushr 33)) and 0xFFFF).toFloat() / 65535f
    }

    fun symmetricNoise(seed: Long, index: Int, channel: Int = 0): Float =
        deterministicUnit(seed, index, channel) * 2f - 1f

    fun crayonClusterCoverage(pressure: Float): Float =
        (0.52f + pressure.coerceIn(0f, 1f) * 0.35f).coerceIn(0f, 1f)

    fun crayonRadiusMultiplier(noise: Float): Float =
        0.35f + noise.coerceIn(0f, 1f) * 0.95f

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
    
    protected fun calculatePointWidth(
        index: Int,
        total: Int,
        point: TouchPoint,
        baseWidth: Float
    ): Float {
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

// Brush Configuration Data Classes
private data class ParticleTip(
    val radiusScale: Float,
    val spacingScale: Float,
    val scatterScale: Float,
    val alongStrokeJitter: Float,
    val opacity: Float,
    val particlesPerStamp: Int,
    val distribution: ParticleDistribution = ParticleDistribution.UNIFORM,
    val maximumSpacing: Float = Float.MAX_VALUE,
    val maximumParticleRadius: Float = Float.MAX_VALUE,
    val clusterSpan: Int = 1
)

private enum class ParticleDistribution { UNIFORM, CLUSTERED }

private data class StrandProfile(
    val strands: Int,
    val scatter: Float,
    val lengthJitter: Float,
    val strokeScale: Float,
    val opacity: Float
)

private data class PhaseParticleProfile(
    val particles: Int,
    val radius: Float,
    val spacing: Float,
    val scatter: Float,
    val alongJitter: Float,
    val opacity: Float,
    val maximumSpacing: Float,
    val skipProbability: Float = 0f
)

private data class SoftLayer(val radiusScale: Float, val opacity: Float)

private data class SoftTip(
    val radiusScale: Float,
    val opacity: Float,
    val spacingScale: Float,
    val layers: List<SoftLayer>
)

// Global Brush Profiles
private val PencilTip = ParticleTip(0.18f, 0.16f, 0.20f, 0.06f, 0.35f, 2, maximumSpacing = 1.6f)
private val CrayonTip = ParticleTip(0.13f, 0.30f, 0.76f, 0.42f, 0.58f, 7, ParticleDistribution.CLUSTERED, maximumSpacing = 1.35f, maximumParticleRadius = 2.2f, clusterSpan = 4)
private val HairProfile = StrandProfile(3, 0.24f, 0.35f, 0.16f, 0.62f)
private val FurProfile = StrandProfile(5, 0.58f, 0.70f, 0.13f, 0.42f)
private val SketchProfile = PhaseParticleProfile(3, 0.16f, 0.28f, 0.24f, 0.10f, 0.34f, 1.4f)
private val SprayProfile = PhaseParticleProfile(8, 0.10f, 0.44f, 1.25f, 0.82f, 0.32f, 2.4f, 0.46f)
private val StippleProfile = PhaseParticleProfile(2, 0.24f, 0.70f, 0.42f, 0.18f, 0.58f, 3.2f)
private val GrainProfile = PhaseParticleProfile(3, 0.12f, 0.32f, 0.28f, 0.16f, 0.18f, 1.8f, 0.18f)
private val SoftBrushTip = SoftTip(0.90f, 0.82f, 0.18f, listOf(SoftLayer(1.26f, 0.08f), SoftLayer(1.06f, 0.16f), SoftLayer(0.78f, 0.38f)))
private val AirbrushTip = SoftTip(0.95f, 0.45f, 0.24f, listOf(SoftLayer(1.65f, 0.04f), SoftLayer(1.28f, 0.08f), SoftLayer(0.90f, 0.14f)))

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
        BrushId.SAW to SawRenderer()
    )

    fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        renderers[snapshot.id]?.draw(canvas, points, width, paint, snapshot)
    }
}

private class PenRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            PenStrokeRenderer.draw(canvas, points, width, paint)
        } finally {
            state.restore(paint)
        }
    }
}

private class InkRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        if (points.size < 2) {
            PressureStrokeRenderer.draw(canvas, points, width, paint)
            return
        }
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.FILL
            val path = Path()
            val leftPoints = mutableListOf<PointF>()
            val rightPoints = mutableListOf<PointF>()

            StrokeGeometry.forEachSegment(points) { index, previous, current ->
                val normal = StrokeGeometry.calculateNormal(previous, current)
                val w = calculatePointWidth(index + 1, points.size, current, width) * BrushDynamics.inkWidthMultiplier(current.pressure)
                
                if (leftPoints.isEmpty()) {
                    val w0 = calculatePointWidth(index, points.size, previous, width) * BrushDynamics.inkWidthMultiplier(previous.pressure)
                    leftPoints.add(PointF(previous.x + normal.first * w0 / 2f, previous.y + normal.second * w0 / 2f))
                    rightPoints.add(PointF(previous.x - normal.first * w0 / 2f, previous.y - normal.second * w0 / 2f))
                }
                
                leftPoints.add(PointF(current.x + normal.first * w / 2f, current.y + normal.second * w / 2f))
                rightPoints.add(PointF(current.x - normal.first * w / 2f, current.y - normal.second * w / 2f))
            }

            if (leftPoints.isNotEmpty()) {
                path.moveTo(leftPoints[0].x, leftPoints[0].y)
                for (i in 1 until leftPoints.size) path.lineTo(leftPoints[i].x, leftPoints[i].y)
                for (i in rightPoints.size - 1 downTo 0) path.lineTo(rightPoints[i].x, rightPoints[i].y)
                path.close()
                canvas.drawPath(path, paint)
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class ParticleBrushRenderer(private val tip: ParticleTip) : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            drawParticleStroke(canvas, points, width, paint, snapshot.seed, tip)
        } finally {
            state.restore(paint)
        }
    }

    private fun drawParticleStroke(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, seed: Long, tip: ParticleTip) {
        paint.style = Paint.Style.FILL
        val originalAlpha = paint.alpha
        var stampIndex = 0
        var rawLength = 0f
        for (index in 1 until points.size) {
            rawLength += hypot(points[index].x - points[index - 1].x, points[index].y - points[index - 1].y)
        }
        var distanceAlongStroke = 0f

        fun stamp(x: Float, y: Float, pressure: Float, velocity: Float, index: Int, tangentX: Float, tangentY: Float, distanceAlong: Float) {
            val normalizedPressure = pressure.coerceIn(0f, 1f)
            val dynamicWidth = calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)
            
            val isClustered = tip.distribution == ParticleDistribution.CLUSTERED
            if (isClustered) {
                val clusterIndex = stampIndex / tip.clusterSpan
                if (BrushDynamics.deterministicUnit(seed, clusterIndex, 9) > BrushDynamics.crayonClusterCoverage(normalizedPressure)) {
                    stampIndex++
                    return
                }
            }

            val radiusBase = if (isClustered) {
                min(dynamicWidth * tip.radiusScale, tip.maximumParticleRadius)
            } else {
                max(0.45f, dynamicWidth * tip.radiusScale)
            }
            
            val scatter = dynamicWidth * tip.scatterScale
            val endpointOpacity = if (isClustered) {
                BrushDynamics.crayonEndpointOpacity(min(distanceAlong, (rawLength - distanceAlong).coerceAtLeast(0f)), dynamicWidth)
            } else 1f

            repeat(tip.particlesPerStamp) { pIdx ->
                val noiseIndex = stampIndex * tip.particlesPerStamp + pIdx
                val alongStroke: Float
                val acrossStroke: Float
                if (isClustered) {
                    val u1 = BrushDynamics.deterministicUnit(seed, noiseIndex, 1).coerceAtLeast(0.001f)
                    val u2 = BrushDynamics.deterministicUnit(seed, noiseIndex, 2)
                    val radial = min(2.4f, sqrt(-2f * ln(u1)))
                    val theta = (2.0 * Math.PI * u2).toFloat()
                    val pressureTooth = 1.25f - normalizedPressure * 0.45f
                    alongStroke = radial * cos(theta) * dynamicWidth * tip.alongStrokeJitter * 0.52f * pressureTooth
                    acrossStroke = radial * sin(theta) * scatter * 0.52f * pressureTooth
                } else {
                    alongStroke = BrushDynamics.symmetricNoise(seed, noiseIndex, 1) * dynamicWidth * tip.alongStrokeJitter
                    acrossStroke = BrushDynamics.symmetricNoise(seed, noiseIndex, 2) * scatter
                }
                val xOffset = tangentX * alongStroke - tangentY * acrossStroke
                val yOffset = tangentY * alongStroke + tangentX * acrossStroke
                val radiusModifier = if (isClustered) BrushDynamics.crayonRadiusMultiplier(BrushDynamics.deterministicUnit(seed, noiseIndex, 3))
                else 0.70f + BrushDynamics.deterministicUnit(seed, noiseIndex, 3) * 0.55f
                
                val radius = radiusBase * radiusModifier * (0.72f + endpointOpacity * 0.28f)
                paint.alpha = (originalAlpha * tip.opacity * (if (isClustered) 0.32f + BrushDynamics.deterministicUnit(seed, noiseIndex, 4) * 0.58f else 0.58f + BrushDynamics.deterministicUnit(seed, noiseIndex, 4) * 0.42f) * endpointOpacity).roundToInt().coerceIn(1, 255)
                canvas.drawCircle(x + xOffset, y + yOffset, radius, paint)
            }
            stampIndex++
        }

        val first = points.first()
        stamp(first.x, first.y, first.pressure, first.velocity, 0, 1f, 0f, 0f)
        StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
            val deltaX = curr.x - prev.x
            val deltaY = curr.y - prev.y
            val dist = hypot(deltaX, deltaY)
            val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
            val requestedSpacing = max(0.65f, dynamicWidth * tip.spacingScale)
            val spacing = min(requestedSpacing, tip.maximumSpacing)
            val stamps = ceil(dist / spacing).toInt().coerceAtLeast(1)
            val tx = if (dist > 0f) deltaX / dist else 1f
            val ty = if (dist > 0f) deltaY / dist else 0f
            repeat(stamps) { step ->
                val t = (step + 1).toFloat() / stamps
                stamp(
                    prev.x + deltaX * t,
                    prev.y + deltaY * t,
                    prev.pressure + (curr.pressure - prev.pressure) * t,
                    prev.velocity + (curr.velocity - prev.velocity) * t,
                    idx + 1, tx, ty, distanceAlongStroke + dist * t
                )
            }
            distanceAlongStroke += dist
        }
    }
}

private class SoftBrushRenderer(private val tip: SoftTip) : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            drawSoftStroke(canvas, points, width, paint, tip)
        } finally {
            state.restore(paint)
        }
    }

    private fun drawSoftStroke(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, tip: SoftTip) {
        paint.style = Paint.Style.FILL
        val originalAlpha = paint.alpha
        fun stamp(x: Float, y: Float, pressure: Float, velocity: Float, index: Int) {
            val dynamicWidth = calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)
            val radius = dynamicWidth * tip.radiusScale
            tip.layers.forEach { layer ->
                paint.alpha = (originalAlpha * tip.opacity * layer.opacity).roundToInt().coerceIn(1, 255)
                canvas.drawCircle(x, y, radius * layer.radiusScale, paint)
            }
        }

        val first = points.first()
        stamp(first.x, first.y, first.pressure, first.velocity, 0)
        StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
            val dist = hypot(curr.x - prev.x, curr.y - prev.y)
            val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
            val stamps = ceil(dist / max(0.5f, dynamicWidth * tip.spacingScale)).toInt().coerceAtLeast(1)
            repeat(stamps) { step ->
                val t = (step + 1).toFloat() / stamps
                stamp(
                    prev.x + (curr.x - prev.x) * t,
                    prev.y + (curr.y - prev.y) * t,
                    prev.pressure + (curr.pressure - prev.pressure) * t,
                    prev.velocity + (curr.velocity - prev.velocity) * t,
                    idx + 1
                )
            }
        }
    }
}

private class AirbrushRenderer(private val tip: SoftTip) : BaseBrushRenderer() {
    private var airbrushGradient: RadialGradient? = null
    private var airbrushGradientColor: Int = Int.MIN_VALUE
    private var airbrushGradientAlpha: Int = Int.MIN_VALUE

    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            val originalColor = paint.color
            val originalAlpha = paint.alpha
            val gradient = cachedAirbrushGradient(originalColor, originalAlpha)
            val localMatrix = Matrix()
            paint.alpha = 255
            paint.shader = gradient
            paint.style = Paint.Style.FILL

            fun stamp(x: Float, y: Float, pressure: Float, velocity: Float, index: Int) {
                val dynamicWidth = calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)
                val radius = dynamicWidth * tip.radiusScale
                localMatrix.reset()
                localMatrix.setScale(radius, radius)
                localMatrix.postTranslate(x, y)
                gradient.setLocalMatrix(localMatrix)
                canvas.drawCircle(x, y, radius, paint)
            }

            val first = points.first()
            stamp(first.x, first.y, first.pressure, first.velocity, 0)
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dist = hypot(curr.x - prev.x, curr.y - prev.y)
                val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                val stamps = ceil(dist / max(0.5f, dynamicWidth * tip.spacingScale)).toInt().coerceAtLeast(1)
                repeat(stamps) { step ->
                    val t = (step + 1).toFloat() / stamps
                    stamp(
                        prev.x + (curr.x - prev.x) * t,
                        prev.y + (curr.y - prev.y) * t,
                        prev.pressure + (curr.pressure - prev.pressure) * t,
                        prev.velocity + (curr.velocity - prev.velocity) * t,
                        idx + 1
                    )
                }
            }
        } finally {
            state.restore(paint)
        }
    }

    private fun cachedAirbrushGradient(color: Int, alpha: Int): RadialGradient {
        val effectiveAlpha = BrushDynamics.airbrushAlpha(alpha)
        if (airbrushGradient == null || airbrushGradientColor != color || airbrushGradientAlpha != effectiveAlpha) {
            airbrushGradient = RadialGradient(0f, 0f, 1f,
                intArrayOf(Color.argb(effectiveAlpha, Color.red(color), Color.green(color), Color.blue(color)), Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            airbrushGradientColor = color
            airbrushGradientAlpha = effectiveAlpha
        }
        return airbrushGradient!!
    }
}

private class HighlighterRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.SQUARE
            paint.strokeJoin = Paint.Join.BEVEL
            paint.alpha = BrushDynamics.highlighterAlpha(paint.alpha)
            paint.xfermode = null
            PressureStrokeRenderer.draw(canvas, points, width * 1.05f, paint)
        } finally {
            state.restore(paint)
        }
    }
}

private class StrandBrushRenderer(private val profile: StrandProfile) : BaseBrushRenderer() {
    private val strandPathPool = Array(12) { Path() }
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        if (points.size < 2) {
            PressureStrokeRenderer.draw(canvas, points, width * profile.strokeScale, paint)
            return
        }
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            val originalAlpha = paint.alpha
            val strandCount = profile.strands.coerceIn(1, strandPathPool.size)
            val first = points.first()
            val next = points[1]
            val firstDist = hypot(next.x - first.x, next.y - first.y)
            val firstNx = if (firstDist > 0f) -(next.y - first.y) / firstDist else 0f
            val firstNy = if (firstDist > 0f) (next.x - first.x) / firstDist else 1f
            for (s in 0 until strandCount) {
                strandPathPool[s].reset()
                val offset = BrushDynamics.symmetricNoise(snapshot.seed, s, 1) * width * profile.scatter
                strandPathPool[s].moveTo(first.x + firstNx * offset, first.y + firstNy * offset)
            }
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val tx = dx / dist
                    val ty = dy / dist
                    val nx = -ty
                    val ny = tx
                    repeat(strandCount) { s ->
                        val offset = BrushDynamics.symmetricNoise(snapshot.seed, s, 1) * width * profile.scatter
                        val spike = if (profile.strands == 5) BrushDynamics.symmetricNoise(snapshot.seed, s, 2) * width * 0.12f else 0f
                        strandPathPool[s].lineTo(curr.x + nx * (offset + spike), curr.y + ny * (offset + spike))
                    }
                }
            }
            for (s in 0 until strandCount) {
                val lengthFactor = 1f + BrushDynamics.symmetricNoise(snapshot.seed, s, 2) * profile.lengthJitter
                paint.strokeWidth = max(0.35f, width * profile.strokeScale * (0.82f + lengthFactor * 0.18f))
                paint.alpha = (originalAlpha * profile.opacity * (0.78f + BrushDynamics.deterministicUnit(snapshot.seed, s, 3) * 0.22f)).roundToInt().coerceIn(1, 255)
                canvas.drawPath(strandPathPool[s], paint)
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class SketchRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            val originalAlpha = paint.alpha
            val pRenderer = PhaseParticleRenderer(SketchProfile)
            pRenderer.draw(canvas, points, width, paint, snapshot)
            
            if (points.size < 2) return
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            val range = width * 4f
            val rangeSq = range * range
            val lineWidth = max(0.2f, width * 0.045f)
            for (i in 1 until points.size) {
                val current = points[i]
                val firstHistory = max(0, i - 15)
                for (j in i - 1 downTo firstHistory step 2) {
                    val prev = points[j]
                    val dSq = (current.x - prev.x) * (current.x - prev.x) + (current.y - prev.y) * (current.y - prev.y)
                    if (dSq in 9f..rangeSq) {
                        val dist = sqrt(dSq)
                        val fade = 1f - (dist / range)
                        paint.alpha = (originalAlpha * 0.15f * fade * current.pressure.coerceIn(0f, 1f)).roundToInt().coerceIn(1, 255)
                        paint.strokeWidth = lineWidth * (0.5f + BrushDynamics.deterministicUnit(snapshot.seed, i * 31 + j) * 0.5f)
                        canvas.drawLine(prev.x, prev.y, current.x, current.y, paint)
                    }
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class CalligraphyRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.BUTT
            paint.strokeJoin = Paint.Join.MITER
            val nibAngle = -Math.PI.toFloat() / 4f
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val tangentAngle = kotlin.math.atan2(dy, dx)
                    val nibResponse = kotlin.math.abs(cos(tangentAngle - nibAngle))
                    val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                    paint.strokeWidth = dynamicWidth * (0.32f + nibResponse * 1.18f)
                    canvas.drawLine(prev.x, prev.y, curr.x, curr.y, paint)
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class PhaseParticleRenderer(private val profile: PhaseParticleProfile) : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.FILL
            val originalAlpha = paint.alpha
            var stampIndex = 0
            fun stamp(x: Float, y: Float, pressure: Float, velocity: Float, index: Int, tx: Float, ty: Float) {
                val nx = -ty
                val ny = tx
                val dynamicWidth = calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)
                repeat(profile.particles) { pIdx ->
                    val pIndex = stampIndex * profile.particles + pIdx
                    if (profile.skipProbability > 0f && BrushDynamics.deterministicUnit(snapshot.seed, pIndex, 0) < profile.skipProbability) return@repeat
                    val across = BrushDynamics.symmetricNoise(snapshot.seed, pIndex, 1) * dynamicWidth * profile.scatter
                    val along = BrushDynamics.symmetricNoise(snapshot.seed, pIndex, 2) * dynamicWidth * profile.alongJitter
                    val radial = dynamicWidth * profile.radius * (0.7f + BrushDynamics.deterministicUnit(snapshot.seed, pIndex, 3) * 0.6f)
                    paint.alpha = (originalAlpha * profile.opacity * (0.72f + BrushDynamics.deterministicUnit(snapshot.seed, pIndex, 4) * 0.28f)).roundToInt().coerceIn(1, 255)
                    canvas.drawCircle(x + nx * across + tx * along, y + ny * across + ty * along, max(0.35f, radial), paint)
                }
                stampIndex++
            }
            val first = points.first()
            stamp(first.x, first.y, first.pressure, first.velocity, 0, 1f, 0f)
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                val spacing = min(max(0.65f, dynamicWidth * profile.spacing), profile.maximumSpacing)
                val count = ceil(dist / spacing).toInt().coerceAtLeast(1)
                val tx = if (dist > 0f) dx / dist else 1f
                val ty = if (dist > 0f) dy / dist else 0f
                repeat(count) { step ->
                    val t = (step + 1).toFloat() / count
                    stamp(prev.x + dx * t, prev.y + dy * t, prev.pressure + (curr.pressure - prev.pressure) * t, prev.velocity + (curr.velocity - prev.velocity) * t, idx + 1, tx, ty)
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class RoughPenRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            var index = 0
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val nx = -dy / dist
                    val ny = dx / dist
                    val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                    val wobble = BrushDynamics.symmetricNoise(snapshot.seed, index++, 0) * dynamicWidth * 0.12f
                    paint.strokeWidth = dynamicWidth * (0.82f + BrushDynamics.deterministicUnit(snapshot.seed, index, 1) * 0.28f)
                    canvas.drawLine(prev.x + nx * wobble, prev.y + ny * wobble, curr.x + nx * wobble, curr.y + ny * wobble, paint)
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class GrainPenRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            PressureStrokeRenderer.draw(canvas, points, width, paint)
            paint.style = Paint.Style.FILL
            PhaseParticleRenderer(GrainProfile).draw(canvas, points, width, paint, snapshot)
        } finally {
            state.restore(paint)
        }
    }
}

private class DashRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.BUTT
            val dash = max(width * 2.4f, 5f)
            val gap = max(width * 1.3f, 3f)
            var travelled = 0f
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    var local = 0f
                    while (local < dist) {
                        val cycle = (travelled + local) % (dash + gap)
                        val take = min(dist - local, if (cycle < dash) dash - cycle else (dash + gap) - cycle)
                        if (cycle < dash && take > 0f) {
                            val t0 = local / dist
                            val t1 = (local + take) / dist
                            val dynamicWidth = calculatePointWidth(idx, points.size, TouchPoint(prev.x + dx * t0, prev.y + dy * t0, prev.pressure, velocity = prev.velocity), width)
                            paint.strokeWidth = dynamicWidth
                            canvas.drawLine(prev.x + dx * t0, prev.y + dy * t0, prev.x + dx * t1, prev.y + dy * t1, paint)
                        }
                        local += take.coerceAtLeast(0.001f)
                    }
                    travelled += dist
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class GearRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.SQUARE
            val step = max(width * 1.8f, 6f)
            var travelled = 0f
            var next = 0f
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val tx = dx / dist
                    val ty = dy / dist
                    while (next <= travelled + dist) {
                        val t = ((next - travelled) / dist).coerceIn(0f, 1f)
                        val x = prev.x + dx * t
                        val y = prev.y + dy * t
                        val dynamicWidth = calculatePointWidth(idx, points.size, TouchPoint(x, y, prev.pressure, velocity = prev.velocity), width)
                        val radius = dynamicWidth * (0.34f + BrushDynamics.deterministicUnit(snapshot.seed, next.roundToInt(), 2) * 0.12f)
                        paint.strokeWidth = max(0.5f, dynamicWidth * 0.18f)
                        canvas.drawCircle(x, y, radius, paint)
                        canvas.drawLine(x - ty * radius, y + tx * radius, x + ty * radius, y - tx * radius, paint)
                        next += step
                    }
                    travelled += dist
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

private class SawRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.SQUARE
            var travelled = 0f
            val tooth = max(width * 1.4f, 5f)
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val nx = -dy / dist
                    val ny = dx / dist
                    val t0 = travelled / tooth
                    val t1 = (travelled + dist) / tooth
                    val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                    val y0 = if (kotlin.math.floor(t0).toInt() % 2 == 0) -dynamicWidth * 0.28f else dynamicWidth * 0.28f
                    val y1 = if (kotlin.math.floor(t1).toInt() % 2 == 0) -dynamicWidth * 0.28f else dynamicWidth * 0.28f
                    paint.strokeWidth = dynamicWidth * 0.15f
                    canvas.drawLine(prev.x + nx * y0, prev.y + ny * y0, curr.x + nx * y1, curr.y + ny * y1, paint)
                    travelled += dist
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}
