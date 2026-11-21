package com.donnclab.kinetiframe.studio.render

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
class HeldDrawingCompositorTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun sparseFrameMetadataStillExportsHeldPixelsAndUnlinkedFramesStayEmpty() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val main = Layer(id = "main", name = "Drawing")
        val project = ProjectDocument(stage = StageDimensions(16, 16), layers = listOf(main))
        val compositor = ProjectFrameCompositor(project, mapOf(0 to listOf(main)), cache, null, null)
        try {
            cache.putFrame(0, Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) })
            cache.holdDrawing("main", 0, 2, 4)
            for (frame in 0..2) assertEquals(Color.RED, compositor.composite(frame).getPixel(8, 8))
            assertEquals(Color.TRANSPARENT, compositor.composite(3).getPixel(8, 8))
        } finally {
            compositor.release()
            scope.cancel()
        }
    }
}
