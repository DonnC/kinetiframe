package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.studio.domain.ViewManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GridDefaultsTest {
    @Test fun `new workspaces use a uniform thirty pixel grid at eight percent opacity`() {
        val canvas = StudioState().canvas
        assertEquals(30f, canvas.gridSpacingHorizontal)
        assertEquals(30f, canvas.gridSpacingVertical)
        assertEquals(.08f, canvas.gridOpacity)
    }

    @Test fun `reset restores the same defaults after custom settings`() {
        var state = StudioState()
        val manager = ViewManager { transform -> state = transform(state) }
        manager.setGridSpacingVertical(90f)
        manager.setGridSpacingHorizontal(15f)
        manager.setGridOpacity(.6f)
        manager.resetGridSettings()
        assertEquals(30f, state.canvas.gridSpacingHorizontal)
        assertEquals(30f, state.canvas.gridSpacingVertical)
        assertEquals(.08f, state.canvas.gridOpacity)
    }
}
