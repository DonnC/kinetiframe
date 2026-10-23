package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.studio.StudioIntent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Device tests for the compact part-action row shown when an Actor Studio bone is selected.
 * Covers setup-versus-performance gating (cut and detach only apply while rigging) and the
 * rule that detach is offered only for bound artwork.
 */
class PartActionsRowTest {
    @get:Rule val compose = createComposeRule()

    private fun unboundArm() = BoneNode("arm", name = "Arm", length = 40f)
    private fun boundArm() = BoneNode(
        hierarchy = BoneHierarchy("arm", "Arm", null),
        rest = BoneRest(40f),
        artwork = BoneArtwork(textureBinding = TextureBinding("tex-arm", 0f, 0f)),
    )

    @Test fun moveArtworkAndAttachmentHaveDifferentIntents() {
        var intent: StudioIntent? = null
        compose.setContent { PartActionsRow(boundArm(), setup = true, onIntent = { intent = it }) }
        compose.onNodeWithContentDescription("Move part artwork").performClick()
        compose.runOnIdle {
            assertEquals(
                StudioIntent.Actor.SetEditTarget(com.donnclab.kinetiframe.studio.ActorEditTarget.ARTWORK),
                intent,
            )
        }
        compose.onNodeWithContentDescription("Move attachment joint").performClick()
        compose.runOnIdle {
            assertEquals(
                StudioIntent.Actor.SetEditTarget(com.donnclab.kinetiframe.studio.ActorEditTarget.JOINT_START),
                intent,
            )
        }
    }

    @Test fun frozenPerformanceCannotRepositionRestArtworkOrJoint() {
        compose.setContent { PartActionsRow(boundArm(), setup = false, onIntent = {}) }
        compose.onNodeWithContentDescription("Move part artwork").assertDoesNotExist()
        compose.onNodeWithContentDescription("Move attachment joint").assertIsNotEnabled()
    }

    @Test fun attachArtworkSelectsLassoCutWhileInSetup() {
        var intent: StudioIntent? = null
        compose.setContent {
            PartActionsRow(unboundArm(), setup = true, onIntent = { intent = it }, canCaptureSketch = true)
        }
        compose.onNodeWithContentDescription("Capture sketch artwork").performClick()
        compose.runOnIdle {
            assertEquals(StudioIntent.Draw.SelectTool(DrawingTool.LASSO_CUT), intent)
        }
    }

    @Test fun attachArtworkIsDisabledOutsideSetup() {
        compose.setContent { PartActionsRow(unboundArm(), setup = false, onIntent = {}) }
        compose.onNodeWithContentDescription("Capture sketch artwork").assertDoesNotExist()
    }

    @Test fun detachArtworkRequiresSetupAndAnExistingBinding() {
        // Unbound part: do not offer a meaningless detach operation.
        compose.setContent { PartActionsRow(unboundArm(), setup = true, onIntent = {}) }
        compose.onNodeWithContentDescription("Detach artwork").assertDoesNotExist()
    }

    @Test fun detachArtworkDispatchesUnbindWhenBoundAndInSetup() {
        var intent: StudioIntent? = null
        compose.setContent { PartActionsRow(boundArm(), setup = true, onIntent = { intent = it }) }
        compose.onNodeWithContentDescription("Detach artwork").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(StudioIntent.Actor.Unbind, intent) }
    }
}
