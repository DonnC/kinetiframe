package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.ui.TimelineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FrameImagePlacementTest {
    @Test fun imageIsATransformableUndoableStampNotABackground() = runBlocking {
        var state = TimelineState()
        val history = mutableListOf<HistoryItem>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val manager = SelectionManager(
            scope, Dispatchers.Unconfined, Mutex(), { state },
            { transform -> state = transform(state) }, {}, { null }, {}, history,
        )
        try {
            val image = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
            manager.pasteBitmap(image, 100, 100)
            assertEquals(DrawingTool.LASSO, state.tool.activeTool)
            assertTrue(state.selection.active)
            assertSame(image, state.selection.selectedContent?.bitmap)
            assertEquals(0.5f, state.selection.transform.values[0], 0f)
            assertNull(state.project.backgroundAssetId)
            manager.commitSelection().join()
            assertFalse(state.selection.active)
            assertEquals(1, history.size)
            assertTrue(history.single() is HistoryItem.RasterSelection)
            manager.pasteBitmap(image, 100, 100)
            manager.cancelSelection().join()
            assertEquals(1, history.size)
        } finally {
            scope.cancel()
        }
    }
}
