package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.figure.DrawnFigure
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2

/**
 * A ready-made skeleton for a common kind of actor: a person, a four-legged animal, and later a
 * bird, a car or anything animators often need. A template only describes bones. Everything else
 * is shared by every actor whatever its template: posing and IK, pose keys, recording, presets,
 * performances, drawing and saving.
 *
 * To add one: implement this interface and add it to [SkeletonTemplates.all], then give it a
 * name in the app's string resources. Its [id] is saved in actors and must never change.
 */
interface SkeletonTemplate {
    /** Saved in each actor's manifest, such as `human-v1`. Never rename it. */
    val id: String

    /** An English name for tools and logs; screens show a translated name from resources. */
    val defaultName: String

    /** Bones that presets and saved motion may rely on; an actor of this template keeps them all. */
    val roles: Set<String>

    /** The root bone the template promises, or null when the rig's root is free. */
    val rootId: String?

    /** The bones in template units around the stage centre, parents before children. */
    fun bones(view: ActorView): List<TemplateBone>

    /** A starting drawing for the new rig, or null to start with no artwork. */
    fun figure(graph: SceneGraph): DrawnFigure? = null
}

/**
 * One bone of a template, from [from] to [to] in template units: a 1000 by 800 box around the
 * stage centre. [chainRootId] makes the bone solve with its ancestors when its tip is dragged.
 */
data class TemplateBone(
    val id: String,
    val name: String,
    val parentId: String?,
    val from: Point2,
    val to: Point2,
    val chainRootId: String? = null,
    val drawOrder: Int? = null,
)

/** Every skeleton the app offers. Add new templates here. */
object SkeletonTemplates {
    val all: List<SkeletonTemplate> = listOf(HumanSkeleton, FourLeggedSkeleton, ScratchSkeleton)

    /** An unknown ID, such as one saved by a newer app, starts from scratch rather than failing. */
    fun byId(id: String): SkeletonTemplate = all.firstOrNull { it.id == id } ?: ScratchSkeleton
}

/** Two arms and two legs. Profile views put the far limbs behind the body. */
object HumanSkeleton : SkeletonTemplate {
    override val id = ActorManifest.HUMAN_TEMPLATE_ID
    override val defaultName = "Two legs"
    override val rootId = "torso"
    override val roles = setOf("torso", "head") +
        listOf("left", "right").flatMap { side ->
            listOf("upper_arm", "forearm", "hand", "thigh", "shin", "foot").map { "${side}_$it" }
        }

    override fun bones(view: ActorView): List<TemplateBone> {
        val profile = view == ActorView.PROFILE
        val bones = mutableListOf(
            TemplateBone(
                "torso",
                "Torso",
                null,
                Point2(0f, 80f),
                Point2(0f, -130f),
                drawOrder = if (profile) 6 else null,
            ),
            TemplateBone(
                "head",
                "Head",
                "torso",
                Point2(0f, -130f),
                Point2(0f, -230f),
                drawOrder = if (profile) 14 else null,
            ),
        )
        for ((side, sign) in listOf("left" to -1f, "right" to 1f)) {
            val shoulder = if (profile) 0f else sign * 20
            val elbow = if (profile) 35f else sign * 130
            val wrist = if (profile) 75f else sign * 165
            val hip = if (profile) 0f else sign * 30
            val knee = if (profile) 45f else sign * 60
            val ankle = if (profile) -10f else sign * 40

            // In profile the left side is the far side, drawn behind the body.
            fun order(index: Int) = if (!profile) {
                null
            } else if (side == "left") {
                index
            } else {
                8 + index
            }
            bones += TemplateBone(
                "${side}_upper_arm",
                "$side upper arm",
                "torso",
                Point2(shoulder, -120f),
                Point2(elbow, -30f),
                drawOrder = order(0),
            )
            bones += TemplateBone(
                "${side}_forearm",
                "$side forearm",
                "${side}_upper_arm",
                Point2(elbow, -30f),
                Point2(wrist, 80f),
                "${side}_upper_arm",
                order(1),
            )
            bones += TemplateBone(
                "${side}_hand",
                "$side hand",
                "${side}_forearm",
                Point2(wrist, 80f),
                Point2(wrist + if (profile) 30f else sign * 10, 115f),
                drawOrder = order(2),
            )
            bones += TemplateBone(
                "${side}_thigh",
                "$side thigh",
                "torso",
                Point2(hip, 80f),
                Point2(knee, 205f),
                drawOrder = order(3),
            )
            bones += TemplateBone(
                "${side}_shin",
                "$side shin",
                "${side}_thigh",
                Point2(knee, 205f),
                Point2(ankle, 325f),
                "${side}_thigh",
                order(4),
            )
            bones += TemplateBone(
                "${side}_foot",
                "$side foot",
                "${side}_shin",
                Point2(ankle, 325f),
                Point2(ankle + if (profile) 65f else sign * 50, 330f),
                drawOrder = order(5),
            )
        }
        return bones
    }
}

/** A spine, neck, head, tail and four legs: dogs, cats, horses, cows. */
object FourLeggedSkeleton : SkeletonTemplate {
    override val id = ActorManifest.ANIMAL_TEMPLATE_ID
    override val defaultName = "Four legs"
    override val rootId = "spine"
    private val corners = listOf("back_left", "back_right", "front_left", "front_right")
    override val roles = setOf("spine", "neck", "head", "tail") +
        corners.flatMap { corner -> listOf("${corner}_upper", "${corner}_lower", "${corner}_paw") }

    override fun bones(view: ActorView): List<TemplateBone> {
        val bones = mutableListOf(
            TemplateBone("spine", "Spine", null, Point2(-170f, -30f), Point2(150f, -30f)),
            TemplateBone("neck", "Neck", "spine", Point2(150f, -30f), Point2(190f, -120f)),
            TemplateBone("head", "Head", "neck", Point2(190f, -120f), Point2(275f, -100f)),
            TemplateBone("tail", "Tail", "spine", Point2(-170f, -30f), Point2(-285f, -100f)),
        )
        for ((pair, x) in listOf("back" to -140f, "front" to 130f)) {
            for ((side, offset) in listOf("left" to -25f, "right" to 25f)) {
                val role = "${pair}_$side"
                bones += TemplateBone(
                    "${role}_upper",
                    "$pair $side upper leg",
                    "spine",
                    Point2(x + offset, -30f),
                    Point2(x + offset - 30, 95f),
                )
                bones += TemplateBone(
                    "${role}_lower",
                    "$pair $side lower leg",
                    "${role}_upper",
                    Point2(x + offset - 30, 95f),
                    Point2(x + offset, 225f),
                    "${role}_upper",
                )
                bones += TemplateBone(
                    "${role}_paw",
                    "$pair $side paw",
                    "${role}_lower",
                    Point2(x + offset, 225f),
                    Point2(x + offset + 40, 225f),
                )
            }
        }
        return bones
    }
}

/**
 * Start from scratch: one root bone, the minimum a valid rig needs. The animator adds every other
 * bone, so this can become a person, a bat, a flag or anything else.
 */
object ScratchSkeleton : SkeletonTemplate {
    override val id = ActorManifest.CUSTOM_TEMPLATE_ID
    override val defaultName = "Start from scratch"
    override val rootId: String? = null
    override val roles = emptySet<String>()

    override fun bones(view: ActorView) = listOf(TemplateBone("root", "Root", null, Point2(0f, 0f), Point2(0f, -60f)))
}
