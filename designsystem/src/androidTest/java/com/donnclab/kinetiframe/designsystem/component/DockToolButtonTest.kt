package com.donnclab.kinetiframe.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pen
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DockToolButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unselectedToolSelectsWithoutOpeningMenu() {
        var selected = 0
        var menus = 0
        compose.setContent {
            KinetiFrameTheme { DockToolButton(DockToolAction(Lucide.Pen, LABEL, { selected++ }), false, { menus++ }) }
        }
        compose.onNodeWithContentDescription(LABEL).performClick()
        compose.runOnIdle {
            assertEquals(1, selected)
            assertEquals(0, menus)
        }
    }

    @Test fun selectedToolOpensMenuWithoutSelectingAgain() {
        var selected = 0
        var menus = 0
        compose.setContent {
            KinetiFrameTheme { DockToolButton(DockToolAction(Lucide.Pen, LABEL, { selected++ }), true, { menus++ }) }
        }
        compose.onNodeWithContentDescription(LABEL).performClick()
        compose.runOnIdle {
            assertEquals(0, selected)
            assertEquals(1, menus)
        }
    }

    @Test fun longPressOpensMenuWithoutSelecting() {
        var selected = 0
        var menus = 0
        compose.setContent {
            KinetiFrameTheme { DockToolButton(DockToolAction(Lucide.Pen, LABEL, { selected++ }), false, { menus++ }) }
        }
        compose.onNodeWithContentDescription(LABEL).performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(0, selected)
            assertEquals(1, menus)
        }
    }

    private companion object {
        const val LABEL = "Pen"
    }
}
