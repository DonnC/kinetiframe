package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.engine.model.ProjectAsset
import com.donnclab.kinetiframe.engine.model.TextureSlot
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.StudioEffect
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.actorDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/** Optional geometric sample skin. Replaces only missing artwork; bitmap work stays off the UI thread. */
class ActorSampleArtwork(
    private val scope: CoroutineScope,
    private val assets: RigAssetManager,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val checkpoint: () -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
) {
    private var job: Job? = null
    fun add(color: Int) {
        if (job?.isActive == true) return
        val definition = getState().actorDefinition() ?: return
        job = scope.launch {
            val prepared = mutableMapOf<String, Pair<String, ProjectAsset>>()
            var installed = false
            try {
                withContext(Dispatchers.Default) {
                    definition.sceneGraph.bones.values.filter { it.textureBinding == null }.forEach { bone ->
                        ensureActive()
                        val width = ceil(bone.length.coerceIn(1f, 2048f)).toInt() + 32
                        val bitmap = Bitmap.createBitmap(width, 48, Bitmap.Config.ARGB_8888)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                        Canvas(bitmap).drawRoundRect(0f, 0f, width.toFloat(), 48f, 24f, 24f, paint)
                        val id = assets.registerAsset(bitmap)
                        prepared[bone.id] = id to ProjectAsset(id, AssetKind.RASTER_TEXTURE, width = width, height = 48)
                    }
                }
                // A switch, fit or undo while preparing must not install artwork on a stale rig.
                if (getState().actorDefinition() != definition ||
                    getState().scene.studioMode != StudioMode.RIG_SETUP
                ) {
                    return@launch
                }
                if (prepared.isEmpty()) return@launch
                checkpoint()
                updateState { state ->
                    val bones = definition.sceneGraph.bones.mapValues { (id, bone) ->
                        prepared[id]?.let { (assetId, _) ->
                            bone.withArtwork(
                                textureBinding = TextureBinding(assetId, 16f, 24f),
                                textureSlotId = "slot-$id",
                            )
                        }
                            ?: bone
                    }
                    state.copy(
                        project = state.project.copy(
                            assets = state.project.assets + prepared.values.associate { it.first to it.second },
                            puppetDefinitions = state.project.puppetDefinitions + (
                                definition.id to definition.copy(
                                    sceneGraph = definition.sceneGraph.copy(bones = bones),
                                    textureSlots =
                                    definition.textureSlots +
                                        prepared.map { (boneId, item) ->
                                            "slot-$boneId" to
                                                TextureSlot("slot-$boneId", item.first)
                                        }.toMap(),
                                )
                                ),
                        ),
                        actor = state.actor.copy(showSource = false),
                    )
                }
                installed = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                sendEffect(StudioEffect.ShowError("Sample artwork could not be created: ${error.message}"))
            } finally {
                if (!installed) prepared.values.forEach { assets.releaseAsset(it.first) }
            }
        }
    }
}
