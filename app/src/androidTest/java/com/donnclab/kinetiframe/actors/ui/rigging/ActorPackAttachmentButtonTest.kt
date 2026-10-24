package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ui.EditorTopBar
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.PuppetTopBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActorPackAttachmentButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test fun universalAttachmentOpensDirectlyWithoutHumanSpecificMenu() {
        var variant: Boolean? = null
        compose.setContent { ActorPackAttachmentButton({ variant = it }) }
        compose.onNodeWithContentDescription("Attach artwork").performClick()
        compose.onNodeWithText("Add expression").assertDoesNotExist()
        compose.runOnIdle { assertEquals(false, variant) }
    }

    @Test fun setupAppBarExposesAttachment() {
        var variant: Boolean? = null
        compose.setContent { PuppetTopBar({}, {}, onAttachArtwork = { variant = it }) }
        compose.onNodeWithContentDescription("Attach artwork").performClick()
        compose.runOnIdle { assertEquals(false, variant) }
    }

    @Test fun drawingAppBarExposesAttachment() {
        var variant: Boolean? = null
        compose.setContent {
            EditorTopBar(
                StudioState(scene = SceneState(selectedBoneId = "eyes")), {}, {}, {}, {}, {},
                isActorStudio = true, showOverflowMenu = false, onOverflowMenuToggle = {}, onAttachActorArtwork = {
                    variant =
                        it
                },
            )
        }
        compose.onNodeWithContentDescription("Attach artwork").performClick()
        compose.runOnIdle { assertEquals(false, variant) }
    }

    @Test fun performanceCannotAttachNewRigArtwork() {
        compose.setContent { PuppetTopBar({}, {}, performanceOnly = true, onAttachArtwork = {}) }
        compose.onNodeWithContentDescription("Attach artwork").assertDoesNotExist()
    }
}
