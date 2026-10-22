package com.donnclab.kinetiframe.studio.ui.canvas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ui.dock.SmudgeBlurOptionsContent
import org.junit.Rule
import org.junit.Test

class ManipulationOptionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun smudgeHasSizeButNoStrengthControl() {
        compose.setContent { KinetiFrameTheme { SmudgeBlurOptionsContent(StudioState(), DrawingTool.SMUDGE, {}) } }
        compose.onNodeWithText("Size").assertExists()
        compose.onNodeWithText("Strength").assertDoesNotExist()
    }
}
