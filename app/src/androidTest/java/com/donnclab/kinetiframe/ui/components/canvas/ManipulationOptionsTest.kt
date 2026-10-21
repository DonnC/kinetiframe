package com.donnclab.kinetiframe.ui.components.canvas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.components.dock.SmudgeBlurOptionsContent
import org.junit.Rule
import org.junit.Test

class ManipulationOptionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun smudgeHasSizeButNoStrengthControl() {
        compose.setContent { KinetiFrameTheme { SmudgeBlurOptionsContent(TimelineState(), DrawingTool.SMUDGE, {}) } }
        compose.onNodeWithText("Size").assertExists()
        compose.onNodeWithText("Strength").assertDoesNotExist()
    }
}
