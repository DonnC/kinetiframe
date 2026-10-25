package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal class ParticleBrushRenderer(private val tip: ParticleTip) : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            drawParticleStroke(canvas, points, width, paint, snapshot.seed, tip)
        } finally {
            state.restore(paint)
        }
    }

    private fun drawParticleStroke(
        canvas: Canvas,
        points: List<TouchPoint>,
        width: Float,
        paint: Paint,
        seed: Long,
        tip: ParticleTip,
    ) {
        paint.style = Paint.Style.FILL
        val originalAlpha = paint.alpha
        var stampIndex = 0
        var rawLength = 0f
        for (index in 1 until points.size) {
            rawLength += hypot(points[index].x - points[index - 1].x, points[index].y - points[index - 1].y)
        }
        var distanceAlongStroke = 0f

        fun stamp(
            x: Float,
            y: Float,
            pressure: Float,
            velocity: Float,
            index: Int,
            tangentX: Float,
            tangentY: Float,
            distanceAlong: Float,
        ) {
            val normalizedPressure = pressure.coerceIn(0f, 1f)
            val dynamicWidth =
                calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)

            val isClustered = tip.distribution == ParticleDistribution.CLUSTERED
            if (isClustered) {
                val clusterIndex = stampIndex / tip.clusterSpan
                if (BrushDynamics.deterministicUnit(seed, clusterIndex, 9) >
                    BrushDynamics.crayonClusterCoverage(normalizedPressure)
                ) {
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
                BrushDynamics.crayonEndpointOpacity(
                    min(distanceAlong, (rawLength - distanceAlong).coerceAtLeast(0f)),
                    dynamicWidth,
                )
            } else {
                1f
            }

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
                    alongStroke =
                        BrushDynamics.symmetricNoise(seed, noiseIndex, 1) * dynamicWidth * tip.alongStrokeJitter
                    acrossStroke = BrushDynamics.symmetricNoise(seed, noiseIndex, 2) * scatter
                }
                val xOffset = tangentX * alongStroke - tangentY * acrossStroke
                val yOffset = tangentY * alongStroke + tangentX * acrossStroke
                val radiusModifier = if (isClustered) {
                    BrushDynamics.crayonRadiusMultiplier(BrushDynamics.deterministicUnit(seed, noiseIndex, 3))
                } else {
                    0.70f + BrushDynamics.deterministicUnit(seed, noiseIndex, 3) * 0.55f
                }

                val radius = radiusBase * radiusModifier * (0.72f + endpointOpacity * 0.28f)
                paint.alpha =
                    (
                        originalAlpha * tip.opacity *
                            (
                                if (isClustered) {
                                    0.32f + BrushDynamics.deterministicUnit(seed, noiseIndex, 4) * 0.58f
                                } else {
                                    0.58f +
                                        BrushDynamics.deterministicUnit(seed, noiseIndex, 4) * 0.42f
                                }
                                ) *
                            endpointOpacity
                        ).roundToInt().coerceIn(1, 255)
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
                    idx + 1,
                    tx,
                    ty,
                    distanceAlongStroke + dist * t,
                )
            }
            distanceAlongStroke += dist
        }
    }
}

internal class SoftBrushRenderer(private val tip: SoftTip) : BaseBrushRenderer() {
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
            val dynamicWidth =
                calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)
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
                    idx + 1,
                )
            }
        }
    }
}

internal class AirbrushRenderer(private val tip: SoftTip) : BaseBrushRenderer() {
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
                val dynamicWidth =
                    calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)
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
                        idx + 1,
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
            airbrushGradient = RadialGradient(
                0f,
                0f,
                1f,
                intArrayOf(
                    Color.argb(effectiveAlpha, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            airbrushGradientColor = color
            airbrushGradientAlpha = effectiveAlpha
        }
        return airbrushGradient!!
    }
}

internal class StrandBrushRenderer(private val profile: StrandProfile) : BaseBrushRenderer() {
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
                        val spike = if (profile.strands ==
                            5
                        ) {
                            BrushDynamics.symmetricNoise(snapshot.seed, s, 2) * width * 0.12f
                        } else {
                            0f
                        }
                        strandPathPool[s].lineTo(curr.x + nx * (offset + spike), curr.y + ny * (offset + spike))
                    }
                }
            }
            for (s in 0 until strandCount) {
                val lengthFactor = 1f + BrushDynamics.symmetricNoise(snapshot.seed, s, 2) * profile.lengthJitter
                paint.strokeWidth = max(0.35f, width * profile.strokeScale * (0.82f + lengthFactor * 0.18f))
                paint.alpha =
                    (
                        originalAlpha * profile.opacity *
                            (0.78f + BrushDynamics.deterministicUnit(snapshot.seed, s, 3) * 0.22f)
                        ).roundToInt().coerceIn(1, 255)
                canvas.drawPath(strandPathPool[s], paint)
            }
        } finally {
            state.restore(paint)
        }
    }
}

internal class SketchRenderer : BaseBrushRenderer() {
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
                        paint.alpha =
                            (
                                originalAlpha * 0.15f * fade * current.pressure.coerceIn(
                                    0f,
                                    1f,
                                )
                                ).roundToInt().coerceIn(1, 255)
                        paint.strokeWidth =
                            lineWidth * (0.5f + BrushDynamics.deterministicUnit(snapshot.seed, i * 31 + j) * 0.5f)
                        canvas.drawLine(prev.x, prev.y, current.x, current.y, paint)
                    }
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

internal class PhaseParticleRenderer(private val profile: PhaseParticleProfile) : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.FILL
            val originalAlpha = paint.alpha
            var stampIndex = 0
            fun stamp(x: Float, y: Float, pressure: Float, velocity: Float, index: Int, tx: Float, ty: Float) {
                val nx = -ty
                val ny = tx
                val dynamicWidth =
                    calculatePointWidth(index, points.size, TouchPoint(x, y, pressure, velocity = velocity), width)
                repeat(profile.particles) { pIdx ->
                    val pIndex = stampIndex * profile.particles + pIdx
                    if (profile.skipProbability > 0f &&
                        BrushDynamics.deterministicUnit(snapshot.seed, pIndex, 0) < profile.skipProbability
                    ) {
                        return@repeat
                    }
                    val across = BrushDynamics.symmetricNoise(snapshot.seed, pIndex, 1) * dynamicWidth * profile.scatter
                    val along =
                        BrushDynamics.symmetricNoise(snapshot.seed, pIndex, 2) * dynamicWidth * profile.alongJitter
                    val radial =
                        dynamicWidth * profile.radius *
                            (0.7f + BrushDynamics.deterministicUnit(snapshot.seed, pIndex, 3) * 0.6f)
                    paint.alpha =
                        (
                            originalAlpha * profile.opacity *
                                (0.72f + BrushDynamics.deterministicUnit(snapshot.seed, pIndex, 4) * 0.28f)
                            ).roundToInt().coerceIn(1, 255)
                    canvas.drawCircle(
                        x + nx * across + tx * along,
                        y + ny * across + ty * along,
                        max(0.35f, radial),
                        paint,
                    )
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
                    stamp(
                        prev.x + dx * t,
                        prev.y + dy * t,
                        prev.pressure + (curr.pressure - prev.pressure) * t,
                        prev.velocity + (curr.velocity - prev.velocity) * t,
                        idx + 1,
                        tx,
                        ty,
                    )
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}
