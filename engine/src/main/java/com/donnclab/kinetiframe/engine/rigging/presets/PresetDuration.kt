// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import kotlin.math.round

object PresetDuration {
    private const val MICROS_PER_SECOND = 1_000_000
    private const val MIN_SPEED = .25f
    private const val MAX_SPEED = 3f

    // Saved speed stays raw; only the evaluation clock clamps it. Keep double precision before rounding.
    fun micros(durationSeconds: Float, speed: Float): Long =
        round(durationSeconds.toDouble() * MICROS_PER_SECOND / speed.coerceIn(MIN_SPEED, MAX_SPEED))
            .toLong().coerceAtLeast(1)
}
