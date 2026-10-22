package com.donnclab.kinetiframe.actors.rig.model

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.project.model.StageDimensions

// Internal identifiers remain readable for saved actors; creation now describes topology,
// never a stock character illustration or a promise of automatically generated viewpoints.
enum class ActorTemplate(val title: String) { HUMAN("Two legs"), ANIMAL("Four supports"), CUSTOM("Empty root") }

/** Stable semantic IDs allow motion recipes to target roles rather than editable display names. */
object ActorTemplates {
    fun create(template: ActorTemplate, stage: StageDimensions, view: ActorView = ActorView.ORIGINAL): SceneGraph {
        val bones = linkedMapOf<String, BoneNode>()
        val worlds = mutableMapOf<String, Pair<Offset, Float>>()
        val scale = minOf(stage.width / 1000f, stage.height / 800f)
        val origin = Offset(stage.width / 2f, stage.height / 2f)
        fun add(
            id: String,
            name: String,
            parent: String?,
            x: Float,
            y: Float,
            ex: Float,
            ey: Float,
            chain: String? = null,
        ) {
            val start = origin + Offset(x, y) * scale
            val end = origin + Offset(ex, ey) * scale
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
            add("root", "Root", null, 0f, 0f, 0f, -60f)
            return SceneGraph(bones.values.filter { it.parentId == null }.map { it.id }, bones)
        }
        if (template == ActorTemplate.HUMAN) {
            add("torso", "Torso", null, 0f, 80f, 0f, -130f)
            add("head", "Head", "torso", 0f, -130f, 0f, -230f)
            for ((side, sign) in listOf("left" to -1f, "right" to 1f)) {
                val profile = view == ActorView.PROFILE
                val shoulder = if (profile) 0f else sign * 20
                val elbow = if (profile) 35f else sign * 130
                val wrist = if (profile) 75f else sign * 165
                val hip = if (profile) 0f else sign * 30
                val knee = if (profile) 45f else sign * 60
                val ankle = if (profile) -10f else sign * 40
                add("${side}_upper_arm", "$side upper arm", "torso", shoulder, -120f, elbow, -30f)
                add(
                    "${side}_forearm",
                    "$side forearm",
                    "${side}_upper_arm",
                    elbow,
                    -30f,
                    wrist,
                    80f,
                    "${side}_upper_arm",
                )
                add(
                    "${side}_hand",
                    "$side hand",
                    "${side}_forearm",
                    wrist,
                    80f,
                    wrist + if (profile) 30f else sign * 10,
                    115f,
                )
                add("${side}_thigh", "$side thigh", "torso", hip, 80f, knee, 205f)
                add("${side}_shin", "$side shin", "${side}_thigh", knee, 205f, ankle, 325f, "${side}_thigh")
                add(
                    "${side}_foot",
                    "$side foot",
                    "${side}_shin",
                    ankle,
                    325f,
                    ankle + if (profile) 65f else sign * 50,
                    330f,
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
            add("spine", "Spine", null, -170f, -30f, 150f, -30f)
            add("neck", "Neck", "spine", 150f, -30f, 190f, -120f)
            add("head", "Head", "neck", 190f, -120f, 275f, -100f)
            add("tail", "Tail", "spine", -170f, -30f, -285f, -100f)
            for ((pair, x) in listOf("back" to -140f, "front" to 130f)) {
                for ((side, offset) in listOf("left" to -25f, "right" to 25f)) {
                    val role = "${pair}_$side"
                    add("${role}_upper", "$pair $side upper leg", "spine", x + offset, -30f, x + offset - 30, 95f)
                    add(
                        "${role}_lower",
                        "$pair $side lower leg",
                        "${role}_upper",
                        x + offset - 30,
                        95f,
                        x + offset,
                        225f,
                        "${role}_upper",
                    )
                    add("${role}_paw", "$pair $side paw", "${role}_lower", x + offset, 225f, x + offset + 40, 225f)
                }
            }
        }
        return SceneGraph(bones.values.filter { it.parentId == null }.map { it.id }, bones)
    }
}
