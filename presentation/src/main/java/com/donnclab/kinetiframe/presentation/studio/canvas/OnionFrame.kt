package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Bitmap

/** Retains temporal distance even when an intervening frame has no pixels. */
data class OnionFrame(val bitmap: Bitmap, val distance: Int)

internal data class OnionSnapshot(
    val frameIndex: Int,
    val layerId: String,
    val past: List<OnionFrame> = emptyList(),
    val future: List<OnionFrame> = emptyList(),
)
