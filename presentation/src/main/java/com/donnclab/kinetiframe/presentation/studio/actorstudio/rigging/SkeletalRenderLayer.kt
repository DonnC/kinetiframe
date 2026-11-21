package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.donnclab.kinetiframe.actors.render.PuppetArtworkRenderer
import com.donnclab.kinetiframe.actors.render.PuppetRenderPlan
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition

/**
 * Draws the confirmed actor skin from the actor document, never from the selected raster layer.
 *
 * Per-part artwork layers intentionally carry only `boundBoneId`; requiring the currently
 * selected layer to carry a puppet instance made a finished actor disappear whenever the
 * animator selected a part to repaint it. The actor is document-owned, so its assembled skin
 * must remain visible during painting, fitting, and performance posing alike.
 */
@Composable
fun SkeletalRenderLayer(
    state: StudioState,
    rigAssetManager: RigAssetManager,
    modifier: Modifier = Modifier,
    eraseSelectedArtwork: ((android.graphics.Canvas) -> Unit)? = null,
) {
    val commands =
        remember(state.project, state.layers, state.animation.currentFrame, state.actor, state.scene.studioMode) {
            val instance = state.activeActor()
            val definition = state.actorDefinition(true)
            if (instance == null || definition == null) {
                emptyList()
            } else {
                PuppetRenderPlan.create(definition, instance)
            }
        }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val viewportMatrix = remember(state.canvas.matrixValues) {
        android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.concat(viewportMatrix)
            commands.forEach { command ->
                rigAssetManager.getAsset(command.assetId)?.takeUnless { it.isRecycled }?.let { bitmap ->
                    val isolated = eraseSelectedArtwork != null && command.boneId == state.scene.selectedBoneId
                    val save = if (isolated) {
                        nativeCanvas.saveLayer(
                            0f,
                            0f,
                            state.project.stage.width.toFloat(),
                            state.project.stage.height.toFloat(),
                            null,
                        )
                    } else {
                        -1
                    }
                    PuppetArtworkRenderer.draw(nativeCanvas, command, bitmap, paint)
                    if (isolated) {
                        eraseSelectedArtwork?.invoke(nativeCanvas)
                        nativeCanvas.restoreToCount(save)
                    }
                }
            }
            nativeCanvas.restore()
        }
    }
}
