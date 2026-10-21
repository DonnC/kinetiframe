package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneDisplay
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.ui.components.rigging.ActorPartTree
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Device tests for the expandable actor part tree: selection callbacks, expanding and
 * collapsing a parent part, and the bound-versus-unbound artwork indicator.
 */
class ActorPartTreeTest {
    @get:Rule val compose = createComposeRule()

    private fun humanoidGraph() = SceneGraph(
        rootBoneIds = listOf("hips"),
        bones = mapOf(
            "hips" to BoneNode("hips", name = "Hips", length = 30f),
            "torso" to BoneNode(
                hierarchy = BoneHierarchy("torso", "Torso", "hips"),
                rest = BoneRest(40f),
                artwork = BoneArtwork(textureBinding = TextureBinding("tex-torso", 0f, 0f)),
            ),
            "arm" to BoneNode("arm", parentId = "hips", name = "Arm", length = 22f),
        ),
    )

    @Test fun selectingAPartReportsItsStableId() {
        var selected: String? = null
        compose.setContent {
            ActorPartTree(graph = humanoidGraph(), selectedPartId = null, onPartSelected = { selected = it })
        }
        compose.onNodeWithText("Torso").performClick()
        compose.runOnIdle { assertEquals("torso", selected) }
    }

    @Test fun boundAndUnboundPartsShowDistinctArtworkStatus() {
        compose.setContent {
            ActorPartTree(graph = humanoidGraph(), selectedPartId = null, onPartSelected = {})
        }
        compose.onNodeWithContentDescription("Artwork bound").assertExists()
        compose.onNodeWithContentDescription("Artwork not bound").assertExists()
    }

    @Test fun collapsingAParentHidesItsChildrenWithoutChangingSelection() {
        var selected: String? = "arm"
        compose.setContent {
            ActorPartTree(graph = humanoidGraph(), selectedPartId = selected, onPartSelected = { selected = it })
        }
        compose.onNodeWithText("Arm").assertExists()
        compose.onNodeWithContentDescription("Collapse Hips").performClick()
        compose.onNodeWithText("Arm").assertDoesNotExist()
        // Collapsing is presentation-only: the previously selected part's ID is untouched.
        compose.runOnIdle { assertEquals("arm", selected) }
    }

    @Test fun visibilityToggleReportsThePartIdAndTheOppositeOfItsCurrentState() {
        var toggled: Pair<String, Boolean>? = null
        compose.setContent {
            ActorPartTree(
                graph = humanoidGraph(),
                selectedPartId = null,
                onPartSelected = {},
                onVisibilityToggle = { id, visible -> toggled = id to visible },
            )
        }
        // Torso starts visible (default), so its eye click should request hiding it.
        compose.onNodeWithContentDescription("Hide Torso").performClick()
        compose.runOnIdle { assertEquals("torso" to false, toggled) }
    }

    @Test fun aHiddenPartShowsTheOppositeEyeAffordance() {
        val graph = SceneGraph(
            rootBoneIds = listOf("hidden"),
            bones = mapOf(
                "hidden" to BoneNode(
                    hierarchy = BoneHierarchy("hidden", "Hidden", null),
                    rest = BoneRest(10f),
                    display = BoneDisplay(visible = false),
                ),
            ),
        )
        compose.setContent { ActorPartTree(graph = graph, selectedPartId = null, onPartSelected = {}) }
        compose.onNodeWithContentDescription("Show Hidden").assertExists()
    }
}
