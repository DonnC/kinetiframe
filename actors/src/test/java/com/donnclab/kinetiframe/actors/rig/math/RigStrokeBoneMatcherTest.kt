package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RigStrokeBoneMatcherTest {
    @Test
    fun `routes a forearm sketch to its closest bone without a selected part`() {
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val forearm = RigGeometry.resolve(graph).getValue("left_forearm")
        val points = listOf(
            forearm.start + (forearm.end - forearm.start) * 0.2f,
            forearm.start + (forearm.end - forearm.start) * 0.55f,
            forearm.start + (forearm.end - forearm.start) * 0.8f,
        )

        assertEquals("left_forearm", RigStrokeBoneMatcher.nearestBoneId(graph, points))
    }
}
