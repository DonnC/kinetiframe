// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

data class BendLimits(val minimumDegrees: Float = 0f, val maximumDegrees: Float = 175f) {
    companion object {
        val Default = BendLimits()
    }
}
enum class BendSide(val sign: Int) { NEGATIVE(-1), POSITIVE(1) }
enum class TwoBoneIssue { CHAIN_SHAPE, NON_FINITE, BEND_LIMITS, SEGMENT_LENGTH }
class TwoBoneException(val issue: TwoBoneIssue) : IllegalArgumentException(issue.name)

internal fun requireIk(condition: Boolean, issue: TwoBoneIssue) {
    if (!condition) throw TwoBoneException(issue)
}
