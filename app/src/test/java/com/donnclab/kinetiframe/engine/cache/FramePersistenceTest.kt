package com.donnclab.kinetiframe.engine.cache

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FramePersistenceTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun bitmap(color: Int) = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    @Test fun latestPixelsSurviveRecreatingTheCacheAndCancellingItsOldScope() = runBlocking {
        val directory = temporary.newFolder()
        val oldScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(directory, oldScope)
        repeat(20) { cache.putFrame("main", 0, bitmap(if (it == 19) Color.BLUE else Color.RED)) }
        cache.putFrame("other-frame-layer", 2, bitmap(Color.GREEN))
        cache.persistAllToDisk(listOf("main", "other-frame-layer"), 3)
        oldScope.cancel()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val restored = FrameCacheManager(directory, newScope)
            assertEquals(Color.BLUE, restored.getFrame("main", 0)!!.getPixel(0, 0))
            assertEquals(Color.GREEN, restored.getFrame("other-frame-layer", 2)!!.getPixel(0, 0))
        } finally {
            newScope.cancel()
        }
    }

    @Test fun evictionWritesAreCompletedBeforeSaveReturns() = runBlocking {
        val directory = temporary.newFolder()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(directory, scope, maxRamBytes = 1024)
            repeat(8) { cache.putFrame("main", it, bitmap(Color.RED + it)) }
            cache.persistAllToDisk(listOf("main"), 8)
            assertEquals(8, directory.listFiles()!!.count { it.extension == "png" })
            val restored = FrameCacheManager(directory, scope)
            repeat(8) { assertEquals(Color.RED + it, restored.getFrame("main", it)!!.getPixel(0, 0)) }
        } finally {
            scope.cancel()
        }
    }

    @Test fun diskFailuresAreNotReportedAsSuccessfulSaves() = runBlocking {
        val directory = temporary.newFolder()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(directory, scope)
            cache.putFrame("main", 0, bitmap(Color.RED))
            File(directory, "frame_v2_main_frame_0.png").mkdirs()
            try {
                cache.persistAllToDisk(listOf("main"), 1)
                fail("Expected storage failure")
            } catch (
                _: java.io.IOException,
            ) { }
        } finally {
            scope.cancel()
        }
    }

    @Test fun concurrentReadsDuringEvictionWritesRemainReadableAfterRestart() = runBlocking {
        val directory = temporary.newFolder()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(directory, scope, maxRamBytes = 1024)
            withTimeout(30000) {
                coroutineScope {
                    val writer = launch(Dispatchers.Default) {
                        repeat(64) { cache.putFrame("main", it, bitmap(Color.RED + it)) }
                    }
                    val readers = List(4) {
                        launch(Dispatchers.Default) {
                            repeat(128) { cache.getFrame("missing-layer", it) }
                        }
                    }
                    writer.join()
                    readers.joinAll()
                }
                cache.persistAllToDisk(listOf("main"), 64)
            }
            val reopened = FrameCacheManager(directory, scope)
            repeat(64) { assertEquals(Color.RED + it, reopened.getFrame("main", it)!!.getPixel(0, 0)) }
        } finally {
            scope.cancel()
        }
    }

    @Test fun clearingDuringEvictionCompletionDoesNotLeaveFramesOnDisk() = runBlocking {
        val directory = temporary.newFolder()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(directory, scope, maxRamBytes = 1024)
            repeat(32) { cache.putFrame("main", it, bitmap(Color.BLUE)) }
            withTimeout(30000) { cache.clear() }
            assertNull(cache.getFrame("main", 0))
            assertEquals(0, directory.listFiles()!!.count { it.extension == "png" })
        } finally {
            scope.cancel()
        }
    }
}
