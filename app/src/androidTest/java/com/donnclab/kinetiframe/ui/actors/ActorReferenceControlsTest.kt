package com.donnclab.kinetiframe.ui.actors

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.center
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.width
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActorReferenceControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun visibleReferenceCannotEnterSavedPartArtwork() {
        val directory = java.nio.file.Files.createTempDirectory("actor-reference-test").toFile()
        val reference = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.MAGENTA)
        }
        val ink = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLACK)
        }
        try {
            val session = ActorReferenceSession().apply { replace(reference, 200, 200) }
            val runtime = com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager()
            val id = runtime.registerAsset(ink)
            val graph = com.donnclab.kinetiframe.engine.rigging.model.SceneGraph(
                listOf("part"),
                mapOf(
                    "part" to
                        com.donnclab.kinetiframe.engine.rigging.model.BoneNode(
                            hierarchy = BoneHierarchy("part", "Part", null),
                            rest = BoneRest(10f),
                            artwork = BoneArtwork(
                                textureBinding = com.donnclab.kinetiframe.engine.rigging.model.TextureBinding(
                                    id,
                                    0f,
                                    0f,
                                ),
                            ),
                        ),
                ),
            )
            val files = com.donnclab.kinetiframe.data.actor.ActorFileStore(directory)
            val paths = com.donnclab.kinetiframe.data.actor.ActorArtworkStore(
                files,
            ).save("actor", "revision", graph, runtime)
            assertEquals(setOf("part"), paths.keys)
            val saved = android.graphics.BitmapFactory.decodeFile(
                java.io.File(files.actorDirectory("actor"), paths.getValue("part")).path,
            )
            assertEquals(10, saved.width)
            assertEquals(android.graphics.Color.BLACK, saved.getPixel(0, 0))
            assertEquals(1, directory.walkTopDown().count { it.isFile })
            assertTrue(session.visible)
            saved.recycle()
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun referenceMenuPositionsHidesAndRemovesOnlyTheSessionResource() {
        val session = ActorReferenceSession()
        val image = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        session.replace(image, 200, 200)
        compose.setContent { MaterialTheme { ActorReferenceControls(session, 200, 200) } }
        compose.runOnIdle {
            assertEquals(Offset(100f, 100f), session.transform.matrix.map(session.bounds.center))
            assertTrue(session.editing)
        }
        compose.onNodeWithContentDescription("Reference image layer (temporary)").performClick()
        compose.onNodeWithText("Reference · not saved or exported").assertDoesNotExist()
        compose.onNodeWithContentDescription("Position reference").performClick()
        compose.runOnIdle { assertFalse(session.editing) }
        compose.onNodeWithContentDescription("Reference image layer (temporary)").performClick()
        compose.onNodeWithContentDescription("Reference visibility").performClick()
        compose.runOnIdle { assertFalse(session.visible) }
        compose.onNodeWithContentDescription("Remove reference").performClick()
        compose.runOnIdle {
            assertNull(session.bitmap)
            assertFalse(image.isRecycled)
        }
    }

    @Test fun replacementResetsTransformWithoutRecyclingDisplayedImage() {
        val session = ActorReferenceSession()
        val first = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val second = Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)
        compose.setContent { MaterialTheme { ActorReferenceControls(session, 200, 200) } }
        compose.runOnIdle {
            session.replace(first, 200, 200)
            session.transform.matrix = androidx.compose.ui.graphics.Matrix().apply { translate(123f, 456f) }
            session.replace(second, 200, 200)
            assertSame(second, session.bitmap)
            assertEquals(Offset(100f, 100f), session.transform.matrix.map(session.bounds.center))
            assertFalse(first.isRecycled)
        }
    }
}
