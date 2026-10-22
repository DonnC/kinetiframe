package com.donnclab.kinetiframe.project.model

import kotlinx.serialization.Serializable

/**
 * Lightweight data container for spatial coordinates, stylus pressure, and timestamp.
 */
@Serializable
data class TouchPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val tilt: Float = 0f,
    val orientation: Float = 0f,
    val velocity: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
)
