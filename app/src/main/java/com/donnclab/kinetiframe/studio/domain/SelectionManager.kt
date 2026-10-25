package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry
import com.donnclab.kinetiframe.core.document.BaseNode
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.core.geometry.Affine2D
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
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val frameOperationMutex: Mutex,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val rebuildCurrentBitmap: suspend () -> Unit,
    private val snapshotActiveLayer: suspend () -> Bitmap?,
    private val setBaseBitmap: (Bitmap?) -> Unit,
    private val history: MutableList<HistoryItem>,
    private val redoHistory: MutableList<HistoryItem> = mutableListOf(),
    private val trimHistory: suspend () -> Unit = {},
    private val pendingOperations: PendingOperations = PendingOperations(),
) {
    private data class Clipboard(val content: SelectedContent, val transform: Matrix, val projectId: String)
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
        path: Path?,
        rect: Rect?,
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
                    pendingClear = HistoryItem.ClearArea(path?.let { Path().apply { addPath(it) } }, rect)
                    history.add(requireNotNull(pendingClear))
                    rebuildCurrentBitmap()
                }
                val actorBounds = actorHits.flatMap { it.quads.flatten() }.let(::pointsBounds)
                val videoBounds = selectedVideos.values.flatMap {
                    com.donnclab.kinetiframe.media.video.SceneVideoGeometry.corners(it)
                }.let(::pointsBounds)
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

    private fun expose(content: SelectedContent, matrix: Matrix = Matrix()) {
        updateState { state ->
            state.copy(
                selection = state.selection.copy(
                    selectedContent = content,
                    active = true,
                    bounds = content.bounds,
                    transform = matrix,
                    pivotOffset = Offset.Zero,
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
        val affine = selection.transform.toAffine()
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
                        Matrix(selection.transform.values.copyOf()),
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
                Clipboard(it, Matrix(state.selection.transform.values.copyOf()), state.project.id)
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
                val matrix = Matrix(saved.transform.values.copyOf()).apply {
                    values[12] += 20f
                    values[13] += 20f
                }
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
                val bounds = Rect(left, top, left + bitmap.width, top + bitmap.height)
                val matrix = Matrix().apply {
                    translate(bounds.center.x, bounds.center.y)
                    scale(scaleToFit, scaleToFit)
                    translate(-bounds.center.x, -bounds.center.y)
                }
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
        pointsBounds(nodes.values.filterIsInstance<TextNode>().flatMap { corners(it, density, fonts) })

    private fun corners(node: TextNode, density: Float, fonts: FontRegistry?): List<Offset> {
        val typeface =
            fonts?.availableFonts?.find { it.fileName == node.fontFileName }?.typeface
                ?: android.graphics.Typeface.DEFAULT
        val bounds = MultilineTextRenderer.getBounds(node, typeface, density)
        val x = bounds.width * node.scaleX / 2f
        val y = bounds.height * node.scaleY / 2f
        return listOf(Offset(-x, -y), Offset(x, -y), Offset(x, y), Offset(-x, y)).map {
            val point = rotateVector(it, node.rotation) + Offset(node.x, node.y)
            Offset(node.selectionTransform.mapX(point.x, point.y), node.selectionTransform.mapY(point.x, point.y))
        }
    }
    private fun pointsBounds(points: List<Offset>): Rect? = points.takeIf { it.isNotEmpty() }?.let {
        Rect(it.minOf(Offset::x), it.minOf(Offset::y), it.maxOf(Offset::x), it.maxOf(Offset::y))
    }
    private fun union(a: Rect?, b: Rect?): Rect = when {
        a == null || a == Rect.Zero -> b ?: Rect.Zero
        b == null -> a
        else -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
    }
}
private fun Matrix.toAffine() = values.let { Affine2D(it[0], it[1], it[4], it[5], it[12], it[13]) }
