package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry
import com.donnclab.kinetiframe.core.document.BaseNode
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.core.geometry.Affine2D
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.core.geometry.Rect2
import com.donnclab.kinetiframe.studio.SelectedContent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.drawing.FontRegistry
import com.donnclab.kinetiframe.studio.drawing.MultilineTextRenderer
import com.donnclab.kinetiframe.studio.drawing.rotateVector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Floating pixels/nodes; actors remain live project references, never flattened. */
class SelectionManager(
    runtime: StudioRuntime,
    private val frameOperationMutex: Mutex,
    hooks: SelectionHooks,
    undo: UndoStacks,
) {
    private val scope: CoroutineScope = runtime.scope
    private val dispatcher: CoroutineDispatcher = runtime.dispatcher
    private val getState: () -> StudioState = runtime.getState
    private val updateState: ((StudioState) -> StudioState) -> Unit = runtime.updateState
    private val pendingOperations: PendingOperations = runtime.pending
    private val rebuildCurrentBitmap: suspend () -> Unit = hooks.rebuildCurrentBitmap
    private val snapshotActiveLayer: suspend () -> Bitmap? = hooks.snapshotActiveLayer
    private val setBaseBitmap: (Bitmap?) -> Unit = hooks.setBaseBitmap
    private val trimHistory: suspend () -> Unit = hooks.trimHistory
    private val history: MutableList<HistoryItem> = undo.history
    private val redoHistory: MutableList<HistoryItem> = undo.redo

    private data class Clipboard(val content: SelectedContent, val transform: Affine2D, val projectId: String)
    private var clipboard: Clipboard? = null
    private var pendingClear: HistoryItem.ClearArea? = null

    fun selectVideo(id: String) = pendingOperations.launch(scope, dispatcher) {
        frameOperationMutex.withLock {
            finish(commit = true)
            val clip = getState().project.videoClips[id]?.takeIf { it.isSceneContent } ?: return@withLock
            if (getState().animation.currentFrame !in
                clip.timelineStartFrame until clip.timelineEndFrame
            ) {
                return@withLock
            }
            expose(
                SelectedContent(
                    videos = mapOf(id to clip),
                    audio = linkedSound(listOf(clip)),
                    bounds = com.donnclab.kinetiframe.media.video.SceneVideoGeometry.bounds(clip),
                ),
            )
            updateState {
                it.copy(
                    tool = it.tool.copy(
                        activeTool = com.donnclab.kinetiframe.core.document.DrawingTool.LASSO,
                        constructionShape = null,
                    ),
                )
            }
        }
    }

    fun selectTextNode(id: String, density: Float, fontRegistry: FontRegistry?) =
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                finish(commit = true)
                val node = getState().scene.nodes[id] as? TextNode ?: return@withLock
                if (!node.isVisible) return@withLock
                val nodes = mapOf(id to node)
                expose(
                    SelectedContent(
                        nodes = nodes,
                        bounds = nodesBounds(nodes, density, fontRegistry) ?: return@withLock,
                    ),
                )
            }
        }

    fun selectArea(
        path: android.graphics.Path?,
        rect: Rect2?,
        density: Float,
        fontRegistry: FontRegistry? = null,
        actors: ActorSelectionGeometry? = null,
    ): kotlinx.coroutines.Job {
        val requested = getState()
        val requestedLayer = requested.currentFrameLayers().getOrNull(requested.layers.activeLayerIndex)?.id
        return pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val current = getState()
                if (current.project.id != requested.project.id ||
                    current.animation.currentFrame != requested.animation.currentFrame ||
                    current.currentFrameLayers().getOrNull(current.layers.activeLayerIndex)?.id != requestedLayer
                ) {
                    return@withLock
                }
                finish(commit = true)
                val state = getState()
                val area = SelectionAreaMask(path, rect) ?: return@withLock
                var actorHits = actors?.takeIf {
                    it.projectId == state.project.id &&
                        it.frame == state.animation.currentFrame
                }
                    ?.objects?.filter { geometry ->
                        state.project.actorPlacements[geometry.placement.placementId] == geometry.placement &&
                            area.matches(geometry.quads, state.selection.partialInclude)
                    }.orEmpty()
                if (area.isTap) actorHits = actorHits.maxByOrNull { it.placement.zIndex }?.let(::listOf).orEmpty()
                val selectedActors = actorHits.associate { it.placement.placementId to it.placement }
                val selectedVideos = state.project.videoClips.filterValues { clip ->
                    !(
                        area.isTap &&
                            selectedActors.isNotEmpty()
                        ) &&
                        clip.isSceneContent &&
                        state.animation.currentFrame in clip.timelineStartFrame until clip.timelineEndFrame &&
                        area.matches(
                            listOf(com.donnclab.kinetiframe.media.video.SceneVideoGeometry.corners(clip)),
                            state.selection.partialInclude,
                        )
                }
                var nodes = state.scene.nodes.filterValues { node ->
                    node is TextNode && node.isVisible &&
                        area.matches(listOf(corners(node, density, fontRegistry)), state.selection.partialInclude)
                }
                if (area.isTap) {
                    nodes = if (selectedActors.isNotEmpty() || selectedVideos.isNotEmpty()) {
                        emptyMap()
                    } else {
                        nodes.maxByOrNull { it.value.zIndex }?.let { mapOf(it.key to it.value) }.orEmpty()
                    }
                }
                // Whole objects take precedence: moving an actor must not punch a hole
                // through the painted background. Pixel masks edit the active layer.
                val layer = state.currentFrameLayers().getOrNull(state.layers.activeLayerIndex)
                val editable = layer == null || (layer.isVisible && !layer.isLocked && layer.opacity > 0f)
                val source = if (editable && selectedActors.isEmpty() && selectedVideos.isEmpty() &&
                    nodes.isEmpty()
                ) {
                    rebuildCurrentBitmap()
                    snapshotActiveLayer()
                } else {
                    null
                }
                val raster = source?.let { SelectionRaster.extract(it, path, rect) }
                source?.recycle() // Private snapshot, never the cache's displayed frame.
                if (raster == null && nodes.isEmpty() && selectedActors.isEmpty() &&
                    selectedVideos.isEmpty()
                ) {
                    return@withLock
                }
                if (raster != null) {
                    // Clear through history only. Clearing baseBitmap too destroys undo.
                    pendingClear = HistoryItem.ClearArea(path?.let { android.graphics.Path(it) }, rect)
                    history.add(requireNotNull(pendingClear))
                    rebuildCurrentBitmap()
                }
                val actorBounds = Rect2.around(actorHits.flatMap { it.quads.flatten() })
                val videoBounds = selectedVideos.values.flatMap {
                    com.donnclab.kinetiframe.media.video.SceneVideoGeometry.corners(it)
                }.let(Rect2::around)
                val bounds =
                    union(
                        union(union(raster?.bounds, nodesBounds(nodes, density, fontRegistry)), actorBounds),
                        videoBounds,
                    )
                expose(
                    SelectedContent(
                        nodes = nodes,
                        actors = selectedActors,
                        videos = selectedVideos,
                        audio = linkedSound(selectedVideos.values),
                        bitmap = raster?.bitmap,
                        bounds = bounds,
                        isRasterSelection =
                        raster != null,
                    ),
                )
            }
        }
    }

    private fun expose(content: SelectedContent, matrix: Affine2D = Affine2D()) {
        updateState { state ->
            state.copy(
                selection = state.selection.copy(
                    selectedContent = content,
                    active = true,
                    bounds = content.bounds,
                    transform = matrix,
                    pivotOffset = Point2.Zero,
                    contextMenuVisible = false,
                ),
                scene = state.scene.copy(nodes = state.scene.nodes - content.nodes.keys, activeNodeId = null),
                project = if (content.isPaste) {
                    state.project.copy(
                        actorPlacements =
                        state.project.actorPlacements + content.actors,
                        videoClips = state.project.videoClips + content.videos,
                        audioClips =
                        state.project.audioClips + content.audio,
                    )
                } else {
                    state.project
                },
            )
        }
    }

    fun commitSelection() = pendingOperations.launch(scope, dispatcher) {
        frameOperationMutex.withLock { finish(commit = true) }
    }
    fun cancelSelection() = pendingOperations.launch(scope, dispatcher) {
        frameOperationMutex.withLock { finish(commit = false) }
    }
    fun deleteSelection() = pendingOperations.launch(scope, dispatcher) {
        frameOperationMutex.withLock { finish(commit = true, delete = true) }
    }

    private suspend fun finish(commit: Boolean, delete: Boolean = false) {
        val selection = getState().selection
        val content = selection.selectedContent ?: return
        if (delete &&
            content.audio.values.any { getState().project.audioTracks[it.trackIndex]?.isLocked == true }
        ) {
            return
        }
        val affine = selection.transform
        val beforeActors = if (content.isPaste) emptyMap() else content.actors
        val beforeNodes = if (content.isPaste) emptyMap() else content.nodes
        val beforeVideos = if (content.isPaste) emptyMap() else content.videos
        val currentAudio = content.audio.mapValues { (id, original) -> getState().project.audioClips[id] ?: original }
        val beforeAudio = if (content.isPaste) emptyMap() else currentAudio
        val afterAudio = if (!commit) {
            beforeAudio
        } else if (delete) {
            emptyMap()
        } else {
            currentAudio
        }
        val afterVideos = if (!commit) {
            beforeVideos
        } else if (delete) {
            emptyMap()
        } else {
            content.videos.mapValues { (_, clip) ->
                clip.copy(selectionTransform = affine * clip.selectionTransform)
            }
        }
        val afterActors = if (!commit) {
            beforeActors
        } else if (delete) {
            emptyMap()
        } else {
            content.actors.mapValues { (_, actor) ->
                actor.copy(selectionTransform = affine * actor.selectionTransform)
            }
        }
        val afterNodes = if (!commit) {
            beforeNodes
        } else if (delete) {
            emptyMap()
        } else {
            content.nodes.mapValues { (_, node) ->
                if (node is TextNode) node.copy(selectionTransform = affine * node.selectionTransform) else node
            }
        }
        if (!commit) pendingClear?.let { history.remove(it) }
        if (commit) {
            redoHistory.clear()
            if (content.isRasterSelection) {
                pendingClear?.let { history.remove(it) }
                val stamp = if (delete) {
                    null
                } else {
                    HistoryItem.SelectionEdit(
                        content.bitmap,
                        emptyMap(),
                        selection.transform,
                        content.bounds,
                        SelectionEditType.COMMIT,
                    )
                }
                if (pendingClear != null || stamp != null) history.add(HistoryItem.RasterSelection(pendingClear, stamp))
            }
            if (beforeActors != afterActors || beforeNodes != afterNodes || beforeVideos != afterVideos) {
                history.add(
                    HistoryItem.ObjectEdit(
                        beforeActors,
                        afterActors,
                        beforeNodes,
                        afterNodes,
                        beforeVideos,
                        afterVideos,
                        beforeAudio,
                        afterAudio,
                    ),
                )
            }
            trimHistory()
        }
        pendingClear = null
        updateState { state ->
            state.copy(
                selection = state.selection.copy(
                    selectedContent = null,
                    active = false,
                    bounds = null,
                    contextMenuVisible = false,
                ),
                scene = state.scene.copy(
                    nodes = (state.scene.nodes - content.nodes.keys) + afterNodes,
                    activeNodeId = null,
                ),
                project = state.project.copy(
                    actorPlacements =
                    (state.project.actorPlacements - content.actors.keys) + afterActors,
                    videoClips = (state.project.videoClips - content.videos.keys) + afterVideos,
                    audioClips = (state.project.audioClips - content.audio.keys) + afterAudio,
                ),
            )
        }
        rebuildCurrentBitmap()
    }

    fun cutSelection() {
        copySelection()
        deleteSelection()
    }
    fun copySelection() {
        val state = getState()
        state.selection.selectedContent?.let {
            clipboard =
                Clipboard(it, state.selection.transform, state.project.id)
        }
    }
    fun hasClipboardContent() = clipboard != null
    fun pasteSelection() {
        val saved = clipboard ?: return
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                finish(commit = true)
                if (saved.projectId != getState().project.id && saved.content.nodes.isEmpty() &&
                    saved.content.bitmap == null
                ) {
                    return@withLock
                }
                val nodes = saved.content.nodes.values.associate { node ->
                    val id = UUID.randomUUID().toString()
                    id to node.copyWithId(id)
                }
                val actors = if (saved.projectId == getState().project.id) {
                    saved.content.actors.values.associate { actor ->
                        val id = UUID.randomUUID().toString()
                        id to actor.copy(placementId = id, zIndex = actor.zIndex + 1)
                    }
                } else {
                    emptyMap() // Performances are project-scoped, not cross-project raw pixels.
                }
                // In place, not offset: animators paste to carry a drawing to another frame unmoved (R3-22).
                val matrix = saved.transform
                // One video lane: copies append sequentially rather than introducing overlapping decoders.
                var nextVideoFrame = getState().project.videoClips.values.maxOfOrNull { it.timelineEndFrame } ?: 0
                val audioCopies = mutableMapOf<String, com.donnclab.kinetiframe.core.document.AudioClipPlacement>()
                val videos = if (saved.projectId == getState().project.id) {
                    saved.content.videos.values.associate { clip ->
                        val id = UUID.randomUUID().toString()
                        val start = nextVideoFrame
                        nextVideoFrame += clip.timelineEndFrame - clip.timelineStartFrame
                        val linked = clip.linkedAudioClipId?.let { saved.content.audio[it] }
                        val audioId = linked?.let { UUID.randomUUID().toString() }
                        val copied = clip.copy(
                            clipId = id,
                            timelineStartFrame = start,
                            timelineEndFrame = nextVideoFrame,
                            linkedAudioClipId = audioId,
                        )
                        if (linked != null &&
                            audioId != null
                        ) {
                            audioCopies[audioId] =
                                com.donnclab.kinetiframe.media.video.VideoAudioLink.synchronize(
                                    copied,
                                    linked.copy(clipId = audioId),
                                    getState().project.fps,
                                )
                        }
                        id to copied
                    }
                } else {
                    emptyMap()
                }
                updateState {
                    it.copy(
                        animation = it.animation.copy(totalFrames = maxOf(it.animation.totalFrames, nextVideoFrame)),
                    )
                }
                expose(
                    saved.content.copy(
                        nodes = nodes,
                        actors = actors,
                        videos = videos,
                        audio = audioCopies,
                        isPaste = true,
                    ),
                    matrix,
                )
            }
        }
    }

    private fun linkedSound(clips: Collection<com.donnclab.kinetiframe.core.document.VideoClipPlacement>) =
        clips.mapNotNull {
            it.linkedAudioClipId
        }.mapNotNull { getState().project.audioClips[it] }.associateBy { it.clipId }

    fun pasteBitmap(
        bitmap: Bitmap,
        stageWidth: Int,
        stageHeight: Int,
        dropCenterX: Float? = null,
        dropCenterY: Float? = null,
    ) {
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                finish(commit = true)
                val scaleToFit = minOf(1f, stageWidth.toFloat() / bitmap.width, stageHeight.toFloat() / bitmap.height)
                val left = (dropCenterX ?: stageWidth / 2f) - bitmap.width / 2f
                val top = (dropCenterY ?: stageHeight / 2f) - bitmap.height / 2f
                val bounds = Rect2(left, top, left + bitmap.width, top + bitmap.height)
                // Scale about the image's centre.
                val center = bounds.center
                val matrix = Affine2D(
                    a = scaleToFit,
                    d = scaleToFit,
                    tx = center.x * (1f - scaleToFit),
                    ty = center.y * (1f - scaleToFit),
                )
                expose(
                    SelectedContent(bitmap = bitmap, bounds = bounds, isRasterSelection = true, isPaste = true),
                    matrix,
                )
                // A pasted image must enter transform mode even if the eraser/brush was selected.
                updateState {
                    it.copy(
                        tool = it.tool.copy(
                            activeTool = com.donnclab.kinetiframe.core.document.DrawingTool.LASSO,
                            constructionShape = null,
                        ),
                    )
                }
            }
        }
    }

    private fun nodesBounds(nodes: Map<String, BaseNode>, density: Float, fonts: FontRegistry?) =
        Rect2.around(nodes.values.filterIsInstance<TextNode>().flatMap { corners(it, density, fonts) })

    private fun corners(node: TextNode, density: Float, fonts: FontRegistry?): List<Point2> {
        val typeface =
            fonts?.availableFonts?.find { it.fileName == node.fontFileName }?.typeface
                ?: android.graphics.Typeface.DEFAULT
        val bounds = MultilineTextRenderer.getBounds(node, typeface, density)
        val x = bounds.width * node.scaleX / 2f
        val y = bounds.height * node.scaleY / 2f
        return listOf(Point2(-x, -y), Point2(x, -y), Point2(x, y), Point2(-x, y)).map {
            val point = rotateVector(it, node.rotation) + Point2(node.x, node.y)
            Point2(node.selectionTransform.mapX(point.x, point.y), node.selectionTransform.mapY(point.x, point.y))
        }
    }
    private fun union(a: Rect2?, b: Rect2?): Rect2 = when {
        a == null || a == Rect2.Zero -> b ?: Rect2.Zero
        b == null -> a
        else -> a.union(b)
    }
}
