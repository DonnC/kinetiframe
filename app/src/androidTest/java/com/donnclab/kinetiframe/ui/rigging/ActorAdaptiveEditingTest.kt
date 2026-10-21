package com.donnclab.kinetiframe.ui.rigging

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.center
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.components.dock.ToolDock
import com.donnclab.kinetiframe.ui.components.rigging.ArtworkAlignmentDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActorAdaptiveEditingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun boneDockOffersSelectionButNotRasterToolsOrDuplicateBindLasso() {
        compose.setContent {
            MaterialTheme {
                ToolDock(state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP)), onIntent = {})
            }
        }
        compose.onNodeWithContentDescription("Select actor part").assertExists()
        compose.onNodeWithContentDescription("Eraser").assertDoesNotExist()
        compose.onNodeWithContentDescription("Construction shapes").assertDoesNotExist()
        compose.onNodeWithContentDescription("Rig Artwork Bind").assertDoesNotExist()
        compose.onNodeWithContentDescription("Fill").assertDoesNotExist()
    }

    @Test fun placementUsesTouchAndNeverRequestsCoordinates() {
        val bitmap = Bitmap.createBitmap(60, 40, Bitmap.Config.ARGB_8888)
        var result: TextureBinding? = null
        try {
            compose.setContent {
                MaterialTheme {
                    ArtworkAlignmentDialog(
                        "Head",
                        bitmap,
                        80f,
                        0f,
                        {},
                        { _, binding, _, _ -> result = binding },
                    )
                }
            }
            compose.onNodeWithText("Offset X").assertDoesNotExist()
            compose.onNodeWithText("Width · px").assertDoesNotExist()
            compose.onNodeWithContentDescription("Artwork placement preview").performTouchInput {
                swipe(center, center + androidx.compose.ui.geometry.Offset(30f, 10f))
            }
            compose.onNodeWithContentDescription("Confirm artwork attachment").performClick()
            compose.runOnIdle {
                assertTrue(result!!.offsetX > 40f)
                assertTrue(result!!.offsetY > 0f)
            }
        } finally {
            bitmap.recycle()
        }
    }
}
