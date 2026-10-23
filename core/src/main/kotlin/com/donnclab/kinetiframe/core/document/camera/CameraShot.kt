// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.document.camera

import kotlinx.serialization.Serializable

@Serializable
enum class CameraAction { PUSH_IN, PULL_OUT, PAN, TILT, SHAKE, HANDHELD, FADE_IN, FADE_OUT, WHIP_PAN, FOLLOW }

@Serializable
enum class CameraEasing { SMOOTH, LINEAR, EASE_IN, EASE_OUT }

@Serializable
enum class ShakeDirection { BOTH, HORIZONTAL, VERTICAL }

/** Normalized stage framing, never the editor's inspection/pan matrix. */
@Serializable
data class CameraPose(
    val x: Float = .5f,
    val y: Float = .5f,
    val zoom: Float = 1f,
    val rotation: Float = 0f,
    val black: Float = 0f,
) {
    init {
        require(x.isFinite() && y.isFinite() && zoom.isFinite() && rotation.isFinite() && black.isFinite())
        require(x in -1f..2f && y in -1f..2f && zoom in .25f..4f && rotation in -180f..180f && black in 0f..1f)
    }
}

@Serializable
data class CameraShot(
    val id: String,
    val action: CameraAction,
    val startFrame: Int,
    val endFrame: Int,
    val start: CameraPose = CameraPose(),
    val end: CameraPose = CameraPose(),
    val strength: Float = .5f,
    val seed: Int = 1,
    /** Opt-in only: selected ranges otherwise restore the preceding stable camera. */
    val holdEnd: Boolean = false,
    /** Frames reserved inside the selected range, not an extension of its lifetime. */
    val waitFrames: Int = 0,
    val settleFrames: Int = 0,
    val easing: CameraEasing = CameraEasing.SMOOTH,
    /** Null retains the original action-specific frequency for older projects. */
    val frequencyHz: Float? = null,
    val shakeDirection: ShakeDirection = ShakeDirection.BOTH,
    val rotationStrength: Float = 0f,
    val targetPlacementId: String? = null,
    val followSmoothingSeconds: Float = .2f,
) {
    init {
        require(id.isNotBlank() && startFrame >= 0 && endFrame > startFrame && endFrame <= 1_000_000)
        require(strength.isFinite() && strength in 0f..1f)
        require(waitFrames >= 0 && settleFrames >= 0)
        require(frequencyHz == null || (frequencyHz.isFinite() && frequencyHz in .2f..30f))
        require(rotationStrength.isFinite() && rotationStrength in 0f..1f)
    }
    init {
        require(followSmoothingSeconds.isFinite() && followSmoothingSeconds in 0f..1f)
    }
}

/** Rules every saved list of camera shots follows. */
object CameraShots {
    /** At most 256 shots with distinct IDs, and no two shots overlap in time. */
    fun validate(shots: List<CameraShot>) {
        require(shots.size <= 256 && shots.map { it.id }.distinct().size == shots.size)
        shots.sortedBy { it.startFrame }.zipWithNext().forEach { (a, b) ->
            require(a.endFrame <= b.startFrame) { "Camera ranges cannot overlap" }
        }
    }
}
