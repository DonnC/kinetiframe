// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.figure

import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import kotlinx.serialization.Serializable

/**
 * Any actor drawn with curves instead of image cutouts: a person, an animal, a bat, a flag or an
 * object. The skeleton can have any shape; nothing here knows about arms or legs. Three kinds of
 * piece cover every thing:
 *
 * - [FigurePart]: a rigid drawing on one bone, with named variants (a head, hand poses, a hoof).
 * - [FigureTube]: an inked tube along a chain of bones that bends smoothly at every joint
 *   (arms, legs, tails, necks, tentacles, ropes).
 * - [FigureSurface]: a drawing that bends with several bones at once, each point following the
 *   bones nearest to it (flag cloth, a bat's wing skin, a bending body, a cape).
 *
 * Colours are named in [palette] so a character can be recoloured.
 */
@Serializable
data class DrawnFigure(
    val palette: Map<String, String>,
    val ink: String = "ink",
    val lineWidth: Float = 4f,
    val tubes: List<FigureTube> = emptyList(),
    val parts: List<FigurePart> = emptyList(),
    val surfaces: List<FigureSurface> = emptyList(),
    /** The variant shown for each part's bone, set by artwork keys while a performance plays. */
    val selections: Map<String, String> = emptyMap(),
) {
    fun select(bySelectedBone: Map<String, String>): DrawnFigure {
        val chosen = bySelectedBone.filter { (bone, variant) ->
            parts.any { it.boneId == bone && variant in it.variants }
        }
        return if (chosen.isEmpty()) this else copy(selections = selections + chosen)
    }
}

/**
 * A tube drawn through [boneIds], a parent-to-child chain: from the first bone's start, round
 * each joint, to the last bone's end. Its width runs from [width] to [endWidth] for tapering tails
 * and horns. Parts on the bone that starts at the tube's end are drawn with it, under a [cuff].
 * Its outline is hidden where it grows out of [rootPartId]'s shape.
 */
@Serializable
data class FigureTube(
    val id: String,
    val boneIds: List<String>,
    val width: Float,
    val fill: String,
    val endWidth: Float = width,
    val drawOrder: Int = 0,
    val cuff: Boolean = false,
    val rootPartId: String? = null,
)

/**
 * A closed drawing attached to [boneId], in that bone's space: the origin is the bone's start
 * and +x runs along the bone. [mirror] flips it across the bone, for the other hand or foot.
 */
@Serializable
data class FigurePart(
    val id: String,
    val boneId: String,
    val variants: Map<String, List<FigureShape>>,
    val drawOrder: Int = 0,
    val mirror: Boolean = false,
) {
    fun shapes(selected: String?): List<FigureShape> =
        variants[selected] ?: variants[DEFAULT_VARIANT] ?: variants.values.first()

    companion object {
        const val DEFAULT_VARIANT = "default"
    }
}

/**
 * A drawing in the actor's rest pose that bends with [bones]. Every curve point follows the bound
 * bones by how near it was to each one in the rest pose, so nothing needs painting by hand.
 */
@Serializable
data class FigureSurface(
    val id: String,
    val bones: List<SurfaceBone>,
    val shapes: List<FigureShape>,
    val drawOrder: Int = 0,
    /** How sharply influence fades with distance; higher keeps each point on its nearest bone. */
    val falloff: Float = 4f,
)

/** A bone a surface follows, with where that bone was in the rest pose the surface was drawn in. */
@Serializable
data class SurfaceBone(val boneId: String, val bindWorld: SkinAffine, val length: Float)

/**
 * One curve in SVG path syntax (absolute M, L, C, Q and Z). [fill] names a palette colour, or
 * null for an open line. [line] scales the figure's line width; 0 draws no outline.
 */
@Serializable
data class FigureShape(val path: String, val fill: String? = null, val line: Float = 1f)
