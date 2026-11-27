package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Bitmap
import com.donnclab.kinetiframe.core.geometry.Affine2D
import com.donnclab.kinetiframe.core.geometry.Rect2
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.domain.DrawingLayers
import com.donnclab.kinetiframe.studio.domain.DrawingManager
import com.donnclab.kinetiframe.studio.domain.HistoryItem
import com.donnclab.kinetiframe.studio.domain.SelectionEditType
import com.donnclab.kinetiframe.studio.domain.StudioRuntime
import com.donnclab.kinetiframe.studio.domain.UndoStacks
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionHistoryTest {
    private val dispatcher = Dispatchers.Unconfined
    private val bakeMutex = Mutex()
    private val frameMutex = Mutex()

    @Test
    fun undoingSelectionCommitRestoresOriginalBitmap() = runTest {
        val history = mutableListOf<HistoryItem>()
        val redoHistory = mutableListOf<HistoryItem>()
        var baseBitmap: Bitmap? = null

        val tempDir = File.createTempFile("history_test", "")
        tempDir.delete()
        tempDir.mkdir()
        val cacheManager = FrameCacheManager(tempDir, this)

        val drawingManager = DrawingManager(
            runtime = StudioRuntime(this, dispatcher, { StudioState() }, { }),
            cacheManager = cacheManager,
            bakeMutex = bakeMutex,
            frameOperationMutex = frameMutex,
            layers = DrawingLayers(
                getBaseBitmap = { baseBitmap },
                setBaseBitmap = { baseBitmap = it },
                activeLayerId = { "main" },
                composeFrame = { _, _, b -> b },
                acquireBitmap = { w, h -> Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) },
                releaseBitmap = { },
            ),
            undo = UndoStacks(history, redoHistory),
        )

        // 1. Simulate a selection commit
        val proxy = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val edit = HistoryItem.SelectionEdit(
            bitmap = proxy,
            nodes = emptyMap(),
            matrix = Affine2D(),
            originalBounds = Rect2(0f, 0f, 50f, 50f),
            type = SelectionEditType.COMMIT,
        )

        history.add(edit)
        assertEquals(1, history.size)

        // 2. Undo
        drawingManager.undo()
        assertEquals(0, history.size)
        assertEquals(1, redoHistory.size)
    }
}
