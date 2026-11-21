// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

enum class BeforeTake { HOLD_FIRST_VALUE, PRESERVE_PRESET }

data class CurveSampling(
    val loop: Boolean = false,
    val durationMicros: Long? = null,
    val beforeTake: BeforeTake = BeforeTake.HOLD_FIRST_VALUE,
) {
    internal fun periodic(first: Long, last: Long): Boolean =
        loop && first == 0L && (durationMicros == null || last == durationMicros)

    internal inline fun excluded(time: Long, first: () -> Long): Boolean =
        beforeTake == BeforeTake.PRESERVE_PRESET && time < first()
}

data class CurveCacheLimits(val rotations: Int = DEFAULT_ROTATIONS, val roots: Int = DEFAULT_ROOTS) {
    companion object {
        private const val DEFAULT_ROTATIONS = 128
        private const val DEFAULT_ROOTS = 32
    }
}
