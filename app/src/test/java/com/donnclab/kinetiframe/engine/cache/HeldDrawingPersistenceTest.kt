package com.donnclab.kinetiframe.engine.cache

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class HeldDrawingPersistenceTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun pixels(color: Int) = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
    private suspend fun color(cache: FrameCacheManager, frame: Int) = cache.getFrame("main", frame)!!.getPixel(0, 0)

    @Test fun oneThousandExposuresStoreOneCelAndSurviveRestartWithSmallCache() = runBlocking {
        val directory = temporary.newFolder()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(directory, scope, maxRamBytes = 512)
            cache.putFrame(0, pixels(Color.RED))
            cache.holdDrawing("main", 0, 999, 1000)
            cache.persistAllToDisk(listOf("main"), 1000)
            assertEquals(1, directory.listFiles()!!.count { "_linked_" in it.name })
            val metadata = Json.encodeToString(cache.exposures())
            val reopened = FrameCacheManager(directory, scope, maxRamBytes = 512)
            reopened.restoreExposures(Json.decodeFromString(metadata), 1000)
            for (frame in listOf(0, 500, 999)) assertEquals(Color.RED, color(reopened, frame))
        } finally {
            scope.cancel()
        }
    }

    @Test fun defaultEditsAreLocalButExplicitSharedEditsUpdateEveryExposure() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val directory = temporary.newFolder()
            val cache = FrameCacheManager(directory, scope)
            cache.putFrame(0, pixels(Color.RED))
            cache.holdDrawing("main", 0, 2, 3)
            cache.putFrame(1, pixels(Color.BLUE))
            assertEquals(Color.RED, color(cache, 0))
            assertEquals(Color.BLUE, color(cache, 1))
            cache.editLinked("main", 2, true)
            cache.putFrame(2, pixels(Color.GREEN))
            assertEquals(Color.GREEN, color(cache, 0))
            assertEquals(Color.BLUE, color(cache, 1))
            cache.persistAllToDisk(listOf("main"), 3)
            val restored = FrameCacheManager(directory, scope).apply { restoreExposures(cache.exposures(), 3) }
            assertEquals(Color.GREEN, color(restored, 0))
            assertEquals(Color.BLUE, color(restored, 1))
            assertEquals(Color.GREEN, color(restored, 2))
            restored.putFrame(2, pixels(Color.YELLOW)) // Shared scope is not persisted.
            assertEquals(Color.GREEN, color(restored, 0))
        } finally {
            scope.cancel()
        }
    }

    @Test fun insertDeleteAndIndependentDuplicatePreserveHeldPixels() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(temporary.newFolder(), scope)
            cache.putFrame(0, pixels(Color.RED))
            cache.holdDrawing("main", 0, 2, 3)
            cache.insertFrame("main", 1, 3)
            assertNull(cache.getFrame(1))
            assertEquals(Color.RED, color(cache, 3))
            cache.deleteFrame("main", 0, 4) // Delete the original exposure, not the canonical cel.
            assertEquals(Color.RED, color(cache, 1))
            cache.duplicateFrame("main", 1, 3)
            cache.putFrame(2, pixels(Color.BLUE))
            assertEquals(Color.RED, color(cache, 1))
            assertEquals(Color.BLUE, color(cache, 2))
            assertEquals(Color.RED, color(cache, 3))
            cache.detachDrawing("main", 3)
            cache.editLinked("main", 1, true)
            cache.putFrame(1, pixels(Color.GREEN))
            assertEquals(Color.RED, color(cache, 3))
        } finally {
            scope.cancel()
        }
    }
}
