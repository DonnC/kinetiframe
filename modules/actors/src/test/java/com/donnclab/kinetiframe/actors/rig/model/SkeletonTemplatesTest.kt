package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.actors.domain.ActorTemplateContract
import com.donnclab.kinetiframe.actors.rig.math.ActorPoseEditor
import com.donnclab.kinetiframe.actors.rig.math.PoseHandles
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.validation.ActorGraphValidation
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkeletonTemplatesTest {
    private val stage = StageDimensions.LandscapeHd

    @Test fun `every registered template builds a rig that keeps its own promises`() {
        val ids = SkeletonTemplates.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        SkeletonTemplates.all.forEach { template ->
            listOf(ActorView.ORIGINAL, ActorView.FRONT, ActorView.PROFILE).forEach { view ->
                val graph = ActorTemplates.create(template, stage, view)
                ActorGraphValidation.validate(graph)
                ActorTemplateContract.validate(template.id, graph)
                assertTrue(PoseHandles.ids(graph).isNotEmpty(), "${template.id} has something to drag")
            }
        }
    }

    @Test fun `an unknown template, such as one from a newer app, starts from scratch`() {
        assertSame(ScratchSkeleton, SkeletonTemplates.byId("robot-v3"))
        ActorTemplateContract.validate("robot-v3", ActorTemplates.create(ScratchSkeleton, stage))
    }

    @Test fun `the contract rejects a rig that drops a role its template promises`() {
        val human = ActorTemplates.create(HumanSkeleton, stage)
        assertThrows(IllegalArgumentException::class.java) {
            ActorTemplateContract.validate(HumanSkeleton.id, human.copy(bones = human.bones - "left_hand"))
        }
    }

    /** What a contributor writes for a new kind of actor: only bones. */
    private object FlagSkeleton : SkeletonTemplate {
        override val id = "flag-v1"
        override val defaultName = "Flag"
        override val roles = setOf("pole", "cloth_near", "cloth_far")
        override val rootId = "pole"
        override fun bones(view: ActorView) = listOf(
            TemplateBone("pole", "Pole", null, Point2(0f, 300f), Point2(0f, -200f)),
            TemplateBone("cloth_near", "Cloth near", "pole", Point2(0f, -200f), Point2(150f, -200f)),
            TemplateBone(
                "cloth_far",
                "Cloth far",
                "cloth_near",
                Point2(150f, -200f),
                Point2(300f, -200f),
                "cloth_near",
            ),
        )
    }

    @Test fun `a contributor's template gets posing and validation without other changes`() {
        val flag = ActorTemplates.create(FlagSkeleton, stage)
        ActorGraphValidation.validate(flag)
        ActorTemplateContract.validate(FlagSkeleton.id, flag)
        assertEquals(setOf("cloth_far"), PoseHandles.ids(flag))
        // Dragging the flag's far corner down bends the cloth chain through the shared IK.
        val before = RigGeometry.resolve(flag).getValue("cloth_far").end
        val target = before + Point2(-40f, 120f)
        val posed = ActorPoseEditor.pose(flag, "cloth_far", target)
        val after = RigGeometry.resolve(posed).getValue("cloth_far").end
        assertNotEquals(before, after)
        assertTrue((after - target).getDistance() < 2f, "corner reached the finger: $after")
    }
}
