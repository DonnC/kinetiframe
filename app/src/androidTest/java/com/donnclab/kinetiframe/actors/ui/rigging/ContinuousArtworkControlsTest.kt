package com.donnclab.kinetiframe.actors.ui.rigging

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.donnclab.kinetiframe.presentation.actors.rigging.ArtworkAlignmentDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContinuousArtworkControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun continuousChoiceEmitsConnectedChildWithoutChangingRigidDefault() {
        val bitmap = Bitmap.createBitmap(100, 20, Bitmap.Config.ARGB_8888)
        var chosen: String? = null
        try {
            compose.setContent {
                MaterialTheme {
                    ArtworkAlignmentDialog(
                        "Arm",
                        bitmap,
                        50f,
                        0f,
                        {},
                        { _, _, _, child -> chosen = child },
                        listOf("lower" to "Forearm"),
                    )
                }
            }
            compose.onNodeWithText("Continuous limb").performScrollTo()
            compose.onAllNodes(isToggleable())[1].performClick()
            compose.onNodeWithContentDescription("Confirm artwork attachment").performClick()
            compose.runOnIdle { assertEquals("lower", chosen) }
        } finally {
            bitmap.recycle()
        }
    }
}
