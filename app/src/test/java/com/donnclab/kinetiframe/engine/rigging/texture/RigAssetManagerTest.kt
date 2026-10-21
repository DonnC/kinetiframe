package com.donnclab.kinetiframe.engine.rigging.texture

import android.graphics.Bitmap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Verifies the mark-and-sweep bitmap lifecycle that protects undo/redo history from recycling. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RigAssetManagerTest {

    private fun bitmap(): Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    @Test
    fun `retainOnly recycles only bitmaps outside the reachable set`() {
        val manager = RigAssetManager()
        val kept = bitmap()
        val dropped = bitmap()
        val keptId = manager.registerAsset(kept)
        val droppedId = manager.registerAsset(dropped)

        manager.retainOnly(setOf(keptId))

        assertFalse("a still-reachable bitmap must survive a sweep", kept.isRecycled)
        assertTrue("an unreachable bitmap must be recycled by the sweep", dropped.isRecycled)
        assertTrue(manager.contains(keptId))
        assertFalse(manager.contains(droppedId))
    }

    @Test
    fun `retainOnly is a no-op when every registered asset is still reachable`() {
        val manager = RigAssetManager()
        val kept = bitmap()
        val id = manager.registerAsset(kept)

        // The reachable set may legitimately contain IDs for assets this manager never held
        // (for example another actor's textures); that must never crash or affect this manager.
        manager.retainOnly(setOf(id, "some-other-actors-asset-id"))

        assertFalse(kept.isRecycled)
        assertTrue(manager.contains(id))
    }

    @Test
    fun `retainOnly with an empty reachable set recycles everything`() {
        val manager = RigAssetManager()
        val a = bitmap()
        val b = bitmap()
        manager.registerAsset(a)
        manager.registerAsset(b)

        manager.retainOnly(emptySet())

        assertTrue(a.isRecycled)
        assertTrue(b.isRecycled)
        assertTrue(manager.size() == 0)
    }

    @Test
    fun `releaseAsset still recycles immediately for pre-checkpoint temporary assets`() {
        val manager = RigAssetManager()
        val bitmap = bitmap()
        val id = manager.registerAsset(bitmap)

        manager.releaseAsset(id)

        assertTrue(bitmap.isRecycled)
        assertFalse(manager.contains(id))
    }
}
