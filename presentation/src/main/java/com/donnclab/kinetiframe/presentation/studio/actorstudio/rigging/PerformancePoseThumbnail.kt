package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.actors.render.PuppetArtworkRenderer
import com.donnclab.kinetiframe.actors.render.PuppetRenderPlan
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.actorPreviewDuration
import com.donnclab.kinetiframe.studio.actorPreviewPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val thumbnailDispatcher = Dispatchers.Default.limitedParallelism(2)

/** Lazy cells own only their small preview, never stage-sized copies of source textures. */
@Composable
internal fun performancePoseThumbnail(state: StudioState, assets: RigAssetManager?, time: Long): Bitmap? {
    val instance = state.activeActor()
    val definition = state.actorDefinition()
    val preset = state.actorPreviewPreset()
    val duration = state.actorPreviewDuration()
    val bitmap by produceState<Bitmap?>(null, listOf(instance, definition, preset, duration, time, assets)) {
        value = withContext(thumbnailDispatcher) {
            val evaluated = state.copy(
                actor = state.actor.copy(previewTimeMicros = time, previewGraph = null),
            ).actorDefinition(true)
            if (assets == null || instance == null || evaluated == null) return@withContext null
            // Asset recycling shares this lock: hold it for the complete native render batch.
            synchronized(assets) {
                val commands = PuppetRenderPlan.create(evaluated, instance)
                val bounds = commands.flatMap { command ->
                    assets.getAsset(command.assetId)?.let {
                        PuppetArtworkRenderer.outline(command, it.width, it.height).toList()
                    }.orEmpty()
                }
                if (bounds.isEmpty()) return@synchronized null
                val xs = bounds.filterIndexed { index, _ -> index % 2 == 0 }
                val ys = bounds.filterIndexed { index, _ -> index % 2 == 1 }
                val left = xs.min()
                val top = ys.min()
                val width = (xs.max() - left).coerceAtLeast(1f)
                val height = (ys.max() - top).coerceAtLeast(1f)
                val scale = minOf(88f / width, 88f / height)
                Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).also { result ->
                    val canvas = Canvas(result)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.translate((96 - width * scale) / 2, (96 - height * scale) / 2)
                    canvas.scale(scale, scale)
                    canvas.translate(-left, -top)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    commands.forEach { command ->
                        assets.getAsset(command.assetId)?.let {
                            PuppetArtworkRenderer.draw(canvas, command, it, paint)
                        }
                    }
                }
            }
        }
    }
    return bitmap
}
