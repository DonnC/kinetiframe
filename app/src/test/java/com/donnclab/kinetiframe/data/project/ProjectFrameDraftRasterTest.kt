package com.donnclab.kinetiframe.data.project

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectFrameDraftRasterTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun flushedRasterDraftDoesNotChangeColdSavedProjectUntilPublished() = runBlocking {
        val saved = temporary.newFolder("saved")
        val red = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        File(saved, "frame_v2_main_frame_0.png").outputStream().use { red.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val draft = File(temporary.root, "draft")
        ProjectFrameDraft.open(saved, draft)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(draft, scope)
            assertEquals(Color.RED, cache.getFrame(0)!!.getPixel(8, 8))
            val blue = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
            cache.putFrame(0, blue)
            cache.persistAllToDisk(listOf("main"), 1)
            assertEquals(Color.RED, FrameCacheManager(saved, scope).getFrame(0)!!.getPixel(8, 8))
            ProjectFrameDraft.publish(draft, saved)
            assertEquals(Color.BLUE, FrameCacheManager(saved, scope).getFrame(0)!!.getPixel(8, 8))
        } finally {
            scope.cancel()
            red.recycle()
        }
    }
}
