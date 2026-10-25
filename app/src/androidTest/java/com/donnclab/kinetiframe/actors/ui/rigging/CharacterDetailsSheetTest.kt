package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.presentation.actors.CharacterDetailsSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CharacterDetailsSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun viewRowsAreTappableAndDurationsAreHumanReadable() {
        var selected: ActorView? = null
        val actor = ActorManifest(
            actorId = "mira",
            displayName = "Mira",
            templateId = ActorManifest.CUSTOM_TEMPLATE_ID,
            currentRevisionId = "front",
            viewRevisions = mapOf(ActorView.FRONT to "front"),
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0,
        )
        val motion = PerformanceDocument(
            performanceId = "wave",
            projectId = "scene",
            actorId = "mira",
            actorRevisionId = "front",
            displayName = "Wave",
            durationMicros = 13_818_089,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0,
        )
        compose.setContent {
            MaterialTheme {
                CharacterDetailsSheet(actor, null, listOf(motion), {
                }, { selected = it }, {}, mapOf("scene" to "Street"))
            }
        }
        compose.onNodeWithText("Front").performClick()
        compose.runOnIdle { assertEquals(ActorView.FRONT, selected) }
        compose.onNodeWithText("Performances · 1").performClick()
        compose.onNodeWithText("Wave").assertExists()
        compose.onNodeWithText("Front · 13.8 s\nStreet").assertExists()
    }
}
