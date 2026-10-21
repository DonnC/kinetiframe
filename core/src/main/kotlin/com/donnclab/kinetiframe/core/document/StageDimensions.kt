// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class StageDimensionIssue { NON_POSITIVE_WIDTH, NON_POSITIVE_HEIGHT }
class StageDimensionException(val issue: StageDimensionIssue) : IllegalArgumentException(issue.name)

/** Immutable authoring dimensions, independent of the device viewport. */
@Serializable
@SerialName("com.kinetiframe.engine.model.StageDimensions")
data class StageDimensions(val width: Int, val height: Int) {
    init {
        if (width <= 0) throw StageDimensionException(StageDimensionIssue.NON_POSITIVE_WIDTH)
        if (height <= 0) throw StageDimensionException(StageDimensionIssue.NON_POSITIVE_HEIGHT)
    }
    companion object {
        val LandscapeHd = StageDimensions(1920, 1080)
        val Square = StageDimensions(1080, 1080)
        val Portrait = StageDimensions(1080, 1350)
        val Vertical = StageDimensions(1080, 1920)
    }
}
