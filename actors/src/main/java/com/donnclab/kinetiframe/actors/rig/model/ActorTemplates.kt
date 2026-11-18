package com.donnclab.kinetiframe.actors.rig.model

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.document.StageDimensions

// Internal identifiers remain readable for saved actors; creation now describes topology,
// never a stock character illustration or a promise of automatically generated viewpoints.
// Screens name each template ("Two legs" and so on) from their own string resources.
enum class ActorTemplate { HUMAN, ANIMAL, CUSTOM }

/** Stable semantic IDs allow motion recipes to target roles rather than editable display names. */
object ActorTemplates {
    fun create(template: ActorTemplate, stage: StageDimensions, view: ActorView = ActorView.ORIGINAL): SceneGraph {
        val bones = linkedMapOf<String, BoneNode>()
        val worlds = mutableMapOf<String, Pair<Offset, Float>>()
        val scale = minOf(stage.width / 1000f, stage.height / 800f)
        val origin = Offset(stage.width / 2f, stage.height / 2f)

        /** [from] and [to] are the bone's ends in template units, around the stage centre. */
        fun add(id: String, name: String, parent: String?, from: Offset, to: Offset, chain: String? = null) {
            val start = origin + from * scale
            val end = origin + to * scale
            val angle = RigGeometry.angle(end - start)
            val parentWorld = worlds[parent]
            val local = RigGeometry.rotate(start - (parentWorld?.first ?: Offset.Zero), -(parentWorld?.second ?: 0f))
            bones[id] = BoneNode(
                hierarchy = BoneHierarchy(id, name, parent),
                rest = BoneRest(
                    (end - start).getDistance(),
                    localTransform = Transform(
                        local.x,
                        local.y,
                        angle - (parentWorld?.second ?: 0f),
                    ),
                    ikChainRootId = chain,
                ),
            )
            worlds[id] = start to angle
        }
        if (template == ActorTemplate.CUSTOM) {
            // Deliberately just one root part -- "start from scratch" means the animator places
            // every bone themselves in Actor Studio; this single root is the minimum a
            // SceneGraph needs to be structurally valid (see ActorRevision.validate).
            add("root", "Root", null, Offset(0f, 0f), Offset(0f, -60f))
            return SceneGraph(bones.values.filter { it.parentId == null }.map { it.id }, bones)
        }
        if (template == ActorTemplate.HUMAN) {
            add("torso", "Torso", null, Offset(0f, 80f), Offset(0f, -130f))
            add("head", "Head", "torso", Offset(0f, -130f), Offset(0f, -230f))
            for ((side, sign) in listOf("left" to -1f, "right" to 1f)) {
                val profile = view == ActorView.PROFILE
                val shoulder = if (profile) 0f else sign * 20
                val elbow = if (profile) 35f else sign * 130
                val wrist = if (profile) 75f else sign * 165
                val hip = if (profile) 0f else sign * 30
                val knee = if (profile) 45f else sign * 60
                val ankle = if (profile) -10f else sign * 40
                add("${side}_upper_arm", "$side upper arm", "torso", Offset(shoulder, -120f), Offset(elbow, -30f))
                add(
                    "${side}_forearm",
                    "$side forearm",
                    "${side}_upper_arm",
                    Offset(elbow, -30f),
                    Offset(wrist, 80f),
                    "${side}_upper_arm",
                )
                add(
                    "${side}_hand",
                    "$side hand",
                    "${side}_forearm",
                    Offset(wrist, 80f),
                    Offset(wrist + if (profile) 30f else sign * 10, 115f),
                )
                add("${side}_thigh", "$side thigh", "torso", Offset(hip, 80f), Offset(knee, 205f))
                add(
                    "${side}_shin",
                    "$side shin",
                    "${side}_thigh",
                    Offset(knee, 205f),
                    Offset(ankle, 325f),
                    "${side}_thigh",
                )
                add(
                    "${side}_foot",
                    "$side foot",
                    "${side}_shin",
                    Offset(ankle, 325f),
                    Offset(ankle + if (profile) 65f else sign * 50, 330f),
                )
                if (profile) {
                    val far = side == "left"
                    listOf("upper_arm", "forearm", "hand", "thigh", "shin", "foot").forEachIndexed { index, role ->
                        val id = "${side}_$role"
                        bones[id] = bones.getValue(id).withDisplay(drawOrder = if (far) index else 8 + index)
                    }
                }
            }
            if (view == ActorView.PROFILE) {
                bones["torso"] = bones.getValue("torso").withDisplay(drawOrder = 6)
                bones["head"] = bones.getValue("head").withDisplay(drawOrder = 14)
            }
        } else {
            add("spine", "Spine", null, Offset(-170f, -30f), Offset(150f, -30f))
            add("neck", "Neck", "spine", Offset(150f, -30f), Offset(190f, -120f))
            add("head", "Head", "neck", Offset(190f, -120f), Offset(275f, -100f))
            add("tail", "Tail", "spine", Offset(-170f, -30f), Offset(-285f, -100f))
            for ((pair, x) in listOf("back" to -140f, "front" to 130f)) {
                for ((side, offset) in listOf("left" to -25f, "right" to 25f)) {
                    val role = "${pair}_$side"
                    add(
                        "${role}_upper",
                        "$pair $side upper leg",
                        "spine",
                        Offset(x + offset, -30f),
                        Offset(x + offset - 30, 95f),
                    )
                    add(
                        "${role}_lower",
                        "$pair $side lower leg",
                        "${role}_upper",
                        Offset(x + offset - 30, 95f),
                        Offset(x + offset, 225f),
                        "${role}_upper",
                    )
                    add(
                        "${role}_paw",
                        "$pair $side paw",
                        "${role}_lower",
                        Offset(x + offset, 225f),
                        Offset(x + offset + 40, 225f),
                    )
                }
            }
        }
        return SceneGraph(bones.values.filter { it.parentId == null }.map { it.id }, bones)
    }
}
