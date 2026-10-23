package com.donnclab.kinetiframe.actors.domain

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.math.ActorJointHandle
import com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose
import com.donnclab.kinetiframe.actors.rig.math.ActorPartStack
import com.donnclab.kinetiframe.actors.rig.math.ActorPoseEditor
import com.donnclab.kinetiframe.actors.rig.math.ArtworkSkinRestBinding
import com.donnclab.kinetiframe.actors.rig.math.ArtworkSkinning
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.actors.rig.math.TwoBoneSolver
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.actors.rig.model.BoneNode
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.project.model.LayerType
import com.donnclab.kinetiframe.project.model.PuppetDefinition
import com.donnclab.kinetiframe.project.model.PuppetInstance
import com.donnclab.kinetiframe.project.model.unbindTextureFromBone
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.currentFrameLayers
import java.util.UUID

/**
 * Actor authoring transactions. Undo history and the lifetime of runtime bitmaps are handled by
 * [ActorEditHistory].
 */
class ActorStudioManager(
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val retainRuntimeAssets: (Set<String>) -> Unit = {},
) {
    private val history = ActorEditHistory(getState, updateState, retainRuntimeAssets)
    private var artworkDragStart: Offset? = null
    private var artworkDragBase: SceneGraph? = null
    private var motionDragBase: SceneGraph? = null

    /** Continuous takes own their own baseline and must not leak it into later manual edits. */
    fun clearMotionDragBaseline() {
        motionDragBase = null
    }

    /** Sweeps runtime bitmaps down to exactly what the live document and undo/redo history need. */
    fun reconcileRuntimeAssets() = history.reconcileRuntimeAssets()

    fun checkpoint() = history.checkpoint()

    /** FxF edits may change project layers; never restore an old actor snapshot across workspaces. */
    fun clearHistory() = history.clearHistory()

    fun handle(intent: StudioIntent.Actor) {
        // The Performance Stage owns gesture data, not its actor revision. Keep this guard at
        // the intent boundary as well as in the UI so an unavailable command cannot mutate a
        // pinned rig through state restoration or accessibility actions.
        if (getState().actor.isPerformanceAuthoring && intent !is StudioIntent.Actor.Drag &&
            intent != StudioIntent.Actor.CommitDrag && intent != StudioIntent.Actor.CancelDrag &&
            intent != StudioIntent.Actor.ResetPose && intent != StudioIntent.Actor.Undo &&
            intent != StudioIntent.Actor.Redo
        ) {
            return
        }
        if (getState().scene.studioMode == StudioMode.RASTER_DRAWING && intent !is StudioIntent.Actor.MovePart) return
        when (intent) {
            is StudioIntent.Actor.SetEditTarget -> {
                artworkDragStart = null
                artworkDragBase = null
                updateState {
                    it.copy(
                        actor = it.actor.copy(editTarget = intent.target, previewGraph = null),
                        tool = it.tool.copy(
                            activeTool = DrawingTool.RIG_SELECT,
                            constructionShape = null,
                        ),
                    )
                }
            }

            is StudioIntent.Actor.AttachArtwork, is StudioIntent.Actor.SelectArtwork,
            is StudioIntent.Actor.UpdateArtwork, is StudioIntent.Actor.RemoveArtwork,
            is StudioIntent.Actor.SetAttachmentBlend, is StudioIntent.Actor.CommitDrawing,
            is StudioIntent.Actor.UseDrawing, is StudioIntent.Actor.SetDrawingOverlay,
            -> Unit

            // Dedicated artwork transaction manager.
            is StudioIntent.Actor.AddSampleArtwork -> Unit

            // Bitmap work belongs to ActorSampleArtwork.
            is StudioIntent.Actor.Create -> {
                checkpoint()
                updateState { state ->
                    val id = UUID.randomUUID().toString()
                    val definition =
                        PuppetDefinition(
                            id,
                            intent.template.title,
                            ActorTemplates.create(intent.template, state.project.stage),
                        )
                    val instance = PuppetInstance("instance-$id", id)
                    val layer = Layer(
                        intent.template.title,
                        id = "layer-$id",
                        type = LayerType.PUPPET,
                        zIndex = state.currentFrameLayers().size,
                        puppetInstanceId = instance.id,
                    )
                    val layers = state.currentFrameLayers() + layer
                    state.copy(
                        project = state.project.copy(
                            layers = layers,
                            puppetDefinitions = state.project.puppetDefinitions + (id to definition),
                            puppetInstances = state.project.puppetInstances + (instance.id to instance),
                        ),
                        layers = state.layers.copy(
                            layers = layers,
                            activeLayerIndex = layers.lastIndex,
                            frameLayers = state.layers.frameLayers + (state.animation.currentFrame to layers),
                        ),
                        scene = state.scene.copy(
                            studioMode = StudioMode.RIG_SETUP,
                            selectedBoneId = definition.sceneGraph.rootBoneIds.first(),
                        ),
                        actor = state.actor.copy(previewGraph = null),
                        tool = state.tool.copy(activeTool = DrawingTool.RIG_SELECT),
                    )
                }
            }

            is StudioIntent.Actor.Drag -> updateState { state ->
                val definition =
                    state.actorDefinition(state.scene.studioMode == StudioMode.ANIMATE_IK) ?: return@updateState state
                if (state.scene.studioMode == StudioMode.ANIMATE_IK &&
                    motionDragBase == null
                ) {
                    motionDragBase = definition.sceneGraph
                }
                val transform = state.activeActor()?.transform ?: return@updateState state
                if (transform.scaleX == 0f || transform.scaleY == 0f) return@updateState state
                val rotated = RigGeometry.rotate(
                    intent.target - Offset(transform.x, transform.y),
                    -transform.rotationDegrees,
                )
                val target = Offset(rotated.x / transform.scaleX, rotated.y / transform.scaleY)
                if (!target.x.isFinite() || !target.y.isFinite()) return@updateState state
                val graph = if (intent.id.startsWith("artwork:") && state.scene.studioMode == StudioMode.RIG_SETUP) {
                    if (artworkDragStart == null) {
                        artworkDragStart = target
                        artworkDragBase = definition.sceneGraph
                    }
                    ActorPoseEditor.moveArtwork(
                        artworkDragBase ?: definition.sceneGraph,
                        intent.id.removePrefix("artwork:"),
                        target - requireNotNull(artworkDragStart),
                    )
                } else if (intent.id.startsWith("attachment:") && state.scene.studioMode == StudioMode.RIG_SETUP) {
                    ActorJointHandle.moveAttachment(
                        definition.sceneGraph,
                        intent.id.removePrefix("attachment:"),
                        target,
                    )
                } else if (intent.id.startsWith("origin:") && state.scene.studioMode == StudioMode.RIG_SETUP) {
                    ActorPoseEditor.moveOrigin(definition.sceneGraph, intent.id.removePrefix("origin:"), target)
                } else if (intent.id.startsWith("origin:") && state.scene.studioMode != StudioMode.RASTER_DRAWING) {
                    ActorPoseEditor.moveRoot(
                        definition.sceneGraph,
                        intent.id.removePrefix("origin:"),
                        target,
                        allRoots = state.actor.takeMoveCharacter && state.actor.takePhase != PerformanceTakePhase.IDLE,
                    )
                } else if (state.scene.studioMode == StudioMode.ANIMATE_IK) {
                    ActorPoseEditor.pose(
                        definition.sceneGraph,
                        intent.id,
                        target,
                        ActorMotionPose.contactBone(
                            definition.sceneGraph,
                            intent.id,
                        )?.let { endpoint ->
                            val root = definition.sceneGraph.bones.getValue(endpoint).ikChainRootId!!
                            val world = RigGeometry.resolve(motionDragBase ?: definition.sceneGraph)
                            TwoBoneSolver.bendSide(
                                listOf(
                                    world.getValue(root).start,
                                    world.getValue(endpoint).start,
                                    world.getValue(endpoint).end,
                                ),
                            )
                        },
                    )
                } else {
                    ActorPoseEditor.fit(definition.sceneGraph, intent.id, target)
                }
                val contacts = ActorMotionPose.followBendSide(
                    graph,
                    ActorPoseTrack.contacts(state.activeActor()?.poseKeys.orEmpty(), state.actor.previewTimeMicros),
                )
                // Moving the pelvis keeps planted limbs anchored during the drag itself.
                val preview = if (state.scene.studioMode == StudioMode.ANIMATE_IK) {
                    ActorMotionPose.apply(
                        graph,
                        emptyMap(),
                        emptyMap(),
                        contacts,
                    )
                } else if (graph.drawing?.enabled == true) {
                    // Fit pins over unchanged source pixels. Updating inverse binds is O(bones);
                    // rebuild weights once on release, not for every touch event.
                    val drawing = requireNotNull(graph.drawing)
                    val world = ArtworkSkinning.worldTransforms(graph)
                    graph.copy(
                        drawing = drawing.withSkin(
                            drawing.skin.copy(
                                mesh = drawing.skin.mesh.copy(
                                    bones = drawing.skin.bones.map {
                                        it.copy(bindWorld = world.getValue(it.boneId))
                                    },
                                ),
                            ),
                        ),
                    )
                } else {
                    graph
                }
                state.copy(actor = state.actor.copy(previewGraph = preview, previewDefinitionId = definition.id))
            }

            StudioIntent.Actor.CommitDrag -> {
                artworkDragStart = null
                artworkDragBase = null
                if (getState().scene.studioMode == StudioMode.RIG_SETUP &&
                    getState().actor.previewGraph == getState().actorDefinition()?.sceneGraph
                ) {
                    updateState { it.copy(actor = it.actor.copy(previewGraph = null)) }
                    return
                }
                if (getState().actor.previewGraph != null) {
                    checkpoint()
                    updateState { state ->
                        val definition = state.actorDefinition() ?: return@updateState state
                        val preview = state.actor.previewGraph ?: return@updateState state
                        val graph = if (state.scene.studioMode == StudioMode.RIG_SETUP) {
                            ArtworkSkinRestBinding.rebase(preview)
                        } else {
                            preview
                        }
                        if (state.actor.previewDefinitionId !=
                            definition.id
                        ) {
                            return@updateState state.copy(actor = state.actor.copy(previewGraph = null))
                        }
                        val instance = state.activeActor() ?: return@updateState state
                        val project = if (state.scene.studioMode == StudioMode.ANIMATE_IK) {
                            state.project.copy(
                                puppetInstances = state.project.puppetInstances +
                                    (
                                        instance.id to
                                            instance.copy(
                                                pose = graph.bones.mapValues {
                                                    it.value.localTransform
                                                },
                                                rotationCurves =
                                                PerformanceAuthoring.refineCurves(
                                                    instance.rotationCurves,
                                                    ActorPoseTrack.capture(
                                                        state.actor.previewTimeMicros,
                                                        graph,
                                                        definition.sceneGraph,
                                                    ).let { key ->
                                                        key.copy(
                                                            rotations = key.rotations.filter { (id, angle) ->
                                                                motionDragBase?.bones?.get(
                                                                    id,
                                                                )?.localTransform?.rotationDegrees?.let {
                                                                    kotlin.math.abs(
                                                                        CompiledMotionCurve.shortestAngle(
                                                                            angle - it,
                                                                        ),
                                                                    ) >
                                                                        .001f
                                                                } ?: true
                                                            },
                                                        )
                                                    },
                                                ),
                                                rootMotionCurves = PerformanceAuthoring.refineRoots(
                                                    instance.rootMotionCurves,
                                                    ActorPoseTrack.capture(
                                                        state.actor.previewTimeMicros,
                                                        graph,
                                                        definition.sceneGraph,
                                                    ),
                                                ),
                                                poseKeys = ActorPoseTrack.put(
                                                    instance.poseKeys,
                                                    ActorPoseTrack.capture(
                                                        state.actor.previewTimeMicros,
                                                        graph,
                                                        definition.sceneGraph,
                                                        ActorMotionPose.followBendSide(
                                                            graph,
                                                            ActorPoseTrack.contacts(
                                                                instance.poseKeys,
                                                                state.actor.previewTimeMicros,
                                                            ),
                                                        ),
                                                    ),
                                                ),
                                            )
                                        ),
                            )
                        } else {
                            state.project.copy(
                                puppetDefinitions = state.project.puppetDefinitions +
                                    (definition.id to definition.copy(sceneGraph = graph)),
                            )
                        }
                        state.copy(project = project, actor = state.actor.copy(previewGraph = null))
                    }
                }
                motionDragBase = null
            }

            StudioIntent.Actor.CancelDrag -> {
                motionDragBase = null
                artworkDragStart = null
                artworkDragBase = null
                updateState { it.copy(actor = it.actor.copy(previewGraph = null)) }
            }

            StudioIntent.Actor.ToggleSource -> updateState {
                it.copy(actor = it.actor.copy(showSource = !it.actor.showSource))
            }

            StudioIntent.Actor.ResetPose -> {
                val instance = getState().activeActor() ?: return
                checkpoint()
                updateState { state ->
                    val rest = state.actorDefinition()?.sceneGraph ?: return@updateState state
                    state.copy(
                        project = state.project.copy(
                            puppetInstances = state.project.puppetInstances +
                                (
                                    instance.id to
                                        instance.copy(
                                            pose = emptyMap(),
                                            rotationCurves = emptyList(),
                                            rootMotionCurves = emptyList(),
                                            poseKeys = ActorPoseTrack.put(
                                                instance.poseKeys,
                                                ActorPoseTrack.capture(state.actor.previewTimeMicros, rest, rest),
                                            ),
                                        )
                                    ),
                        ),
                        actor = state.actor.copy(previewGraph = null),
                    )
                }
            }

            StudioIntent.Actor.Undo -> history.undo()

            StudioIntent.Actor.Redo -> history.redo()

            // Display names are editable; stable semantic IDs remain untouched.
            is StudioIntent.Actor.RenameBone -> {
                val name = intent.name.trim()
                if (name.isNotEmpty() && name.length <= 60) editSelected { it.withHierarchy(name = name) }
            }

            is StudioIntent.Actor.RenameActor -> renameActor(intent.name)

            StudioIntent.Actor.Unbind -> unbindSelected()

            is StudioIntent.Actor.SetPartVisibility -> setPartVisibility(intent.boneId, intent.visible)

            is StudioIntent.Actor.MovePart -> {
                val definition = getState().actorDefinition() ?: return
                val graph = ActorPartStack.move(
                    definition.sceneGraph,
                    intent.boneId,
                    intent.forward,
                )
                if (graph == definition.sceneGraph) return
                checkpoint()
                updateState {
                    it.copy(
                        project = it.project.copy(
                            puppetDefinitions = it.project.puppetDefinitions +
                                (definition.id to definition.copy(sceneGraph = graph)),
                        ),
                    )
                }
            }
        }
    }

    private fun setPartVisibility(boneId: String, visible: Boolean) {
        val state = getState()
        val definition = state.actorDefinition() ?: return
        val bone = definition.sceneGraph.bones[boneId] ?: return
        if (bone.visible == visible) return
        checkpoint()
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetDefinitions = it.project.puppetDefinitions +
                        (
                            definition.id to definition.copy(
                                sceneGraph = definition.sceneGraph.copy(
                                    bones =
                                    definition.sceneGraph.bones + (boneId to bone.withDisplay(visible = visible)),
                                ),
                            )
                            ),
                ),
            )
        }
    }

    private fun renameActor(name: String) {
        val state = getState()
        val definition = state.actorDefinition() ?: return
        checkpoint()
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetDefinitions = it.project.puppetDefinitions +
                        (definition.id to definition.copy(name = name.trim().take(60).ifEmpty { definition.name })),
                ),
            )
        }
    }

    private fun editSelected(
        edit: (
            BoneNode,
        ) -> BoneNode,
    ) {
        val state = getState()
        val definition = state.actorDefinition() ?: return
        val bone = definition.sceneGraph.bones[state.scene.selectedBoneId] ?: return
        checkpoint()
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetDefinitions = it.project.puppetDefinitions +
                        (
                            definition.id to
                                definition.copy(
                                    sceneGraph = definition.sceneGraph.copy(
                                        bones =
                                        definition.sceneGraph.bones + (bone.id to edit(bone)),
                                    ),
                                )
                            ),
                ),
            )
        }
    }

    private fun unbindSelected() {
        val state = getState()
        val definition = state.actorDefinition() ?: return
        val boneId = state.scene.selectedBoneId ?: return
        if (definition.sceneGraph.bones[boneId]?.textureBinding == null) return
        checkpoint()
        updateState { current ->
            val mutation = current.project.unbindTextureFromBone(definition.id, boneId)
            current.copy(project = mutation.document)
        }
        // The just-created undo snapshot still references the detached asset, so a scalar
        // release here would recycle it out from under that snapshot; reconcile instead.
        reconcileRuntimeAssets()
    }
}
