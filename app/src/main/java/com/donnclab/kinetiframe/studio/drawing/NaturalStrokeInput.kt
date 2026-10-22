package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.project.model.TouchPoint
import kotlin.math.abs
import kotlin.math.hypot

/** One causal input stage, not a spring followed by another stabilizer.
 * One-Euro's signed, low-pass derivative controls responsiveness: alternating sensor noise
 * must not be mistaken for purposeful high speed. A soft radial dead band anchors a held nib
 * without quantising slow movement into a sequence of hard jumps. Coordinates stay affine. */
class NaturalStrokeInput {
    private var rawScreen: TouchPoint? = null
    private var output: TouchPoint? = null
    var screenPoint: TouchPoint? = null
        private set
    private var dx = 0f
    private var dy = 0f
    private var forceTarget = 1f
    private var density = 1f

    fun reset(displayDensity: Float = 1f) {
        rawScreen = null
        output = null
        screenPoint = null
        dx = 0f
        dy = 0f
        density = if (displayDensity.isFinite()) displayDensity.coerceIn(0.75f, 4f) else 1f
    }

    fun update(sample: TouchPoint, screen: TouchPoint, stylus: Boolean): TouchPoint? {
        if (!sample.x.isFinite() || !sample.y.isFinite() || !screen.x.isFinite() || !screen.y.isFinite()) return null
        val priorRaw = rawScreen
        if (priorRaw != null && sample.timestamp <= priorRaw.timestamp) return null
        val pressure = PressureResponse.contactPressure(sample.pressure, stylus)
        val prior = output
        if (prior == null || priorRaw == null) {
            forceTarget = pressure
            rawScreen = screen.copy(timestamp = sample.timestamp)
            screenPoint = screen.copy(pressure = pressure, velocity = 0f, timestamp = sample.timestamp)
            return sample.copy(pressure = pressure, velocity = 0f).also { output = it }
        }
        val dt = ((sample.timestamp - priorRaw.timestamp) / 1000f).coerceIn(0.001f, 0.1f)
        val derivativeAlpha = alpha(8f, dt)
        dx += (((screen.x - priorRaw.x) / dt) - dx) * derivativeAlpha
        dy += (((screen.y - priorRaw.y) / dt) - dy) * derivativeAlpha
        rawScreen = screen.copy(timestamp = sample.timestamp)
        val previousScreen = requireNotNull(screenPoint)
        val distance = hypot(screen.x - previousScreen.x, screen.y - previousScreen.y)
        val radius = HOLD_RADIUS_DP * density
        val speed = hypot(dx, dy)
        // Keep ordinary handwriting below the sensor-noise band; fast flicks raise the cutoff.
        val positionAlpha = alpha(8f + 0.025f * speed / density, dt)
        // Subtracting the radius continuously, rather than snapping to an anchor, avoids stair-steps.
        val blend = if (distance <= radius) 0f else positionAlpha * (distance - radius) / distance
        val sx = previousScreen.x + (screen.x - previousScreen.x) * blend
        val sy = previousScreen.y + (screen.y - previousScreen.y) * blend
        if (abs(pressure - forceTarget) > 0.035f) forceTarget = pressure
        val force = PressureResponse.filter(prior.pressure, forceTarget, dt)
        val velocity = if (blend == 0f) 0f else speed
        screenPoint = screen.copy(x = sx, y = sy, pressure = force, velocity = velocity, timestamp = sample.timestamp)
        return sample.copy(
            x = prior.x + (sample.x - prior.x) * blend,
            y = prior.y + (sample.y - prior.y) * blend,
            pressure = force,
            velocity = velocity,
        ).also { output = it }
    }

    private fun alpha(cutoff: Float, dt: Float): Float = dt / (dt + 1f / (2f * Math.PI.toFloat() * cutoff))

    companion object {
        const val HOLD_RADIUS_DP = 0.65f
    }
}
