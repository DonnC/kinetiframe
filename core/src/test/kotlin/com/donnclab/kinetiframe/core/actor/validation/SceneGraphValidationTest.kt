// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

import com.donnclab.kinetiframe.core.actor.artwork.DrawingComposition
import com.donnclab.kinetiframe.core.actor.artwork.SkinTransformException
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class SceneGraphValidationTest {
    @TestFactory fun `every validation rule has a typed rejection fixture`(): List<DynamicTest> {
        val cases = GraphValidationFixtures.invalidCases()
        assertEquals(GraphValidationIssue.entries.toSet(), cases.keys)
        return cases.map { (issue, graph) ->
            DynamicTest.dynamicTest(issue.name) {
                val failure =
                    assertThrows(GraphValidationException::class.java) { SceneGraphValidation.requireValid(graph) }
                assertEquals(issue, failure.issue)
            }
        }
    }

    @Test fun `valid hierarchy cutout and drawing with overlays are accepted without mutation`() {
        val graph = GraphValidationFixtures.graph
        val cutout = GraphValidationFixtures.cutout()
        val drawing = graph.copy(
            drawing = GraphValidationFixtures.drawing().copy(
                composition = DrawingComposition(overlayParts = setOf("child"), underlayParts = setOf("root")),
            ),
        )
        listOf(graph, cutout, drawing).forEach { candidate ->
            val before = candidate.copy()
            SceneGraphValidation.requireValid(candidate)
            assertEquals(before, candidate)
        }
    }

    @Test fun `maximum hierarchy depth is iterative and count overflow is rejected`() {
        val ids = (0 until 512).map(Int::toString)
        val bones = ids.withIndex().associate { (index, id) ->
            id to BoneNode(id, ids.getOrNull(index - 1), id, 1f)
        }
        val graph = SceneGraph(listOf("0"), bones)
        SceneGraphValidation.requireValid(graph)
        val overflow = graph.copy(bones = graph.bones + ("512" to BoneNode("512", "511", "512", 1f)))
        assertEquals(
            GraphValidationIssue.PART_COUNT,
            assertThrows(GraphValidationException::class.java) { SceneGraphValidation.requireValid(overflow) }.issue,
        )
    }

    @Test fun `vertex budget accepts the last complete grids below its limit`() {
        SceneGraphValidation.requireValid(GraphValidationFixtures.tiled(15))
        assertEquals(
            GraphValidationIssue.GRID_BUDGET,
            assertThrows(GraphValidationException::class.java) {
                SceneGraphValidation.requireValid(GraphValidationFixtures.tiled(16))
            }.issue,
        )
    }

    @Test fun `singular bind retains math cause for boundary diagnostics`() {
        val failure = assertThrows(GraphValidationException::class.java) {
            SceneGraphValidation.requireValid(
                GraphValidationFixtures.cutout(GraphValidationFixtures.skin(matrix = GraphValidationFixtures.singular)),
            )
        }
        assertEquals(GraphValidationIssue.SINGULAR_BIND, failure.issue)
        assertInstanceOf(SkinTransformException::class.java, failure.cause)
    }

    @Test fun `first failure order remains roots before invalid parts`() {
        val graph = GraphValidationFixtures.graph.copy(
            rootBoneIds = emptyList(),
            bones = mapOf("root" to GraphValidationFixtures.root.withRest(length = -1f)),
        )
        assertEquals(
            GraphValidationIssue.ROOTS,
            assertThrows(GraphValidationException::class.java) { SceneGraphValidation.requireValid(graph) }.issue,
        )
    }

    @Test fun `shared weights retain normalized and sparse policy distinctions`() {
        val dense = SkinVertex(0f, 0f, List(5) { .2f })
        assertTrue(SkinValidation.validWeights(dense, 5, SkinWeightPolicy.NORMALIZED))
        assertFalse(SkinValidation.validWeights(dense, 5, SkinWeightPolicy.SPARSE))
        val sparse = SkinVertex(0f, 0f, listOf(.5f, 0f, .5f, 0f, 0f))
        assertTrue(SkinValidation.validWeights(sparse, 5, SkinWeightPolicy.SPARSE))
        assertFalse(SkinValidation.validWeights(sparse, 4, SkinWeightPolicy.SPARSE))
    }
}
