package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.geometry.Point2

/** The built-in templates by their older names. New templates only need [SkeletonTemplates]. */
enum class ActorTemplate(val skeleton: SkeletonTemplate) {
    HUMAN(HumanSkeleton),
    ANIMAL(FourLeggedSkeleton),
    CUSTOM(ScratchSkeleton),
}

/** Builds a rig from any [SkeletonTemplate]; stable semantic IDs let motion target roles. */
object ActorTemplates {
    fun create(template: ActorTemplate, stage: StageDimensions, view: ActorView = ActorView.ORIGINAL): SceneGraph =
        create(template.skeleton, stage, view)

    fun create(template: SkeletonTemplate, stage: StageDimensions, view: ActorView = ActorView.ORIGINAL): SceneGraph {
        val bones = linkedMapOf<String, BoneNode>()
        val worlds = mutableMapOf<String, Pair<Point2, Float>>()
        val scale = minOf(stage.width / 1000f, stage.height / 800f)
        val origin = Point2(stage.width / 2f, stage.height / 2f)
        template.bones(view).forEach { bone ->
            val start = origin + bone.from * scale
            val end = origin + bone.to * scale
            val angle = RigGeometry.angle(end - start)
            val parentWorld = worlds[bone.parentId]
            val local = RigGeometry.rotate(start - (parentWorld?.first ?: Point2.Zero), -(parentWorld?.second ?: 0f))
            val node = BoneNode(
                hierarchy = BoneHierarchy(bone.id, bone.name, bone.parentId),
                rest = BoneRest(
                    (end - start).getDistance(),
                    localTransform = Transform(local.x, local.y, angle - (parentWorld?.second ?: 0f)),
                    ikChainRootId = bone.chainRootId,
                ),
            )
            bones[bone.id] = bone.drawOrder?.let { node.withDisplay(drawOrder = it) } ?: node
            worlds[bone.id] = start to angle
        }
        val graph = SceneGraph(bones.values.filter { it.parentId == null }.map { it.id }, bones)
        return template.figure(graph)?.let { graph.copy(figure = it) } ?: graph
    }
}
