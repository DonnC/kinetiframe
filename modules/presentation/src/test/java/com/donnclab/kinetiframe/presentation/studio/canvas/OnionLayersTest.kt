// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.LayerState
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
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

/** R3-23: onion skin shows every visible layer, or only the active one when asked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnionLayersTest {
    @get:Rule val temporary = TemporaryFolder()

    private val back = Layer(id = "back", name = "Back", zIndex = 0)
    private val ink = Layer(id = "ink", name = "Ink", zIndex = 1)
    private val state = StudioState(
        animation = AnimationState(totalFrames = 1),
        layers = LayerState(layers = listOf(back, ink), frameLayers = mapOf(0 to listOf(back, ink))),
    )

    @Test fun everyVisibleLayerShowsByDefault() = withCache { cache ->
        val onion = onionFrameBitmap(cache, state, 0, "ink", activeLayerOnly = false)!!
        assertEquals(Color.RED, onion.getPixel(1, 4))
        assertEquals(Color.BLUE, onion.getPixel(6, 4))
    }

    @Test fun activeLayerOnlyShowsJustThatLayer() = withCache { cache ->
        val onion = onionFrameBitmap(cache, state, 0, "ink", activeLayerOnly = true)!!
        assertEquals(Color.TRANSPARENT, onion.getPixel(1, 4))
        assertEquals(Color.BLUE, onion.getPixel(6, 4))
    }

    @Test fun hiddenLayersStayOut() = withCache { cache ->
        val stack = listOf(back.copy(isVisible = false), ink)
        val hidden = state.copy(layers = state.layers.copy(layers = stack, frameLayers = mapOf(0 to stack)))
        assertEquals(Color.TRANSPARENT, onionFrameBitmap(cache, hidden, 0, "ink", false)!!.getPixel(1, 4))
    }

    /** Back is red on its left half, ink blue on its right half. */
    private fun withCache(block: suspend (FrameCacheManager) -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(temporary.newFolder(), scope)
            cache.putFrame("back", 0, half(Color.RED, left = true))
            cache.putFrame("ink", 0, half(Color.BLUE, left = false))
            block(cache)
        } finally {
            scope.cancel()
        }
    }

    private fun half(colour: Int, left: Boolean) = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until 8) for (y in 0 until 8) if ((x < 4) == left) setPixel(x, y, colour)
    }
}
