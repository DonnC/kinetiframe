// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModelStore
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.geometry.Affine2D
import com.donnclab.kinetiframe.core.geometry.Rect2
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.LayerState
import com.donnclab.kinetiframe.studio.SelectedContent
import com.donnclab.kinetiframe.studio.SelectionState
import com.donnclab.kinetiframe.studio.StudioDirectories
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** R3-22: Paste puts the drawing where it was copied from, so it carries to the next frame unmoved. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectionPasteTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun pasteLandsWhereTheSelectionWasCopiedFrom() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val main = Layer(name = "Main", id = "main")
            val moved = Affine2D(tx = 5f, ty = 7f)
            val pixels = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            val state = StudioState(
                project = ProjectDocument(id = "paste", stage = StageDimensions(16, 16), layers = listOf(main)),
                animation = AnimationState(totalFrames = 1),
                layers = LayerState(layers = listOf(main), frameLayers = mapOf(0 to listOf(main))),
                selection = SelectionState(
                    active = true,
                    bounds = Rect2(0f, 0f, 4f, 4f),
                    selectedContent = SelectedContent(
                        bitmap = pixels,
                        bounds = Rect2(0f, 0f, 4f, 4f),
                        isRasterSelection = true,
                    ),
                    transform = moved,
                ),
            )
            val editor = StudioViewModel(
                StudioDirectories(temporary.newFolder()),
                engineDispatcher = dispatcher,
                initialEditorState = state,
            )
            store.put("editor", editor)
            // Not prepareProjectSave: saving commits the open selection this test copies and inspects.
            testScheduler.advanceUntilIdle()

            editor.handleIntent(StudioIntent.Selection.Copy)
            editor.handleIntent(StudioIntent.Selection.Paste)
            // Committing the old selection hops to real IO threads, so step the scheduler until done.
            var steps = 0
            while (editor.state.value.selection.selectedContent?.isPaste != true && steps++ < PASTE_WAIT_STEPS) {
                testScheduler.advanceUntilIdle()
                Thread.sleep(PASTE_WAIT_MS)
            }

            val pasted = editor.state.value.selection
            assertTrue(pasted.selectedContent!!.isPaste)
            assertEquals(moved.tx, pasted.transform.tx, 0.01f)
            assertEquals(moved.ty, pasted.transform.ty, 0.01f)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private companion object {
        const val PASTE_WAIT_STEPS = 200
        const val PASTE_WAIT_MS = 10L
    }
}
