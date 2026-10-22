package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.project.model.TouchPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A physical stabilizer that uses a mass-spring-damper system to filter input jitter.
 * It simulates a mass that is pulled by a spring toward the actual pen tip position.
 */
class MassSpringStabilizer(var stiffness: Float = 250f, var damping: Float = 32f, var mass: Float = 1.0f) {
    private var posX = 0f
    private var posY = 0f
    private var velX = 0f
    private var velY = 0f

    private var isInitialized = false
    private var bypass = false

    /**
     * Updates the internal physics constants based on a normalized smoothing value (0.0 - 1.0).
     * 0.0 = Exact raw input (physics bypassed)
     * 1.0 = High smoothing (Floats like a "Leash")
     */
    fun configureSmoothing(smoothing: Float) {
        val s = smoothing.coerceIn(0f, 1f)
        bypass = s == 0f
        // Map 0.0-1.0 to a stiffness range of 1000.0 (raw) down to 20.0 (high leash)
        stiffness = 1000f * (1f - s).coerceAtLeast(0.02f)
        damping = 2f * sqrt(stiffness * mass.coerceAtLeast(0.01f))
    }

    /**
     * Resets the stabilizer to a new starting point.
     */
    fun reset(point: TouchPoint) {
        posX = point.x
        posY = point.y
        velX = 0f
        velY = 0f
        isInitialized = true
    }

    /**
     * Snaps the stabilizer immediately to the target position, zeroing velocity.
     * Useful for flushing the last points of a stroke.
     */
    fun flush(target: TouchPoint): TouchPoint {
        posX = target.x
        posY = target.y
        velX = 0f
        velY = 0f
        return target
    }

    /**
     * Clears the stabilizer state, allowing it to re-initialize on the next update.
     */
    fun clear() {
        isInitialized = false
        posX = 0f
        posY = 0f
        velX = 0f
        velY = 0f
    }

    /** Keep processing a held contact while the simulated nib is still catching up. */
    fun needsUpdate(target: TouchPoint): Boolean = !bypass && isInitialized &&
        (abs(posX - target.x) > 0.05f || abs(posY - target.y) > 0.05f || abs(velX) > 0.5f || abs(velY) > 0.5f)

    /**
     * Updates the stabilizer with a new target point and returns the smoothed position.
     * @param target The raw input point.
     * @param deltaTime The time elapsed since the last update in seconds.
     */
    fun update(target: TouchPoint, deltaTime: Float): TouchPoint {
        if (!isInitialized) {
            reset(target)
            return target
        }

        if (bypass) return flush(target)
        if (!deltaTime.isFinite() || deltaTime <= 0f) return target.copy(x = posX, y = posY)
        // Exact damped solution for a held target. Keep custom damping usable for selection
        // handles as well as the critically damped brush configuration. No Euler instability,
        // discarded time, or per-axis temporary objects.
        val safeMass = mass.coerceAtLeast(0.01f)
        val omegaSquared = stiffness.coerceAtLeast(0.01f) / safeMass
        val gamma = damping.coerceAtLeast(0f) / (2f * safeMass)
        val discriminant = gamma * gamma - omegaSquared
        val c: Float
        val s: Float
        if (abs(discriminant) < omegaSquared * 0.00001f) {
            c = exp(-gamma * deltaTime)
            s = c * deltaTime
        } else if (discriminant > 0f) {
            val root = sqrt(discriminant)
            val first = exp((-gamma + root) * deltaTime)
            val second = exp((-gamma - root) * deltaTime)
            c = (first + second) * 0.5f
            s = (first - second) / (2f * root)
        } else {
            val root = sqrt(-discriminant)
            val decay = exp(-gamma * deltaTime)
            c = decay * cos(root * deltaTime)
            s = decay * sin(root * deltaTime) / root
        }
        val errorX = posX - target.x
        val errorY = posY - target.y
        posX = target.x + (c + gamma * s) * errorX + s * velX
        posY = target.y + (c + gamma * s) * errorY + s * velY
        velX = -omegaSquared * s * errorX + (c - gamma * s) * velX
        velY = -omegaSquared * s * errorY + (c - gamma * s) * velY
        // Width dynamics belong to the real input, not the simulated spring's speed.
        return target.copy(x = posX, y = posY)
    }
}
