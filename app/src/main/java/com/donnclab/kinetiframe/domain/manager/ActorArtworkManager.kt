package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.rigging.model.ActorArtwork
import com.donnclab.kinetiframe.engine.rigging.model.ActorGraphValidation
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkVariant
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.StudioEffect
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import java.util.UUID

/** Attachment and discrete acting authoring. Library pixels are never mutated or referenced live. */
class ActorArtworkManager(
    private val runtime: RigAssetManager,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val checkpoint: () -> Unit,
    private val reconcile: () -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
) {
    private fun canEdit(state: StudioState) = !state.actor.isPerformanceAuthoring &&
        state.scene.studioMode in setOf(StudioMode.RIG_SETUP, StudioMode.RASTER_DRAWING)

    fun useDrawing(enabled: Boolean) {
        val state = getState()
        if (!canEdit(state)) return
        val definition = state.actorDefinition() ?: return
        val drawing = definition.sceneGraph.drawing ?: return
        if (drawing.enabled == enabled) return
        checkpoint()
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetDefinitions = it.project.puppetDefinitions +
                        (
                            definition.id to
                                definition.copy(
                                    sceneGraph = definition.sceneGraph.copy(drawing = drawing.withEnabled(enabled)),
                                )
                            ),
                ),
                actor = it.actor.copy(previewGraph = null),
            )
        }
    }

    fun setDrawingOverlay(partId: String, enabled: Boolean, behind: Boolean = false) {
        val state = getState()
        if (!canEdit(state)) return
        val definition = state.actorDefinition() ?: return
        val drawing = definition.sceneGraph.drawing ?: return
        val visible = partId in drawing.overlayParts || partId in drawing.underlayParts
        if (partId !in definition.sceneGraph.bones ||
            (visible == enabled && (!enabled || (partId in drawing.underlayParts) == behind))
        ) {
            return
        }
        val changed = drawing.copy(
            composition = drawing.composition.copy(
                overlayParts = if (enabled &&
                    !behind
                ) {
                    drawing.overlayParts + partId
                } else {
                    drawing.overlayParts - partId
                },
                underlayParts = if (enabled &&
                    behind
                ) {
                    drawing.underlayParts + partId
                } else {
                    drawing.underlayParts - partId
                },
            ),
        )
        val graph = definition.sceneGraph.copy(drawing = changed)
        ActorGraphValidation.validate(graph)
        checkpoint()
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetDefinitions = it.project.puppetDefinitions +
                        (definition.id to definition.copy(sceneGraph = graph)),
                ),
                actor = it.actor.copy(previewGraph = null),
            )
        }
    }

    /** One authoring transaction owns both layers and their bind. No special preview-only rig. */
    fun commitDrawing(intent: StudioIntent.Actor.CommitDrawing) {
        val state = getState()
        val definition = state.actorDefinition()
        try {
            require(canEdit(state) && definition?.id == intent.definitionId) { "Actor changed while drawing" }
            val source = intent.document
            require(runtime.getAsset(source.inkAssetId) == null && runtime.getAsset(source.colourAssetId) == null) {
                "Drawing source IDs must be new"
            }
            require(
                !intent.ink.isRecycled && !intent.colour.isRecycled &&
                    intent.ink.width == source.width && intent.ink.height == source.height &&
                    intent.colour.width == source.width && intent.colour.height == source.height,
            )
            require(
                runtime.allocatedBytes() + intent.ink.allocationByteCount + intent.colour.allocationByteCount <=
                    64L * 1024 * 1024,
            ) {
                "Actor artwork and history have reached the 64 MB budget. Save and reopen before continuing."
            }
            val original = requireNotNull(definition).sceneGraph
            val rest = intent.restGraph ?: original
            require(rest.bones.keys == original.bones.keys && rest.rootBoneIds == original.rootBoneIds) {
                "Drawing joint structure changed"
            }
            val graph = rest.copy(drawing = source)
            ActorGraphValidation.validate(graph)
            checkpoint()
            runtime.registerAsset(source.inkAssetId, intent.ink)
            runtime.registerAsset(source.colourAssetId, intent.colour)
            updateState {
                it.copy(
                    project = it.project.copy(
                        puppetDefinitions = it.project.puppetDefinitions +
                            (definition.id to definition.copy(sceneGraph = graph)),
                    ),
                    actor = it.actor.copy(previewGraph = null),
                )
            }
            reconcile()
        } catch (failure: Exception) {
            intent.ink.takeUnless { it.isRecycled }?.recycle()
            intent.colour.takeUnless { it.isRecycled }?.recycle()
            sendEffect(StudioEffect.ShowError(failure.message ?: "Could not bind drawing"))
        }
    }

    fun blendAttachment(intent: StudioIntent.Actor.SetAttachmentBlend) {
        val state = getState()
        if (!canEdit(state)) return
        val definition = state.actorDefinition() ?: return
        val bone = definition.sceneGraph.bones[intent.partId] ?: return
        val old =
            if (intent.variantId.isEmpty()) bone.textureBinding else bone.artworkVariants[intent.variantId]?.binding
        if (old == null || old.skin?.attachmentBlend == false) return
        if ((old.skin?.attachmentBlend == true) == intent.enabled) return
        val pixels = runtime.getAsset(old.assetId)?.takeUnless { it.isRecycled } ?: return
        try {
            val skin = if (intent.enabled) {
                com.donnclab.kinetiframe.engine.rigging.math.ArtworkAttachmentBlend.bind(
                    definition.sceneGraph,
                    bone.id,
                    old,
                    pixels.width,
                    pixels.height,
                )
            } else {
                null
            }
            // Reuse the standard alignment transaction: checkpoint, validate, and reconcile once.
            commitUpdate(
                StudioIntent.Actor.UpdateArtwork(
                    bone.id,
                    intent.variantId,
                    bone.artworkVariants[intent.variantId]?.name ?: bone.name,
                    old.copy(skin = skin),
                ),
                replaceSkin = true,
            )
        } catch (error: Exception) {
            sendEffect(StudioEffect.ShowError(error.message ?: "Could not blend attachment"))
        }
    }

    fun update(intent: StudioIntent.Actor.UpdateArtwork) = commitUpdate(intent)

    private fun commitUpdate(intent: StudioIntent.Actor.UpdateArtwork, replaceSkin: Boolean = false) {
        val state = getState()
        if (!canEdit(state)) return
        val definition = state.actorDefinition() ?: return
        val bone = definition.sceneGraph.bones[intent.partId] ?: return
        val old =
            if (intent.variantId.isEmpty()) bone.textureBinding else bone.artworkVariants[intent.variantId]?.binding
        if (old == null) return
        try {
            val binding = if (replaceSkin) {
                intent.alignment.copy(assetId = old.assetId)
            } else {
                com.donnclab.kinetiframe.engine.rigging.math.ArtworkBindingTransform.apply(
                    definition.sceneGraph,
                    bone.id,
                    old,
                    intent.alignment,
                )
            }
            val changed = if (intent.variantId.isEmpty()) {
                bone.withArtwork(textureBinding = binding)
            } else {
                bone.withArtwork(
                    artworkVariants = bone.artworkVariants + (
                        intent.variantId to
                            ArtworkVariant(intent.name.trim().take(60).ifBlank { "Artwork" }, binding)
                        ),
                )
            }
            val graph = definition.sceneGraph.copy(bones = definition.sceneGraph.bones + (bone.id to changed))
            ActorGraphValidation.validate(graph)
            checkpoint()
            updateState {
                it.copy(
                    project = it.project.copy(
                        puppetDefinitions = it.project.puppetDefinitions +
                            (definition.id to definition.copy(sceneGraph = graph)),
                    ),
                    actor = it.actor.copy(previewGraph = null),
                )
            }
            reconcile()
        } catch (error: Exception) {
            sendEffect(StudioEffect.ShowError(error.message ?: "Could not edit artwork"))
        }
    }

    fun remove(intent: StudioIntent.Actor.RemoveArtwork) {
        val state = getState()
        if (!canEdit(state)) return
        val definition = state.actorDefinition() ?: return
        val bone = definition.sceneGraph.bones[intent.partId] ?: return
        if (intent.variantId.isEmpty() && bone.textureBinding == null && bone.textureSlotId == null) return
        if (intent.variantId.isNotEmpty() && intent.variantId !in bone.artworkVariants) return
        val changed = if (intent.variantId.isEmpty()) {
            bone.withArtwork(textureBinding = null, textureSlotId = null)
        } else {
            bone.withArtwork(artworkVariants = bone.artworkVariants - intent.variantId)
        }
        val graph = definition.sceneGraph.copy(bones = definition.sceneGraph.bones + (bone.id to changed))
        val slot = bone.textureSlotId?.takeIf {
            intent.variantId.isEmpty() &&
                graph.bones.values.none { node -> node.textureSlotId == it }
        }
        checkpoint()
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetDefinitions = it.project.puppetDefinitions + (
                        definition.id to definition.copy(
                            sceneGraph = graph,
                            textureSlots = if (slot ==
                                null
                            ) {
                                definition.textureSlots
                            } else {
                                definition.textureSlots - slot
                            },
                        )
                        ),
                    puppetInstances = it.project.puppetInstances.mapValues { (_, instance) ->
                        if (instance.definitionId != definition.id || intent.variantId.isEmpty()) {
                            instance
                        } else {
                            instance.copy(
                                artworkKeys = instance.artworkKeys.map { key ->
                                    if (key.selections[bone.id] ==
                                        intent.variantId
                                    ) {
                                        key.copy(selections = key.selections - bone.id)
                                    } else {
                                        key
                                    }
                                }.filter { key -> key.selections.isNotEmpty() },
                            )
                        }
                    },
                ),
                actor = it.actor.copy(previewGraph = null),
            )
        }
        // The source pixels survive while Undo still references them.
        reconcile()
    }
    fun attach(intent: StudioIntent.Actor.AttachArtwork) {
        val state = getState()
        val definition = state.actorDefinition()
        val bone = definition?.sceneGraph?.bones?.get(intent.partId)
        if (state.actor.isPerformanceAuthoring || state.scene.studioMode != StudioMode.RIG_SETUP || bone == null) {
            intent.bitmap.recycle()
            return
        }
        try {
            require(!intent.bitmap.isRecycled && intent.bitmap.width <= 2048 && intent.bitmap.height <= 2048) {
                "Artwork is too large"
            }
            require(runtime.allocatedBytes() + intent.bitmap.allocationByteCount <= 64L * 1024 * 1024) {
                "Actor artwork and undo history have reached the 64 MB import budget"
            }
            require(intent.replaceVariantId == null || intent.replaceVariantId in bone.artworkVariants) {
                "Artwork no longer exists"
            }
            require(!intent.asVariant || intent.replaceVariantId != null || bone.artworkVariants.size < 24) {
                "This part already has 24 variants"
            }
            val id = UUID.randomUUID().toString()
            require(!intent.attachmentBlend || intent.continuousChildId == null) {
                "Choose a continuous limb or an attachment blend, not both"
            }
            val aligned = intent.alignment.copy(assetId = id)
            val binding = intent.continuousChildId?.let { child ->
                aligned.copy(
                    skin =
                    com.donnclab.kinetiframe.engine.rigging.math.ArtworkSkinBuilder.bind(
                        definition.sceneGraph,
                        bone.id,
                        child,
                        aligned,
                        intent.bitmap.width,
                        intent.bitmap.height,
                    ),
                )
            }
                ?: if (intent.attachmentBlend) {
                    aligned.copy(
                        skin =
                        com.donnclab.kinetiframe.engine.rigging.math.ArtworkAttachmentBlend.bind(
                            definition.sceneGraph,
                            bone.id,
                            aligned,
                            intent.bitmap.width,
                            intent.bitmap.height,
                        ),
                    )
                } else {
                    aligned
                }
            val changed = if (intent.asVariant ||
                intent.replaceVariantId != null
            ) {
                bone.withArtwork(
                    artworkVariants = bone.artworkVariants +
                        (
                            (intent.replaceVariantId ?: id) to
                                ArtworkVariant(intent.name.trim().take(60).ifBlank { "Artwork" }, binding)
                            ),
                )
            } else {
                bone.withArtwork(textureBinding = binding, textureSlotId = null)
            }
            val graph = definition.sceneGraph.copy(
                bones = definition.sceneGraph.bones + (bone.id to changed),
                drawing = definition.sceneGraph.drawing?.let {
                    if (it.enabled && !intent.asVariant &&
                        bone.id !in it.underlayParts
                    ) {
                        it.copy(
                            composition = it.composition.copy(
                                overlayParts =
                                it.overlayParts + bone.id,
                            ),
                        )
                    } else {
                        it
                    }
                },
            )
            val obsoleteSlot = bone.textureSlotId?.takeIf {
                !intent.asVariant && intent.replaceVariantId == null &&
                    graph.bones.values.none { node -> node.textureSlotId == it }
            }
            val slots = if (obsoleteSlot == null) definition.textureSlots else definition.textureSlots - obsoleteSlot
            ActorGraphValidation.validate(graph)
            checkpoint()
            runtime.registerAsset(id, intent.bitmap)
            updateState {
                it.copy(
                    project = it.project.copy(
                        puppetDefinitions = it.project.puppetDefinitions +
                            (definition.id to definition.copy(sceneGraph = graph, textureSlots = slots)),
                    ),
                    actor = it.actor.copy(previewGraph = null),
                )
            }
            reconcile()
        } catch (error: Exception) {
            if (!intent.bitmap.isRecycled) intent.bitmap.recycle()
            sendEffect(StudioEffect.ShowError(error.message ?: "Could not attach artwork"))
        }
    }

    fun select(part: String, variant: String) {
        val state = getState()
        val definition = state.actorDefinition() ?: return
        val bone = definition.sceneGraph.bones[part] ?: return
        if (variant.isNotEmpty() && variant !in bone.artworkVariants) return
        if (state.scene.studioMode != StudioMode.ANIMATE_IK) return
        val instance = state.activeActor() ?: return
        if (instance.artworkKeys.size >= 720 &&
            instance.artworkKeys.none { it.timeMicros == state.actor.previewTimeMicros }
        ) {
            sendEffect(StudioEffect.ShowError("This performance already has 720 artwork keys"))
            return
        }
        checkpoint()
        val keys = ActorArtwork.put(instance.artworkKeys, state.actor.previewTimeMicros, part, variant)
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetInstances = it.project.puppetInstances +
                        (instance.id to instance.copy(artworkKeys = keys)),
                ),
                actor = it.actor.copy(previewGraph = null),
            )
        }
    }
}
