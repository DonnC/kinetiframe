package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.project.model.Layer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerMatrixTest {
    @Test
    fun orderedLayers_follow_z_index() {
        val back = Layer("Back", id = "back", zIndex = 0)
        val front = Layer("Front", id = "front", zIndex = 2)
        val middle = Layer("Middle", id = "middle", zIndex = 1)

        assertEquals(
            listOf("back", "middle", "front"),
            LayerMatrix(listOf(front, back, middle)).orderedLayers.map { it.id },
        )
    }

    @Test
    fun locked_layer_cannot_be_active() {
        val locked = Layer("Locked", id = "locked", isLocked = true)

        assertTrue(LayerMatrix(listOf(locked)).activeLayer(0) == null)
    }
}
