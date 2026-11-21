package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.domain.actorstudio.ActorStudioManager
import com.donnclab.kinetiframe.studio.domain.actorstudio.RiggingManager
import com.donnclab.kinetiframe.studio.drawing.BrushRenderers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActorRasterErasingTest {
    @Test fun `clear eraser removes pixels and empty cel removes its previous bound artwork`() {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val assets = RigAssetManager()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val pixels = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) })
            studio.handle(StudioIntent.Actor.Create(ActorTemplate.CUSTOM))
            val manager = RiggingManager(scope, assets, { state }, { state = it(state) }, {})
            pixels.eraseColor(Color.RED)
            manager.autoBindLayerContent("root", pixels)
            assertNotNull(state.actorDefinition()!!.sceneGraph.bones.getValue("root").textureBinding)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
            BrushRenderers.draw(
                Canvas(pixels),
                listOf(TouchPoint(16f, 16f, timestamp = 1L)),
                100f,
                paint,
                BrushSnapshot(),
            )
            assertEquals(0, Color.alpha(pixels.getPixel(16, 16)))
            manager.autoBindLayerContent("root", pixels)
            assertNull(state.actorDefinition()!!.sceneGraph.bones.getValue("root").textureBinding)
            pixels.eraseColor(Color.BLUE)
            manager.autoBindLayerContent("root", pixels)
            assertNotNull(state.actorDefinition()!!.sceneGraph.bones.getValue("root").textureBinding)
        } finally {
            scope.cancel()
            assets.clear()
            pixels.recycle()
        }
    }
}
