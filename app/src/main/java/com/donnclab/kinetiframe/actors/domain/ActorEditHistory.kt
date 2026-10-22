package com.donnclab.kinetiframe.actors.domain

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.math.ActorPoseEditor
import com.donnclab.kinetiframe.actors.rig.math.ArtworkSkinRestBinding
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseKey
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.project.model.LayerType
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.project.model.PuppetDefinition
import com.donnclab.kinetiframe.project.model.PuppetInstance
import com.donnclab.kinetiframe.project.model.referencedAssetIds
import com.donnclab.kinetiframe.project.model.unbindTextureFromBone
import com.donnclab.kinetiframe.studio.LayerState
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.currentFrameLayers
import java.util.UUID

/**
 * Undo and redo for actor edits. History holds immutable document snapshots, never copies of
 * bitmaps.
 *
 * Runtime bitmap lifetime is decoupled from any single mutation: detaching or rebinding a texture
 * can drop the live document's only reference to it while an undo snapshot still needs it. Rather
 * than releasing on the spot, every mutation that could shrink the set of referenced assets calls
 * [reconcileRuntimeAssets], which recomputes the full reachable set (the live document plus every
 * undo and redo snapshot) and asks [retainRuntimeAssets] to sweep anything outside it. A bitmap a
 * snapshot still needs therefore survives however many times a live edit unbinds it.
 */
internal class ActorEditHistory(
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val retainRuntimeAssets: (Set<String>) -> Unit,
) {
    private data class Snapshot(
        val project: ProjectDocument,
        val layers: LayerState,
        val selected: String?,
        val durationMicros: Long,
    )
    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    private fun snapshot() = getState().let {
        // Temporary extraction metadata must not reappear as an orphan when binding is undone.
        val project =
            it.scene.pivotPlacement?.let { pending -> it.project.copy(assets = it.project.assets - pending.assetId) }
                ?: it.project
        Snapshot(project, it.layers, it.scene.selectedBoneId, it.actor.performanceDurationMicros)
    }

    /**
     * Every asset ID reachable right now: the live document, a texture mid-extraction that is
     * not yet bound to any bone ([com.donnclab.kinetiframe.studio.SceneState.pivotPlacement] -- it exists only
     * in the live scene, never inside a snapshot), and every undo and redo snapshot. Nothing
     * outside this set can possibly be rendered, confirmed or restored, so it is always safe for
     * [retainRuntimeAssets] to recycle anything else.
     */
    private fun reachableAssetIds(): Set<String> = (
        getState().project.referencedAssetIds() +
            setOfNotNull(getState().scene.pivotPlacement?.assetId) +
            undoStack.flatMap { it.project.referencedAssetIds() } +
            redoStack.flatMap { it.project.referencedAssetIds() }
        ).toSet()

    /** Sweeps runtime bitmaps down to exactly what the live document and undo/redo history need. */
    fun reconcileRuntimeAssets() {
        retainRuntimeAssets(reachableAssetIds())
    }

    fun checkpoint() {
        undoStack.addLast(snapshot())
        if (undoStack.size > 32) undoStack.removeFirst()
        redoStack.clear()
        updateState { it.copy(actor = it.actor.copy(canUndo = true, canRedo = false)) }
        // No reconcile here: a caller can checkpoint before it folds a pending extraction into the
        // document (confirming a pivot checkpoints first and binds second). Reconciling then would
        // sweep a bitmap that is about to become referenced. Evicting old history only shrinks what
        // is reachable, so the next unbind, rebind, undo, redo or clearHistory sweep catches it.
    }

    /** FxF edits may change project layers; never restore an old actor snapshot across workspaces. */
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        updateState { it.copy(actor = it.actor.copy(canUndo = false, canRedo = false, previewGraph = null)) }
        reconcileRuntimeAssets()
    }

    fun undo() = restore(undoStack, redoStack)

    fun redo() = restore(redoStack, undoStack)

    private fun restore(from: ArrayDeque<Snapshot>, to: ArrayDeque<Snapshot>) {
        if (from.isEmpty()) return
        to.addLast(snapshot())
        val saved = from.removeLast()
        updateState {
            it.copy(
                project = saved.project,
                layers = saved.layers,
                scene = it.scene.copy(selectedBoneId = saved.selected),
                actor = it.actor.copy(
                    previewGraph = null,
                    performanceDurationMicros = saved.durationMicros,
                    previewTimeMicros = it.actor.previewTimeMicros.coerceAtMost(saved.durationMicros),
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                ),
            )
        }
        reconcileRuntimeAssets()
    }
}
