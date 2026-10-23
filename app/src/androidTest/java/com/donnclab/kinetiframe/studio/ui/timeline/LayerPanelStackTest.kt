package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.LayerState
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.Rule
import org.junit.Test

class LayerPanelStackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mainLayerIsVisibleInPanel() {
        val layers = listOf(
            Layer(id = "main", name = "Main", zIndex = 0),
            Layer(id = "ink", name = "Ink", zIndex = 1),
        )
        compose.setContent {
            KinetiFrameTheme {
                LayerPanel(StudioState(layers = LayerState(layers = layers)), {})
            }
        }
        compose.onNodeWithText("Main").assertExists()
        compose.onNodeWithText("Ink").assertExists()
    }
}
