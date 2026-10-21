package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.ActorGraphValidation
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates
import com.donnclab.kinetiframe.engine.rigging.model.ActorView
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ActorGraphValidationTest {
    @Test fun `every template and authored view starts with a valid hierarchy`() {
        ActorTemplate.entries.forEach { template ->
            ActorView.entries.forEach { view ->
                ActorGraphValidation.validate(ActorTemplates.create(template, StageDimensions.LandscapeHd, view))
            }
        }
    }

    @Test fun `cycles unknown parents invalid IK and non finite transforms are rejected`() {
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val head = graph.bones.getValue("head")
        listOf(
            head.withHierarchy(parentId = "head"),
            head.withHierarchy(parentId = "missing"),
            head.withRest(ikChainRootId = "left_thigh"),
            head.withRest(
                localTransform = head.localTransform.copy(
                    translation = com.donnclab.kinetiframe.core.geometry.Point2(Float.NaN, head.localTransform.y),
                ),
            ),
        ).forEach { bad ->
            assertThrows(IllegalArgumentException::class.java) {
                ActorGraphValidation.validate(
                    graph.copy(
                        bones =
                        graph.bones + (head.id to bad),
                    ),
                )
            }
        }
    }
}
