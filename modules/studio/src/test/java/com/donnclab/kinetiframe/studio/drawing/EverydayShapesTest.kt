// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.drawing

import org.junit.Assert.assertEquals
import org.junit.Test

/** R3-30: line, rectangle and ellipse join the construction guides. */
class EverydayShapesTest {
    private val bounds = ShapeBounds(startX = 10f, startY = 40f, endX = 50f, endY = 20f)

    @Test fun aLineRunsFromWhereTheDragStartedToWhereItEnded() {
        val line = ConstructionShapeGeometry.sample(ConstructionShape.LINE, bounds).single()
        assertEquals(10f to 40f, line.first().x to line.first().y)
        assertEquals(50f to 20f, line.last().x to line.last().y)
    }

    @Test fun aRectangleIsClosedAtItsCorners() {
        val rectangle = ConstructionShapeGeometry.sample(ConstructionShape.RECTANGLE, bounds).single()
        assertEquals(listOf(10f to 20f, 50f to 20f, 50f to 40f, 10f to 40f, 10f to 20f), rectangle.map { it.x to it.y })
    }

    @Test fun anEllipseStaysInsideTheDraggedBox() {
        val ellipse = ConstructionShapeGeometry.sample(ConstructionShape.ELLIPSE, bounds).single()
        assert(ellipse.all { it.x in 9.99f..50.01f && it.y in 19.99f..40.01f })
    }
}
