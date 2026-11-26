// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimelineScaleTest {
    @Test fun `the phone strip keeps its 40 dp cell`() {
        assertEquals(40.dp, TimelineScale.Phone.cell)
    }

    @Test fun `a square canvas gives square tablet frames`() {
        val scale = TimelineScale.wide(StageDimensions.Square)
        assertEquals(scale.thumbHeight, scale.thumbWidth)
        assertEquals(scale.thumbWidth + TimelineTokens.frameGap, scale.cell)
    }

    @Test fun `tablet frames follow the canvas shape within their bounds`() {
        val landscape = TimelineScale.wide(StageDimensions.LandscapeHd)
        val tall = TimelineScale.wide(StageDimensions(1080, 1920))
        assertEquals(TimelineTokens.wideThumbMaxWidth, landscape.thumbWidth)
        assertEquals(TimelineTokens.wideThumbMinWidth, tall.thumbWidth)
        assertTrue(landscape.cell > TimelineScale.Phone.cell * 2)
    }
}
