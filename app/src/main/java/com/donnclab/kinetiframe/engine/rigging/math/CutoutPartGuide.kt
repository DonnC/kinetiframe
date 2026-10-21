package com.donnclab.kinetiframe.engine.rigging.math

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min

/**
 * The drawing contract for one rigid cut-out part.
 *
 * This is deliberately derived from the two existing, freely movable bone nodes rather than
 * persisted as another set of animation nodes. The start/end points are the exact attachment
 * boundaries the renderer already uses, while the envelope and overlap values only make that
 * contract visible to the animator.
 */
data class CutoutPartGuide(
    val partId: String,
    val start: Offset,
    val end: Offset,
    val envelopeRadius: Float,
    val overlapRadius: Float,
)

object CutoutPartGuideFactory {
    /**
     * Creates a bounded capsule guide. Long limbs gain enough room for clothing and line weight,
     * but the cap prevents an oversized guide from swallowing neighbouring parts.
     */
    fun create(partId: String, start: Offset, end: Offset): CutoutPartGuide {
        val length = (end - start).getDistance()
        val envelope = min(48f, max(22f, length * 0.18f))
        return CutoutPartGuide(
            partId = partId,
            start = start,
            end = end,
            envelopeRadius = envelope,
            // Deliberate overlap hides seams at a rotating joint; it is a visual/cropping
            // allowance, never a second influence or an additional animated node.
            overlapRadius = envelope * 0.72f,
        )
    }
}
