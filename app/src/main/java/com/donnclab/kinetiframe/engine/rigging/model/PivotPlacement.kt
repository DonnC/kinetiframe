package com.donnclab.kinetiframe.engine.rigging.model

/** Temporary interaction state while a newly extracted cutout is being rigged. */
data class PivotPlacement(
    val assetId: String,
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val pivotX: Float,
    val pivotY: Float,
    /** Document-space top-left position of the processed bitmap. */
    val documentLeft: Float,
    val documentTop: Float,
    val definitionId: String? = null,
    val targetBoneId: String? = null,
)
