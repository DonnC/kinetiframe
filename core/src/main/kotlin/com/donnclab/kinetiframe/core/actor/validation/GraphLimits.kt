// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

internal object GraphLimits {
    const val MAX_PARTS = 512
    const val MAX_VARIANTS = 24
    const val MAX_VARIANT_NAME = 60
    val VARIANT_ID = Regex("[A-Za-z0-9_-]{1,80}")
    val ARTWORK_SCALE = .001f..100f
    val SOURCE_DIMENSION = 1..2048
    val GRID_DIMENSION = 1..32
    const val MAX_CUTOUT_INFLUENCES = 4
    const val MAX_DRAWING_INFLUENCES = 64
    const val MAX_OVERLAYS = 64
    const val MAX_REGIONS = 32
    val REGION_POINTS = 3..256
    val FEATHER = 0f..128f
    val NORMALIZED = 0f..1f
    const val MAX_VERTICES = 16_384
    const val WEIGHT_TOLERANCE = .0001f
}
