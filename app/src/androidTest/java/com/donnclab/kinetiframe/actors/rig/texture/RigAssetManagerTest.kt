package com.donnclab.kinetiframe.actors.rig.texture

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RigAssetManagerTest {
    @Test
    fun releaseRemovesAndRecyclesOnlyTheRequestedAsset() {
        val manager = RigAssetManager()
        val first = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val second = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val firstId = manager.registerAsset(first)
        val secondId = manager.registerAsset(second)

        manager.releaseAsset(firstId)

        assertFalse(manager.contains(firstId))
        assertNull(manager.getAsset(firstId))
        assertTrue(first.isRecycled)
        assertTrue(manager.contains(secondId))
        assertSame(second, manager.getAsset(secondId))
        manager.clear()
        assertTrue(second.isRecycled)
    }
}
