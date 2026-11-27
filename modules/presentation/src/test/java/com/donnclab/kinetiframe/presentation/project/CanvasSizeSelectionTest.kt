// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.project

import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** R3-33: Custom starts from the size already picked, so its fields and label agree. */
class CanvasSizeSelectionTest {
    @Test fun `switching to Custom keeps the size already chosen`() {
        val custom = CanvasSizeSelection(preset = CanvasPresets.TikTok).choose(CanvasPresets.Custom)
        assertEquals(CanvasPresets.TikTok.dimensions, custom.dimensions)
    }

    @Test fun `picking Custom again keeps the typed size`() {
        val typed = CanvasSizeSelection(preset = CanvasPresets.Custom, customDimensions = StageDimensions(800, 600))
        assertEquals(StageDimensions(800, 600), typed.choose(CanvasPresets.Custom).dimensions)
    }

    @Test fun `a named size is its own size`() {
        val youtube = CanvasSizeSelection().choose(CanvasPresets.YouTube)
        assertEquals(CanvasPresets.YouTube.dimensions, youtube.dimensions)
    }
}
