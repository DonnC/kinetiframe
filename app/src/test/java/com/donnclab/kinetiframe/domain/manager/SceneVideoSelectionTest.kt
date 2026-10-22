package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import com.donnclab.kinetiframe.engine.model.ProjectAsset
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.model.VideoClipPlacement
import com.donnclab.kinetiframe.ui.StudioState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalCoroutinesApi::class)
class SceneVideoSelectionTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun lassoMovesVideoMetadataWithoutPaintingOrDeletingItsSource() = runBlocking {
        val clip =
            VideoClipPlacement(
                "v",
                "a",
                "TV",
                0,
                12,
                isSceneContent = true,
                x = 10f,
                y = 10f,
                width = 100f,
                height = 50f,
                linkedAudioClipId = "sound",
            )
        val sound = AudioClipPlacement("sound", "a", 0, "TV sound", 0, 12)
        var state =
            StudioState(
                project = ProjectDocument(
                    stage = StageDimensions(120, 80),
                    videoClips = mapOf("v" to clip),
                    audioClips = mapOf("sound" to sound),
                    assets = mapOf("a" to ProjectAsset("a", AssetKind.VIDEO, "/screen.mp4", 100, 50)),
                ),
            )
        val history = mutableListOf<HistoryItem>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val manager = SelectionManager(
            scope, Dispatchers.Unconfined, Mutex(), { state }, { state = it(state) },
            {}, { throw AssertionError("Video selection must not clear raster pixels") }, {}, history,
        )
        try {
            manager.selectArea(null, Rect(0f, 0f, 120f, 80f), 1f).join()
            assertEquals(clip, state.selection.selectedContent?.videos?.get("v"))
            assertNull(state.selection.selectedContent?.bitmap)
            val transform = Matrix().apply {
                translate(40f, 30f)
                scale(2f, 2f)
            }
            state = state.copy(selection = state.selection.copy(transform = transform))
            manager.commitSelection().join()
            val moved = state.project.videoClips.getValue("v")
            assertEquals(40f, moved.selectionTransform.tx, 0f)
            assertEquals(2f, moved.selectionTransform.a, 0f)
            assertEquals(12, moved.timelineEndFrame)
            val edit = history.single() as HistoryItem.ObjectEdit
            assertEquals(clip, edit.beforeVideos["v"])
            assertEquals(moved, edit.afterVideos["v"])
            manager.selectVideo("v").join()
            manager.cancelSelection().join()
            assertEquals(moved, state.project.videoClips["v"])
            manager.selectVideo("v").join()
            manager.deleteSelection().join()
            assertTrue(state.project.videoClips.isEmpty())
            assertTrue(state.project.audioClips.isEmpty())
            assertEquals(moved, (history.last() as HistoryItem.ObjectEdit).beforeVideos["v"])
            assertTrue("a" in state.project.assets)
            val pending = PendingOperations()
            val drawing = DrawingManager(
                scope, Dispatchers.Unconfined,
                com.donnclab.kinetiframe.engine.cache.FrameCacheManager(temporary.newFolder(), scope), Mutex(), Mutex(),
                { state }, { state = it(state) }, { null }, {}, { "main" }, { _, _, bitmap -> bitmap }, history,
                mutableListOf(), { width, height -> Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) }, {},
                pendingOperations = pending,
            )
            drawing.undo()
            pending.awaitIdle()
            assertEquals(moved, state.project.videoClips["v"])
            assertEquals(sound, state.project.audioClips["sound"])
            drawing.undo()
            pending.awaitIdle()
            assertEquals(clip, state.project.videoClips["v"])
            drawing.redo()
            pending.awaitIdle()
            assertEquals(moved, state.project.videoClips["v"])
            manager.selectVideo("v").join()
            manager.copySelection()
            manager.cancelSelection().join()
            manager.pasteSelection()
            pending.awaitIdle()
            // The selection manager owns a separate pending queue; its unconfined dispatch completes immediately.
            manager.commitSelection().join()
            val copy = state.project.videoClips.values.single { it.clipId != "v" }
            val copiedSound = state.project.audioClips.getValue(requireNotNull(copy.linkedAudioClipId))
            assertNotEquals("sound", copiedSound.clipId)
            assertEquals(copy.timelineStartFrame, copiedSound.startFrame)
        } finally {
            scope.cancel()
            Dispatchers.resetMain()
        }
    }
}
