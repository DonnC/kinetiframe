package com.donnclab.kinetiframe.presentation.studio.actorstudio.sketch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.down
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchBindingMenu
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchCanvas
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchControls
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchIntent
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchState
import com.donnclab.kinetiframe.presentation.actors.sketch.SketchTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActorSketchBindingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun namedPartMenuArmsTheLassoWithoutPainting() {
        val intents = mutableListOf<ActorSketchIntent>()
        val graph = SceneGraph(
            listOf("head"),
            mapOf(
                "head" to BoneNode("head", name = "Head", length = 32f),
                "eyes" to BoneNode("eyes", "head", "Eyes", 1f),
            ),
        )
        compose.setContent {
            MaterialTheme { ActorSketchBindingMenu(ActorSketchState(busy = false), graph, intents::add) }
        }
        compose.onNodeWithContentDescription("Correct drawing binding").performClick()
        compose.onNodeWithText("Eyes").performClick()
        compose.runOnIdle {
            assertEquals(
                listOf(ActorSketchIntent.BindingPart("eyes"), ActorSketchIntent.Tool(SketchTool.BIND_REGION)),
                intents,
            )
        }
    }

    @Test fun applyRemainsVisibleOnCompactCanvas() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    ActorSketchControls(ActorSketchState(busy = false), {}, true, {}, {})
                }
            }
        }
        compose.onNodeWithContentDescription("Make drawing movable").assertIsDisplayed()
    }

    @Test fun sharedDockSelectsJointModeWithoutDrawing() {
        val intents = mutableListOf<ActorSketchIntent>()
        compose.setContent { MaterialTheme { ActorSketchToolDock(ActorSketchState(busy = false), intents::add) } }
        compose.onNodeWithContentDescription("Fit drawing joints").performClick()
        compose.runOnIdle { assertEquals(listOf(ActorSketchIntent.Tool(SketchTool.JOINT)), intents) }
    }

    @Test fun draggingAnEndpointEmitsJointFitNotInk() {
        val intents = mutableListOf<ActorSketchIntent>()
        val pixels = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
        val graph = SceneGraph(
            listOf("body"),
            mapOf(
                "body" to BoneNode(
                    hierarchy = BoneHierarchy("body", "Body", null),
                    rest = BoneRest(16f, localTransform = Transform(8f, 8f)),
                ),
            ),
        )
        compose.setContent {
            MaterialTheme {
                ActorSketchCanvas(
                    ActorSketchState(ink = pixels, colour = pixels, busy = false, tool = SketchTool.JOINT),
                    graph,
                    floatArrayOf(0f, 0f, 32f, 32f),
                    true,
                    intents::add,
                    Modifier.size(320.dp).testTag("sketch-paper"),
                )
            }
        }
        compose.onNodeWithTag("sketch-paper").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(width * .735f, height * .265f))
            moveTo(androidx.compose.ui.geometry.Offset(width * .6175f, height * .6175f))
            up()
        }
        compose.runOnIdle {
            val fit = intents.filterIsInstance<ActorSketchIntent.MoveJoint>().single()
            assertTrue(intents.none { it is ActorSketchIntent.Stroke })
            assertEquals("body", fit.partId)
            assertFalse(fit.start)
            assertEquals(20f, fit.target.x, .2f)
            assertEquals(20f, fit.target.y, .2f)
        }
    }
}
