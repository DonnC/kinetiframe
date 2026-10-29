package com.donnclab.kinetiframe.presentation.studio.canvas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.studio.dock.SmudgeBlurOptionsContent
import com.donnclab.kinetiframe.studio.StudioState
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
