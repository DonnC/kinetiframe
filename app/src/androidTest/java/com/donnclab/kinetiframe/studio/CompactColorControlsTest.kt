package com.donnclab.kinetiframe.studio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.library.catalog.PackContentsData
import com.donnclab.kinetiframe.presentation.library.catalog.PackContentsGrid
import com.donnclab.kinetiframe.presentation.studio.EditorTopBar
import com.donnclab.kinetiframe.presentation.studio.color.ScreenColorIntent
import com.donnclab.kinetiframe.presentation.studio.color.ScreenColorOverlay
import com.donnclab.kinetiframe.presentation.studio.color.ScreenColorState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CompactColorControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactFieldAcceptsInputWithoutMaterialMinimumHeight() {
        compose.setContent {
            KinetiFrameTheme {
                var text by remember { mutableStateOf("") }
                CompactTextField(text, { text = it }, Modifier.testTag("field"), singleLine = true)
            }
        }
        compose.onNodeWithTag("field").assertHeightIsEqualTo(36.dp)
        compose.onNode(hasSetTextAction()).performTextInput("Tavi")
        compose.onNodeWithText("Tavi").assertExists()
    }

    @Test fun disabledFieldCannotEdit() {
        compose.setContent { KinetiFrameTheme { CompactTextField("Saved", {}, enabled = false) } }
        compose.onNode(hasSetTextAction()).assertIsNotEnabled()
    }

    @Test fun eyedropperRequiresSampleAndCancelRemainsAvailable() {
        var intent: ScreenColorIntent? = null
        compose.setContent {
            KinetiFrameTheme {
                ScreenColorOverlay(ScreenColorState(active = true), Offset.Zero, { intent = it })
            }
        }
        compose.onNodeWithContentDescription("Use sampled color").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Cancel eyedropper").performClick()
        assertEquals(ScreenColorIntent.Cancel, intent)
    }

    @Test fun sampledColorCanBeConfirmed() {
        var intent: ScreenColorIntent? = null
        compose.setContent {
            KinetiFrameTheme {
                ScreenColorOverlay(ScreenColorState(active = true, color = 0xFF123456.toInt()), Offset.Zero, {
                    intent = it
                })
            }
        }
        compose.onNodeWithContentDescription("Use sampled color").assertIsEnabled().performClick()
        assertEquals(ScreenColorIntent.Confirm, intent)
    }

    @Test fun packInspectorOffersEdit() {
        var edited = false
        compose.setContent {
            KinetiFrameTheme {
                PackContentsGrid(PackContentsData("Faces", emptyList()), {}, {}, showAddAction = false, onEdit = {
                    edited =
                        true
                })
            }
        }
        compose.onNodeWithContentDescription("Edit pack").performClick()
        assertEquals(true, edited)
    }

    @Test fun packInspectorBlocksEditDuringRead() {
        compose.setContent {
            KinetiFrameTheme {
                PackContentsGrid(
                    PackContentsData("Faces", emptyList(), loading = true),
                    {},
                    {},
                    showAddAction = false,
                    onEdit = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Edit pack").assertIsNotEnabled()
    }

    @Test fun topBarUsesHomeAndPackSaveInsteadOfDuplicateLayersOrProjectSave() {
        var home = false
        var saved = ""
        compose.setContent {
            KinetiFrameTheme {
                EditorTopBar(
                    StudioState(), {}, { home = true }, {}, {}, {},
                    showOverflowMenu = false, onOverflowMenuToggle = {},
                    onSaveProjectClick = { saved = "project" }, onSavePackClick = { saved = "pack" },
                )
            }
        }
        compose.onNodeWithContentDescription("Home").performClick()
        assertEquals(true, home)
        compose.onNodeWithContentDescription("Layers").assertDoesNotExist()
        compose.onNodeWithContentDescription("Save").performClick()
        assertEquals("pack", saved)
    }

    @Test fun studioOverflowDoesNotExposeDiagnostics() {
        compose.setContent {
            KinetiFrameTheme {
                EditorTopBar(
                    StudioState(),
                    {},
                    {},
                    {},
                    {},
                    {},
                    showOverflowMenu = true,
                    onOverflowMenuToggle = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Settings").assertDoesNotExist()
        compose.onNodeWithText("Share diagnostics").assertDoesNotExist()
        compose.onNodeWithContentDescription("Share diagnostics").assertDoesNotExist()
    }
}
