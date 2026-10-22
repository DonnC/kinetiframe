package com.donnclab.kinetiframe.actors.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PuppetArtworkHitTesterTest {
    @Test fun `transparent foreground allows selection of artwork behind it`() {
        val assets = RigAssetManager()
        assets.registerAsset(
            "back",
            Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.BLUE)
            },
        )
        assets.registerAsset(
            "front",
            Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
                setPixel(10, 10, Color.RED)
            },
        )
        try {
            val commands =
                listOf(
                    PuppetRenderCommand("back", Matrix(), boneId = "body"),
                    PuppetRenderCommand("front", Matrix(), boneId = "eyes"),
                )
            assertEquals("body", PuppetArtworkHitTester.hit(Offset(5f, 5f), commands, assets))
            assertEquals("eyes", PuppetArtworkHitTester.hit(Offset(10f, 10f), commands, assets))
        } finally {
            assets.clear()
        }
    }

    @Test fun `deformed mesh rejects empty bounding box and transparent source pixels`() {
        val assets = RigAssetManager()
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
            setPixel(10, 10, Color.TRANSPARENT)
        }
        assets.registerAsset("arm", bitmap)
        try {
            // A diagonal parallelogram occupies only part of its rectangular bounds.
            val mesh = PuppetSkinMesh(20, 20, 1, 1, floatArrayOf(0f, 0f, 20f, 20f, 0f, 10f, 20f, 30f))
            val command = PuppetRenderCommand("arm", Matrix(), mesh, "arm")
            assertNull(PuppetArtworkHitTester.hit(Offset(18f, 2f), listOf(command), assets))
            assertEquals("arm", PuppetArtworkHitTester.hit(Offset(5f, 10f), listOf(command), assets))
            assertNull(PuppetArtworkHitTester.hit(Offset(10f, 15f), listOf(command), assets))
        } finally {
            assets.clear()
        }
    }
}
