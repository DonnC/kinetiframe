package com.donnclab.kinetiframe.library.model

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkAlignment
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.Serializable

/** Portable source registration, not a copied bind-world matrix from another actor. */
@Serializable
data class PackArtworkRegistration(
    val partId: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val boneLength: Float,
    val pivotX: Float,
    val pivotY: Float,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val scale: Float = 1f,
    val continuousChildId: String? = null,
    val attachmentBlend: Boolean = false,
) {
    fun validate() = apply {
        require(partId.isNotBlank() && partId.length <= 100)
        require(sourceWidth in 1..8192 && sourceHeight in 1..8192)
        require(listOf(boneLength, pivotX, pivotY, offsetX, offsetY, rotationDegrees, scale).all(Float::isFinite))
        require(boneLength > 0f && scale in .001f..100f)
        require(continuousChildId == null || (continuousChildId.isNotBlank() && continuousChildId.length <= 100))
        require(!attachmentBlend || continuousChildId == null)
    }
    fun alignment(targetPart: String, targetLength: Float, decodedWidth: Int, decodedHeight: Int): TextureBinding? {
        validate()
        if (targetPart != partId) return null // Another slot remains a normal visual placement, never guessed.
        val ratio = targetLength / boneLength
        return TextureBinding(
            "pending",
            Point2(
                pivotX * decodedWidth / sourceWidth,
                pivotY * decodedHeight / sourceHeight,
            ),
            ArtworkAlignment(
                Point2(offsetX * ratio, offsetY * ratio),
                rotationDegrees,
                (scale * ratio * sourceWidth / decodedWidth).coerceIn(.001f, 100f),
            ),
        )
    }
}
