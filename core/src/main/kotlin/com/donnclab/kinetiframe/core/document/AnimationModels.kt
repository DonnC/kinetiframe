package com.donnclab.kinetiframe.core.document

import kotlinx.serialization.Serializable

@Serializable
data class Point(val x: Float, val y: Float)

/**
 * Represents a skeletal bone in the rigging system.
 */
@Serializable
data class Bone(
    val name: String,
    val position: Point,
    val rotation: Float,
    val length: Float,
    val parentBoneName: String? = null,
)

/**
 * Represents the transformation state of a bone at a specific keyframe.
 */
@Serializable
data class BoneTransformation(
    val boneName: String,
    val position: Point,
    val rotation: Float,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
)

/**
 * Represents a pose at a specific time in the animation.
 */
@Serializable
data class Keyframe(val frameIndex: Int, val transformations: List<BoneTransformation> = emptyList())

/** The render/storage contract for a layer in the spatial-temporal matrix. */
@Serializable
enum class LayerType {
    BACKGROUND,
    RASTER,
    PUPPET,
}

/**
 * Represents an animation layer. Puppet layers reference a project-owned instance.
 */
@Serializable
data class Layer(
    val name: String,
    val keyframes: List<Keyframe> = emptyList(),
    val isVisible: Boolean = true,
    val id: String = name,
    val type: LayerType = LayerType.RASTER,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val zIndex: Int = 0,
    val puppetInstanceId: String? = null,
    /** Frames with content in this layer; storage remains in the bitmap cache. */
    val populatedFrames: Set<Int> = emptySet(),
    /**
     * When set, this layer is "armed" to a bone: anything sketched on it auto-binds to that
     * bone as its texture,
     * with the bone's own joint becoming the pivot automatically -- no manual lasso-cut or
     * pivot-drag needed. Created by tapping a bone in Setup (see [LayerManager.addLayer]).
     */
    val boundBoneId: String? = null,
)
