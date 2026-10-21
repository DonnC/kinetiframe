package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkVariant
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.components.rigging.ActorArtworkMenu
import com.donnclab.kinetiframe.ui.components.rigging.PartActionsRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActorArtworkControlsTest {
    @Test fun attachmentBlendUsesGenericPartIntent() {
        var intent: TimelineIntent? = null
        val part = BoneNode(
            hierarchy = BoneHierarchy("gear", "Gear", "base"),
            rest = BoneRest(30f),
            artwork = BoneArtwork(textureBinding = TextureBinding("art", 0f, 0f)),
        )
        compose.setContent {
            com.donnclab.kinetiframe.ui.components.rigging.ActorArtworkControls(part, { intent = it }, {}, {})
        }
        compose.onNodeWithContentDescription("Edit part artwork").performClick()
        compose.onNodeWithText("Blend attachment").performClick()
        compose.runOnIdle { assertEquals(TimelineIntent.Actor.SetAttachmentBlend("gear", "", true), intent) }
    }

    @get:Rule val compose = createComposeRule()

    @Test fun expressionMenuKeysTheChosenVariant() {
        var intent: TimelineIntent? = null
        val part = BoneNode(
            hierarchy = BoneHierarchy("mouth", "Mouth", null),
            rest = BoneRest(10f),
            artwork = BoneArtwork(
                artworkVariants = mapOf("smile" to ArtworkVariant("Smile", TextureBinding("art", 0f, 0f))),
            ),
        )
        compose.setContent { ActorArtworkMenu(part) { intent = it } }
        compose.onNodeWithContentDescription("Part artwork").performClick()
        compose.onNodeWithText("Smile").performClick()
        compose.runOnIdle { assertEquals(TimelineIntent.Actor.SelectArtwork("mouth", "smile"), intent) }
    }

    @Test fun artworkCannotChangeWhileAuthoringPinnedPerformance() {
        compose.setContent {
            PartActionsRow(
                BoneNode("head", name = "Head", length = 10f),
                setup = false,
                onIntent = {},
                onChooseArtwork = {},
            )
        }
        compose.onNodeWithContentDescription("Attach library artwork").assertIsNotEnabled()
    }

    @Test fun genericArtworkControlsExposeAlignmentAndUndoableRemoval() {
        var intent: TimelineIntent? = null
        var editing: String? = null
        val part = BoneNode(
            hierarchy = BoneHierarchy("gear", "Gear", null),
            rest = BoneRest(10f),
            artwork = BoneArtwork(textureBinding = TextureBinding("art", 0f, 0f)),
        )
        compose.setContent {
            com.donnclab.kinetiframe.ui.components.rigging.ActorArtworkControls(
                part,
                { intent = it },
                {},
                { editing = it },
            )
        }
        compose.onNodeWithContentDescription("Edit part artwork").performClick()
        compose.onNodeWithText("Resize / align artwork").performClick()
        compose.runOnIdle { assertEquals("", editing) }
        compose.onNodeWithContentDescription("Edit part artwork").performClick()
        compose.onNodeWithText("Remove artwork").performClick()
        compose.runOnIdle { assertEquals(TimelineIntent.Actor.RemoveArtwork("gear"), intent) }
    }
}
