package com.donnclab.kinetiframe.studio.media

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Mirrors RigAssetManagerTest's style for the project-level asset cache -- see
 *  [ProjectAssetManager]'s doc comment for why this is a separate, simpler manager. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProjectAssetManagerTest {
    private fun bitmap(): Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    @Test
    fun `a registered bitmap is retrievable by its id`() {
        val manager = ProjectAssetManager()
        val bmp = bitmap()
        manager.registerAsset("background-a", bmp)
        assertSame(bmp, manager.getAsset("background-a"))
    }

    @Test
    fun `getAsset is null-safe for a null id`() {
        val manager = ProjectAssetManager()
        assertNull(manager.getAsset(null))
    }

    @Test
    fun `getAsset returns null for an id that was never registered`() {
        val manager = ProjectAssetManager()
        assertNull(manager.getAsset("nothing-here"))
    }

    @Test
    fun `releasing an asset drops cache ownership without recycling pixels still held by renderers`() {
        val manager = ProjectAssetManager()
        val bmp = bitmap()
        manager.registerAsset("background-a", bmp)
        val revision = manager.revision.value

        manager.releaseAsset("background-a")

        assertFalse("a held render bitmap must remain usable after cache release", bmp.isRecycled)
        assertNull("a released asset must no longer be retrievable", manager.getAsset("background-a"))
        assertEquals(revision + 1, manager.revision.value)
        assertEquals(0, bmp.getPixel(0, 0))
    }

    @Test
    fun `registering a new bitmap under an id already in use replaces it without recycling the old one`() {
        // BackgroundManager relies on this: it always assigns a fresh, never-reused asset ID per
        // image, so registerAsset is never actually called twice for the same ID in practice --
        // this just confirms the map-replace semantics don't accidentally recycle a bitmap a
        // caller might still be holding, since unlike RigAssetManager's same-named overload this
        // one has no such "restore a persisted asset" contract to honor.
        val manager = ProjectAssetManager()
        val first = bitmap()
        val second = bitmap()
        manager.registerAsset("background-a", first)
        manager.registerAsset("background-a", second)

        assertFalse("replacing must not recycle the bitmap being replaced out", first.isRecycled)
        assertSame(second, manager.getAsset("background-a"))
    }
}
