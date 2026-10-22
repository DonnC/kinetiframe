package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.engine.model.ProjectAsset
import com.donnclab.kinetiframe.engine.model.TextureSlot
import com.donnclab.kinetiframe.engine.model.bindTextureToBone
import com.donnclab.kinetiframe.engine.rigging.math.RigGeometry
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkAlignment
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.PivotPlacement
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import com.donnclab.kinetiframe.engine.rigging.texture.JointProcessor
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.engine.rigging.texture.TextureExtractor
import com.donnclab.kinetiframe.ui.StudioEffect
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import com.donnclab.kinetiframe.ui.currentFrameLayers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.floor

/**
 * Manager for rigging and pivot operations.
 */
class RiggingManager(
    private val scope: CoroutineScope,
    private val rigAssetManager: RigAssetManager,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
    private val checkpoint: () -> Unit = {},
    /**
     * Sweeps runtime bitmaps down to what the live document plus undo/redo history still need
     * (see `ActorStudioManager.reconcileRuntimeAssets`). A rebind here can drop the checkpointed
     * snapshot's last live reference to the previous texture, so releasing it outright would
     * recycle a bitmap that snapshot still needs.
     */
    private val reconcileRuntimeAssets: () -> Unit = {},
) {
    private var extractionGeneration = 0
    fun preparePivotPlacement(sourceFrame: Bitmap, lassoPath: Path, dilationRadius: Int) {
        val definition = getState().actorDefinition()
        val targetBoneId = getState().scene.selectedBoneId
        if (definition?.sceneGraph?.bones?.containsKey(targetBoneId) != true) {
            sendEffect(StudioEffect.ShowError("Select a named part in the inspector before extracting its artwork."))
            return
        }
        val generation = ++extractionGeneration
        scope.launch {
            var registeredId: String? = null
            var installed = false
            try {
                val placement = kotlinx.coroutines.withContext(Dispatchers.Default) {
                    val extracted = TextureExtractor.extractLassoCrop(sourceFrame, lassoPath)
                    val processed = try {
                        JointProcessor.dilateJointEdges(extracted, dilationRadius)
                    } catch (
                        error: Exception,
                    ) {
                        extracted.recycle()
                        throw error
                    }
                    if (processed !== extracted) extracted.recycle()
                    val assetId = rigAssetManager.registerAsset(processed)
                    registeredId = assetId
                    val bounds = lassoPath.getBounds()
                    PivotPlacement(
                        assetId, processed.width, processed.height,
                        processed.width / 2f, processed.height / 2f,
                        floor(bounds.left).toInt().coerceIn(0, sourceFrame.width).toFloat() - dilationRadius,
                        floor(bounds.top).toInt().coerceIn(0, sourceFrame.height).toFloat() - dilationRadius,
                        definition.id, targetBoneId,
                    )
                }
                if (generation != extractionGeneration ||
                    getState().actorDefinition()?.id != definition.id
                ) {
                    return@launch
                }
                val previous = getState().scene.pivotPlacement
                previous?.let { rigAssetManager.releaseAsset(it.assetId) }
                updateState { state ->
                    state.copy(
                        project = state.project.copy(
                            assets = (state.project.assets - listOfNotNull(previous?.assetId)) +
                                (
                                    placement.assetId to ProjectAsset(
                                        placement.assetId,
                                        AssetKind.RASTER_TEXTURE,
                                        width = placement.bitmapWidth,
                                        height = placement.bitmapHeight,
                                    )
                                    ),
                        ),
                        scene = state.scene.copy(pivotPlacement = placement),
                    )
                }
                installed = true
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                sendEffect(StudioEffect.ShowError(error.message ?: "Unable to prepare cutout"))
            } finally {
                // Cancellation may happen between worker completion and delivery to the main dispatcher.
                if (!installed) registeredId?.let(rigAssetManager::releaseAsset)
            }
        }
    }

    fun updatePivot(x: Float, y: Float) {
        updateState { state ->
            state.scene.pivotPlacement?.let { placement ->
                state.copy(
                    scene = state.scene.copy(
                        pivotPlacement = placement.copy(
                            pivotX = x.coerceIn(0f, placement.bitmapWidth.toFloat()),
                            pivotY = y.coerceIn(0f, placement.bitmapHeight.toFloat()),
                        ),
                    ),
                )
            } ?: state
        }
    }

    /** Explicit selection wins over nearest-endpoint inference, especially on overlapping profiles. */
    fun addBoneAt(x: Float, y: Float) {
        val before = getState()
        if (!x.isFinite() || !y.isFinite() || before.actor.isPerformanceAuthoring ||
            before.activeActor() == null
        ) {
            return
        }
        checkpoint()
        updateState { state ->
            // See activeActor()'s doc: resolving the actor from the *active layer* made this bail
            // out silently whenever a part layer was selected, i.e. during normal authoring.
            val instance = state.activeActor() ?: return@updateState state
            val definition = state.project.puppetDefinitions[instance.definitionId] ?: return@updateState state
            val graph = definition.sceneGraph
            val id = "bone-${UUID.randomUUID().toString().take(8)}"
            val computed = com.donnclab.kinetiframe.engine.rigging.math.ForwardKinematicsSolver.resolveWorldStates(
                graph,
            )
            val parent = computed[state.scene.selectedBoneId] ?: computed.values.minByOrNull { bone ->
                val dx = bone.endPoint.x - x
                val dy = bone.endPoint.y - y
                dx * dx + dy * dy
            }
            val bone = if (parent == null) {
                BoneNode(
                    hierarchy = BoneHierarchy(id, "Bone ${graph.bones.size + 1}", null),
                    rest = BoneRest(100f, localTransform = Transform(x = x, y = y)),
                )
            } else {
                val parentNode = graph.bones.getValue(parent.id)
                val inverse = android.graphics.Matrix()
                parent.worldMatrix.invert(inverse)
                val target = floatArrayOf(x, y)
                inverse.mapPoints(target)
                val localDx = target[0] - parentNode.length
                val localDy = target[1]
                BoneNode(
                    hierarchy = BoneHierarchy(id, "Bone ${graph.bones.size + 1}", parent.id),
                    rest = BoneRest(
                        Math.hypot(localDx.toDouble(), localDy.toDouble()).toFloat().coerceAtLeast(1f),
                        localTransform = Transform(
                            x = parentNode.length,
                            y = 0f,
                            rotationDegrees = Math.toDegrees(atan2(localDy, localDx).toDouble()).toFloat(),
                        ),
                    ),
                )
            }
            state.copy(
                project = state.project.copy(
                    puppetDefinitions = state.project.puppetDefinitions + (
                        definition.id to definition.copy(
                            sceneGraph = graph.copy(
                                rootBoneIds = if (parent == null) graph.rootBoneIds + bone.id else graph.rootBoneIds,
                                bones = graph.bones + (bone.id to bone),
                            ),
                        )
                        ),
                ),
            )
        }
    }

    fun confirmPivot() {
        val pending = getState().scene.pivotPlacement ?: return
        if (getState().actorDefinition()?.id != pending.definitionId) {
            cancelPivot()
            return
        }
        checkpoint()
        updateState { state ->
            val placement = state.scene.pivotPlacement ?: return@updateState state
            // See activeActor()'s doc: resolving the actor from the *active layer* made this bail
            // out silently whenever a part layer was selected, i.e. during normal authoring.
            val instance = state.activeActor() ?: return@updateState state
            val definition = state.project.puppetDefinitions[instance.definitionId] ?: return@updateState state
            val id = placement.targetBoneId ?: return@updateState state
            val existing = definition.sceneGraph.bones[id] ?: return@updateState state
            val textureSlotId = "slot-$id"
            val world = RigGeometry.resolve(definition.sceneGraph).getValue(id)
            val offset = RigGeometry.rotate(
                Offset(
                    placement.documentLeft + placement.pivotX,
                    placement.documentTop + placement.pivotY,
                ) - world.start,
                -world.angle,
            )
            val bone = existing.withArtwork(
                textureSlotId = textureSlotId,
                textureBinding = TextureBinding(
                    assetId = placement.assetId,
                    pivot = com.donnclab.kinetiframe.core.geometry.Point2(placement.pivotX, placement.pivotY),
                    alignment = ArtworkAlignment(
                        com.donnclab.kinetiframe.core.geometry.Point2(offset.x, offset.y),
                        -world.angle,
                    ),
                ),
            )
            val mutation = state.project.bindTextureToBone(
                definitionId = definition.id,
                boneId = bone.id,
                slot = TextureSlot(id = textureSlotId, defaultAssetId = placement.assetId),
                binding = bone.textureBinding!!,
                asset = ProjectAsset(
                    placement.assetId,
                    AssetKind.RASTER_TEXTURE,
                    width = placement.bitmapWidth,
                    height = placement.bitmapHeight,
                ),
            )
            state.copy(project = mutation.document, scene = state.scene.copy(pivotPlacement = null))
        }
        // Rebinding may drop the checkpointed snapshot's last live reference to the previous
        // texture; reconcile against the full reachable set instead of releasing it outright.
        reconcileRuntimeAssets()
    }

    /**
     * "Tap a bone, then sketch" binding: whenever a stroke settles on a layer armed to a bone
     * (see [com.donnclab.kinetiframe.models.Layer.boundBoneId]), the whole layer's drawn content -- auto
     * -cropped to its opaque bounds -- becomes that bone's texture, with the bone's own joint as
     * the pivot. No manual lasso-cut or pivot-drag needed; [preparePivotPlacement] +
     * [confirmPivot] still exist for pixel-precise manual control.
     *
     * Runs synchronously and re-fires on every bake (including undo/redo, since those also
     * rebuild the frame bitmap) -- there's no separate checkpoint here because the binding is a
     * pure function of the layer's current pixels, so it self-corrects whenever those change.
     */
    fun autoBindLayerContent(boneId: String, frameBitmap: Bitmap) {
        if (frameBitmap.isRecycled) return
        val definition = getState().actorDefinition() ?: return
        val graph = definition.sceneGraph
        val bone = graph.bones[boneId] ?: return
        val bounds = TextureExtractor.findContentBounds(frameBitmap)
        if (bounds == null) {
            // Erasing the last pixel must remove the old bound surface, not resurrect it.
            // Undo replays the cel and rebinds normally; it does not depend on this asset.
            val slotId = bone.textureSlotId
            if (bone.textureBinding == null || slotId == null) return
            updateState { current ->
                current.copy(
                    project = current.project.copy(
                        puppetDefinitions = current.project.puppetDefinitions + (
                            definition.id to definition.copy(
                                sceneGraph = graph.copy(
                                    bones =
                                    graph.bones +
                                        (boneId to bone.withArtwork(textureBinding = null, textureSlotId = null)),
                                ),
                                textureSlots = definition.textureSlots - slotId,
                            )
                            ),
                    ),
                )
            }
            reconcileRuntimeAssets()
            return
        }
        val cropped = Bitmap.createBitmap(frameBitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
        val assetId = rigAssetManager.registerAsset(cropped)
        val world = RigGeometry.resolve(graph).getValue(boneId)
        val binding = TextureBinding(
            assetId = assetId,
            pivot = com.donnclab.kinetiframe.core.geometry.Point2(
                world.start.x - bounds.left,
                world.start.y - bounds.top,
            ),
            alignment = ArtworkAlignment(rotationDegrees = -world.angle),
        )
        val textureSlotId = bone.textureSlotId ?: "slot-$boneId"
        val previousAssetId = bone.textureBinding?.assetId
        updateState { current ->
            val mutation = current.project.bindTextureToBone(
                definitionId = definition.id,
                boneId = boneId,
                slot = TextureSlot(id = textureSlotId, defaultAssetId = assetId),
                binding = binding,
                asset = ProjectAsset(assetId, AssetKind.RASTER_TEXTURE, width = cropped.width, height = cropped.height),
            )
            current.copy(project = mutation.document)
        }
        if (previousAssetId != null && previousAssetId != assetId) rigAssetManager.releaseAsset(previousAssetId)
        reconcileRuntimeAssets()
    }

    fun cancelPivot() {
        extractionGeneration++
        updateState { state ->
            val placement = state.scene.pivotPlacement
            if (placement != null) {
                rigAssetManager.releaseAsset(placement.assetId)
            }
            state.copy(
                project = if (placement == null) {
                    state.project
                } else {
                    state.project.copy(
                        assets = state.project.assets - placement.assetId,
                    )
                },
                scene = state.scene.copy(pivotPlacement = null),
            )
        }
    }
}
