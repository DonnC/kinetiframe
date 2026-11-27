// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RigSetupEditsTest {
    private val graph = RigEditFixtures.graph

    @Test fun `setup fitting resizes upper segment and keeps endpoint child attached`() {
        val target = Point2(110f, 40f)
        val fitted = RigSetupEdits.fit(graph, "upper", target)
        val world = RigidRigGeometry.resolve(fitted)
        assertTrue((world.getValue("upper").end - target).getDistance() < .001f)
        assertEquals(fitted.bones.getValue("upper").length, fitted.bones.getValue("lower").localTransform.x)
        assertTrue((world.getValue("upper").end - world.getValue("lower").start).getDistance() < .001f)
        assertSame(graph.bones.getValue("other"), fitted.bones.getValue("other"))
        assertEquals(80f, graph.bones.getValue("upper").length)
    }

    @Test fun `shared endpoint origin delegates to fitting rather than detaching`() {
        val target = Point2(110f, 40f)
        assertEquals(RigSetupEdits.fit(graph, "upper", target), RigSetupEdits.moveOrigin(graph, "lower", target))
    }

    @Test fun `all root movement translates disconnected roots without changing children`() {
        val moved = RigSetupEdits.moveRoot(graph, "root", Point2(50f, 20f), RootMoveScope.ALL)
        assertEquals(Point2(350f, 70f), moved.bones.getValue("other").localTransform.translation)
        assertSame(graph.bones.getValue("lower"), moved.bones.getValue("lower"))
        graph.bones.forEach { (id, node) -> assertEquals(node.length, moved.bones.getValue(id).length) }
        val selected = RigSetupEdits.moveRoot(graph, "root", Point2(50f, 20f))
        assertSame(graph.bones.getValue("other"), selected.bones.getValue("other"))
    }

    @Test fun `missing parts tiny fitting targets and non finite origin moves retain graph identity`() {
        assertSame(graph, RigSetupEdits.fit(graph, "missing", Point2(50f, 20f)))
        assertSame(graph, RigSetupEdits.fit(graph, "upper", Point2(0f, 20f)))
        assertSame(graph, RigSetupEdits.moveOrigin(graph, "upper", Point2(Float.NaN, 20f)))
        assertSame(graph, RigSetupEdits.moveRoot(graph, "lower", Point2(50f, 20f), RootMoveScope.ALL))
    }
}
