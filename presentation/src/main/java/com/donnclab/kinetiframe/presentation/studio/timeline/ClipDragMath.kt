package com.donnclab.kinetiframe.presentation.studio.timeline

import kotlin.math.roundToInt

/** Quantise the complete gesture, never individual MotionEvents: slow drags must not disappear. */
internal class ClipDragMath(private val pixelsPerFrame: Float) {
    private var pixels = 0f
    fun add(delta: Float): Int {
        if (delta.isFinite()) pixels += delta
        return if (pixelsPerFrame > 0f) (pixels / pixelsPerFrame).roundToInt() else 0
    }
}
