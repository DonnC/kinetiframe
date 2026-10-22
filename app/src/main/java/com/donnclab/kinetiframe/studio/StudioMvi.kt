package com.donnclab.kinetiframe.studio

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.donnclab.kinetiframe.project.model.DrawCommand
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.studio.drawing.BrushId
import com.donnclab.kinetiframe.studio.drawing.BrushSnapshot
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.project.model.BaseNode
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.project.model.LayerType
import com.donnclab.kinetiframe.project.model.TextNode
import com.donnclab.kinetiframe.actors.rig.model.PivotPlacement
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState

/** Legacy names kept for older callers; the editor now uses a continuous slider. */
enum class GridSize(val sizeDp: Int) {
    SMALL(20), MEDIUM(50), LARGE(100)
}

enum class RulerMode { NONE, LINE, CIRCLE, BOX, MIRROR }

enum class EraserType { AREA, STROKE }

enum class DockEdge { LEFT, RIGHT, TOP, BOTTOM }

enum class SelectionToolType { LASSO, RECTANGLE }

/** Canvas interaction is exclusive: raster paint, puppet setup, or IK posing. */
enum class StudioMode { RASTER_DRAWING, RIG_SETUP, ANIMATE_IK }

data class SelectedContent(
    val strokes: List<DrawCommand> = emptyList(),
    val nodes: Map<String, BaseNode> = emptyMap(),
    val bitmap: Bitmap? = null,
    val bounds: Rect = Rect.Zero,
    /** True when [bitmap] was cut from the rendered layer rather than reconstructed from commands. */
    val isRasterSelection: Boolean = false,
    val actors: Map<String, com.donnclab.kinetiframe.project.model.ActorPlacementReference> = emptyMap(),
    val videos: Map<String, com.donnclab.kinetiframe.project.model.VideoClipPlacement> = emptyMap(),
    val isPaste: Boolean = false,
    /** Linked sound follows selected video through copy/delete/undo, without being drawable. */
    val audio: Map<String, com.donnclab.kinetiframe.project.model.AudioClipPlacement> = emptyMap()
)

data class RulerGuide(
    val mode: RulerMode,
    val start: Offset = Offset(420f, 540f),
    val end: Offset = Offset(1500f, 540f),
    val center: Offset = Offset(960f, 540f),
    val radius: Offset = Offset(360f, 240f),
    val rotation: Float = 0f,
    val locked: Boolean = false
)

data class TextToolOptions(
    val fontFileName: String = "inter_regular.ttf",
    val fontSizeSp: Float = 36f,
    val textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    val alignment: TextNode.TextAlignment = TextNode.TextAlignment.CENTER
)

data class FrameTransform(
    val matrixValues: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrameTransform) return false
        return matrixValues.contentEquals(other.matrixValues)
    }

    override fun hashCode(): Int {
        return matrixValues.contentHashCode()
    }
}

data class CanvasState(
    val matrixValues: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    ),
    val zoom: Float = 1f,
    val pan: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0,
    val showSafeFrame: Boolean = false,
    /** Editor-only inspection aid. Saved scenes and exports still include the background. */
    val backgroundVisible: Boolean = true,
    val gridEnabled: Boolean = GridDefaults.ENABLED,
    val gridOpacity: Float = GridDefaults.OPACITY,
    val gridSpacingVertical: Float = GridDefaults.SPACING,
    val gridSpacingHorizontal: Float = GridDefaults.SPACING,
    val gridSize: GridSize = GridSize.MEDIUM,
    val gridSpacingDp: Float = GridSize.MEDIUM.sizeDp.toFloat(),
    val rulerMode: RulerMode = RulerMode.NONE,
    val rulerGuide: RulerGuide? = null,
    val dockEdge: DockEdge = DockEdge.LEFT,
    val dockOffset: Offset = Offset(16f, 100f),
    val selectionDockOffset: Offset = Offset(1100f, 300f)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CanvasState) return false
        if (!matrixValues.contentEquals(other.matrixValues)) return false
        if (zoom != other.zoom) return false
        if (pan != other.pan) return false
        if (rotation != other.rotation) return false
        if (viewportWidth != other.viewportWidth) return false
        if (viewportHeight != other.viewportHeight) return false
        if (showSafeFrame != other.showSafeFrame) return false
        if (backgroundVisible != other.backgroundVisible) return false
        if (gridEnabled != other.gridEnabled) return false
        if (gridOpacity != other.gridOpacity) return false
        if (gridSpacingVertical != other.gridSpacingVertical) return false
        if (gridSpacingHorizontal != other.gridSpacingHorizontal) return false
        if (gridSize != other.gridSize) return false
        if (gridSpacingDp != other.gridSpacingDp) return false
        if (rulerMode != other.rulerMode) return false
        if (rulerGuide != other.rulerGuide) return false
        if (dockEdge != other.dockEdge) return false
        if (dockOffset != other.dockOffset) return false
        if (selectionDockOffset != other.selectionDockOffset) return false
        return true
    }

    override fun hashCode(): Int {
        var result = matrixValues.contentHashCode()
        result = 31 * result + zoom.hashCode()
        result = 31 * result + pan.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + viewportWidth.hashCode()
        result = 31 * result + viewportHeight.hashCode()
        result = 31 * result + showSafeFrame.hashCode()
        result = 31 * result + backgroundVisible.hashCode()
        result = 31 * result + gridEnabled.hashCode()
        result = 31 * result + gridOpacity.hashCode()
        result = 31 * result + gridSpacingVertical.hashCode()
        result = 31 * result + gridSpacingHorizontal.hashCode()
        result = 31 * result + gridSize.hashCode()
        result = 31 * result + gridSpacingDp.hashCode()
        result = 31 * result + rulerMode.hashCode()
        result = 31 * result + (rulerGuide?.hashCode() ?: 0)
        result = 31 * result + dockEdge.hashCode()
        result = 31 * result + dockOffset.hashCode()
        result = 31 * result + selectionDockOffset.hashCode()
        return result
    }
}

data class ToolConfig(
    val size: Float = 5f,
    val opacity: Float = 1f,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    val strength: Float = 0.5f,
    val eraserType: EraserType = EraserType.AREA,
    val brushId: BrushId = BrushId.PEN
)

data class ToolState(
    val activeTool: DrawingTool = DrawingTool.BRUSH,
    val configs: Map<DrawingTool, ToolConfig> = DrawingTool.values().associateWith { tool ->
        when (tool) {
            DrawingTool.ERASER -> ToolConfig(size = 50f)
            // Strength/intensity is deliberately not user-facing for these two -- a single
            // "Size" slider is the only control that matters day to day, so these are tuned
            // once here for a smooth, FlipaClip-like feel at any brush size rather than left
            // for the animator to dial in themselves.
            DrawingTool.SMUDGE -> ToolConfig(size = 40f, strength = com.donnclab.kinetiframe.studio.drawing.ManipulationDefaults.SMUDGE)
            DrawingTool.BLUR -> ToolConfig(size = 40f, strength = com.donnclab.kinetiframe.studio.drawing.ManipulationDefaults.BLUR)
            else -> ToolConfig()
        }
    },
    val constructionConfig: ToolConfig = ToolConfig(),
    val eraserCursor: Offset? = null,
    /** Null keeps freehand input active; a value starts a transient construction-shape gesture. */
    val constructionShape: ConstructionShape? = null,
    val textToolOptions: TextToolOptions = TextToolOptions()
) {
    val activeConfig: ToolConfig
        get() {
            if (constructionShape != null) return constructionConfig
            val config = configs[activeTool] ?: ToolConfig()
            return if (activeTool == DrawingTool.BLUR || activeTool == DrawingTool.SMUDGE)
                config.copy(strength = com.donnclab.kinetiframe.studio.drawing.ManipulationDefaults.strength(activeTool)) else config
        }
}

data class OnionSkinState(
    val enabled: Boolean = true,
    val depthBefore: Int = 1,
    val depthAfter: Int = 1,
    val colorBefore: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF92003B),
    val colorAfter: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF00C853),
    val opacity: Float = 0.2f
)

data class SelectionState(
    val toolType: SelectionToolType = SelectionToolType.LASSO,
    val active: Boolean = false,
    val bounds: Rect? = null,
    val selectedContent: SelectedContent? = null,
    val transform: androidx.compose.ui.graphics.Matrix = androidx.compose.ui.graphics.Matrix(),
    val pivotOffset: Offset = Offset.Zero,
    /** Context actions are deliberately opt-in so discovery does not obstruct transforms. */
    val contextMenuVisible: Boolean = false,
    val partialInclude: Boolean = false,
    val interactionMode: SelectionInteractionMode = SelectionInteractionMode.TRANSFORM,
    val lockAspectRatio: Boolean = false,
    val skewMode: Boolean = false
)

data class AnimationState(
    val currentFrame: Int = 0,
    val totalFrames: Int = 1,
    val fps: Int = 12,
    val isPlaying: Boolean = false,
    val playbackFrame: Int = 0,
    val frameTransforms: Map<Int, FrameTransform> = emptyMap()
)

/**
 * The one frame the audience is currently seeing. Editing remains anchored to [currentFrame],
 * while project playback advances [playbackFrame] without mutating authoring selection state.
 * Every presentation-only renderer (raster, actors, audio indicators) must use this value.
 */
fun AnimationState.displayFrame(): Int = if (isPlaying) playbackFrame else currentFrame

data class LayerState(
    val layers: List<Layer> = emptyList(),
    val frameLayers: Map<Int, List<Layer>> = emptyMap(),
    val activeLayerIndex: Int = 0,
    val linkedCels: Map<String, Map<Int, String>> = emptyMap(),
    /** Transient editing scope. Local copy-on-write is always the default. */
    val linkedEditTarget: Pair<String, Int>? = null
)

/** Named lane groups on the frame-by-frame timeline. Each kind shares the same collapsible-group UI. */
enum class TrackGroupKind { ACTORS, AUDIO, VIDEO, BACKGROUND }

/** Which track groups the user has collapsed on the main timeline. A group only renders once it
 *  has content -- collapsing is a display preference, never persisted document state. */
data class TimelineTracksState(val collapsedGroups: Set<TrackGroupKind> = emptySet())

data class SceneState(
    val studioMode: StudioMode = StudioMode.RASTER_DRAWING,
    val pivotPlacement: PivotPlacement? = null,
    val selectedBoneId: String? = null,
    val nodes: Map<String, BaseNode> = emptyMap(),
    val activeNodeId: String? = null,
    /** Transient view preference, not part of the document: lets a user sketch with the rig
     *  overlay hidden, then toggle it back on to line up artwork against the bones. */
    val skeletonOverlayVisible: Boolean = true
)

data class FillState(
    val pendingTap: Offset? = null,
    val previewActive: Boolean = false,
    val dockOffset: Offset = Offset(1100f, 500f),
    val previewBitmap: Bitmap? = null
)

/** Transient generated-shape output; it becomes permanent only after explicit confirmation. */
data class ConstructionShapeState(
    val previewStrokes: List<List<TouchPoint>> = emptyList(),
    val previewActive: Boolean = false,
    val dockOffset: Offset = Offset(1100f, 500f)
)

/**
 * Represents the UI state of the animation timeline.
 */
data class StudioState(
    val actor: ActorStudioState = ActorStudioState(),
    val project: ProjectDocument = ProjectDocument(),
    val animation: AnimationState = AnimationState(),
    val layers: LayerState = LayerState(),
    val canvas: CanvasState = CanvasState(),
    val tool: ToolState = ToolState(),
    val onionSkin: OnionSkinState = OnionSkinState(),
    val selection: SelectionState = SelectionState(),
    val fill: FillState = FillState(),
    val constructionShape: ConstructionShapeState = ConstructionShapeState(),
    val scene: SceneState = SceneState(),
    val tracks: TimelineTracksState = TimelineTracksState(),
    val currentBitmap: Bitmap? = null,
    /** Transient bake receipts, published atomically with pixels; never project data. */
    val bakedStrokeSeeds: Set<Long> = emptySet(),
    val packTags: Map<Int, com.donnclab.kinetiframe.library.ui.packs.PackFrameTag> = emptyMap()
) : ViewState {
    val isCanvasTransformed: Boolean
        get() = canvas.zoom != 1f || canvas.pan != Offset.Zero || canvas.rotation != 0f
}

/**
 * The layer STACK for [frame] -- which layers exist, in what order, and with what
 * visibility/opacity/name.
 *
 * A layer is project-wide, exactly as in FlipaClip: the stack is one list shared by every frame,
 * and what differs per frame is only the *pixels* a layer holds, which live in the bitmap cache
 * keyed by (layerId, frameIndex). Reading a per-frame stack instead would let frames disagree:
 * a reorder on one frame would not apply to others, and frames without an entry would composite
 * with no layers at all, including during playback in
 * [com.donnclab.kinetiframe.studio.domain.PlaybackManager].
 *
 * [frame] is retained because callers are frame-oriented and because the legacy per-frame map is
 * still honoured as a fallback for documents saved before the stack became authoritative.
 */
fun StudioState.layersForFrame(frame: Int = animation.currentFrame): List<Layer> =
    layers.layers.ifEmpty { layers.frameLayers[frame].orEmpty() }

fun StudioState.currentFrameLayers(): List<Layer> = layersForFrame(animation.currentFrame)

/**
 * Represents user intentions/actions on the timeline.
 */
sealed class StudioIntent : ViewIntent {
    sealed class PackItem : StudioIntent() {
        data class SetTag(val index: Int, val tag: com.donnclab.kinetiframe.library.ui.packs.PackFrameTag) : PackItem()
        data class Add(val index: Int, val tag: com.donnclab.kinetiframe.library.ui.packs.PackFrameTag) : PackItem()
    }
    sealed class Actor : StudioIntent() {
        data class CommitDrawing(val definitionId: String, val ink: Bitmap, val colour: Bitmap,
            val document: com.donnclab.kinetiframe.actors.rig.model.ActorDrawing,
            val restGraph: com.donnclab.kinetiframe.actors.rig.model.SceneGraph? = null) : Actor()
        data class UseDrawing(val enabled: Boolean) : Actor()
        data class SetDrawingOverlay(val partId: String, val enabled: Boolean, val behind: Boolean = false) : Actor()
        data class SetEditTarget(val target: ActorEditTarget) : Actor()
        /** Bitmap ownership transfers to the actor asset manager, never to a library file. */
        data class AttachArtwork(val partId: String, val name: String, val bitmap: Bitmap,
            val alignment: com.donnclab.kinetiframe.actors.rig.model.TextureBinding, val asVariant: Boolean,
            val continuousChildId: String? = null, val replaceVariantId: String? = null,
            val attachmentBlend: Boolean = false) : Actor()
        data class SelectArtwork(val partId: String, val variantId: String) : Actor()
        data class UpdateArtwork(val partId: String, val variantId: String,
            val name: String, val alignment: com.donnclab.kinetiframe.actors.rig.model.TextureBinding) : Actor()
        data class RemoveArtwork(val partId: String, val variantId: String = "") : Actor()
        data class SetAttachmentBlend(val partId: String, val variantId: String = "", val enabled: Boolean) : Actor()
        data class AddSampleArtwork(val color: Int) : Actor()
        data class Create(val template: com.donnclab.kinetiframe.actors.rig.model.ActorTemplate) : Actor()
        data class Drag(val id: String, val target: Offset) : Actor()
        object CommitDrag : Actor()
        object CancelDrag : Actor()
        object ResetPose : Actor()
        object ToggleSource : Actor()
        object Undo : Actor()
        object Redo : Actor()
        data class RenameBone(val name: String) : Actor()
        data class RenameActor(val name: String) : Actor()
        object Unbind : Actor()
        /** Show/hide one part's rendered artwork without touching its binding or rig data. */
        data class SetPartVisibility(val boneId: String, val visible: Boolean) : Actor()
        data class MovePart(val boneId: String, val forward: Boolean) : Actor()
    }
    sealed class ActorMotion : StudioIntent() {
        object StartTake : ActorMotion()
        object StopTake : ActorMotion()
        object KeepTake : ActorMotion()
        object Retake : ActorMotion()
        object CancelTake : ActorMotion()
        data class SetTakeSmoothness(val value: Float) : ActorMotion()
        data class SetTakeMoveCharacter(val value: Boolean) : ActorMotion()
        object DeleteArtworkKey : ActorMotion()
        data class SetInterpolation(val value: com.donnclab.kinetiframe.actors.data.CurveInterpolation) : ActorMotion()
        data class Scrub(val timeMicros: Long) : ActorMotion()
        data class SetDuration(val durationMicros: Long) : ActorMotion()
        object TogglePlayback : ActorMotion()
        object RecordKey : ActorMotion()
        object DeleteKey : ActorMotion()
        /** Holds/releases the selected two-bone endpoint, recording an ordinary undoable key. */
        object ToggleContact : ActorMotion()
    }
    sealed class ActorPreset : StudioIntent() {
        data class Preview(val id: String) : ActorPreset()
        data class SetIntensity(val value: Float) : ActorPreset()
        data class SetSpeed(val value: Float) : ActorPreset()
        object Apply : ActorPreset()
        object Cancel : ActorPreset()
        object Remove : ActorPreset()
    }
    sealed class ActorPlacement : StudioIntent() {
        data class Add(val reference: com.donnclab.kinetiframe.project.model.ActorPlacementReference) : ActorPlacement()
        data class Duplicate(val placementId: String) : ActorPlacement()
        data class Remove(val placementId: String) : ActorPlacement()
        data class UpdateTransform(val placementId: String, val transform: com.donnclab.kinetiframe.actors.rig.model.Transform) : ActorPlacement()
        /** FxF can only change a placement's scene ordering; rig edits belong to Actor Studio. */
        data class BringToFront(val placementId: String) : ActorPlacement()
        data class SendToBack(val placementId: String) : ActorPlacement()
        /** Dragging a track clip's start/end handle on the main timeline. */
        data class Trim(val placementId: String, val startFrame: Int, val durationFrames: Int) : ActorPlacement()
        data class Move(val placementId: String, val startFrame: Int) : ActorPlacement()
        /** Inline, in-FxF switch to a different saved performance for an already-placed clip's
         *  actor -- deliberately plain fields rather than a data-layer PerformanceDocument, so
         *  this intent layer never has to depend on the data.actor package. No Actor Studio trip
         *  needed; only adding or editing a performance's content does. */
        data class SwitchPerformance(
            val placementId: String,
            val performanceId: String,
            val actorRevisionId: String,
            val label: String,
            val durationFrames: Int
        ) : ActorPlacement()
        /** Cuts a placed clip into two independent placements at [atFrame], which must fall
         *  strictly inside the clip's active range. See [com.donnclab.kinetiframe.actors.domain.ActorPlacementManager.split]
         *  for the timing caveat this shares with left-trim: neither half continues playback from
         *  where the other left off, since the placement model has no in-point field yet. */
        data class Split(val placementId: String, val atFrame: Int) : ActorPlacement()
    }
    /** Main-timeline track-lane UI preferences (collapse state); never touches project state. */
    sealed class Tracks : StudioIntent() {
        data class ToggleGroup(val group: TrackGroupKind) : Tracks()
    }
    sealed class Playback : StudioIntent() {
        object Play : Playback()
        object Pause : Playback()
        object NextFrame : Playback()
        object PreviousFrame : Playback()
        data class SetFps(val fps: Int) : Playback()
        data class SetCurrentFrame(val frame: Int) : Playback()
    }

    sealed class Draw : StudioIntent() {
        data class DrawStroke(val points: List<TouchPoint>) : Draw()
        /**
         * [targetBoneId] is supplied only by Actor Studio's freehand surface. It lets the engine
         * choose the destination artwork layer before the asynchronous bitmap bake starts.
         */
        data class BakeStroke(
            val points: List<TouchPoint>,
            val brush: BrushSnapshot? = null,
            val targetBoneId: String? = null
        ) : Draw()
        data class SelectTool(val tool: DrawingTool) : Draw()
        data class SelectConstructionShape(val shape: ConstructionShape?) : Draw()
        data class SetBrushColor(val color: androidx.compose.ui.graphics.Color) : Draw()
        data class SetBrushSize(val size: Float) : Draw()
        data class SelectBrush(val brushId: BrushId) : Draw()
        data class SetEraserSize(val size: Float) : Draw()
        data class SetEraserType(val type: EraserType) : Draw()
        object ClearAllHandwriting : Draw()
        data class SetBrushOpacity(val opacity: Float) : Draw()
        data class SetBrushStrength(val strength: Float) : Draw()
        data class SetEraserCursor(val offset: Offset?) : Draw()
        object Undo : Draw()
        object Redo : Draw()
    }

    sealed class Layer : StudioIntent() {
        data class SelectLayer(val index: Int) : Layer()
        data class AddLayer(val type: LayerType = LayerType.RASTER) : Layer()
        data class RemoveLayer(val index: Int) : Layer()
        data class ReorderLayer(val fromIndex: Int, val toIndex: Int) : Layer()
        data class SetLayerVisibility(val index: Int, val visible: Boolean) : Layer()
        data class ToggleLayerLock(val index: Int) : Layer()
        data class SetLayerOpacity(val index: Int, val opacity: Float) : Layer()
        data class RenameLayer(val index: Int, val name: String) : Layer()
    }

    sealed class Frame : StudioIntent() {
        object AddFrame : Frame()
        data class DeleteFrame(val index: Int) : Frame()
        data class DuplicateFrame(val index: Int) : Frame()
        data class CopyFrame(val index: Int) : Frame()
        data class CopyFrames(val indices: List<Int>) : Frame()
        data class PasteFrame(val index: Int, val before: Boolean) : Frame()
        data class PasteFrames(val index: Int, val before: Boolean = true) : Frame()
        data class DeleteFrames(val indices: List<Int>) : Frame()
        data class AddFrameAt(val index: Int) : Frame()
        data class HoldDrawing(val first: Int, val last: Int) : Frame()
        data class EditDrawingScope(val index: Int, val allLinked: Boolean) : Frame()
        data class UnlinkDrawing(val index: Int) : Frame()
    }

    sealed class View : StudioIntent() {
        data class SetGridSize(val size: GridSize) : View()
        data class SetGridSpacing(val spacingDp: Float) : View()
        data class SetGridEnabled(val enabled: Boolean) : View()
        data class SetGridOpacity(val opacity: Float) : View()
        data class SetGridSpacingVertical(val spacing: Float) : View()
        data class SetGridSpacingHorizontal(val spacing: Float) : View()
        object ResetGridSettings : View()
        data class SetRulerMode(val mode: RulerMode) : View()
        data class UpdateRulerGuide(val guide: RulerGuide) : View()
        data class UpdateCanvasTransform(val matrix: android.graphics.Matrix) : View()
        object ResetCanvasTransform : View()
        object ToggleSafeFrame : View()
        data class SetViewportSize(val width: Int, val height: Int) : View()
        data class UpdateTransform(val panDelta: Offset, val zoomDelta: Float, val rotationDelta: Float, val pivot: Offset) : View()
        data class SetDockPosition(val edge: DockEdge, val offset: Offset) : View()
    }

    sealed class OnionSkin : StudioIntent() {
        data class SetEnabled(val enabled: Boolean) : OnionSkin()
        data class SetFramesBefore(val frames: Int) : OnionSkin()
        data class SetFramesAfter(val frames: Int) : OnionSkin()
        data class SetColorBefore(val color: androidx.compose.ui.graphics.Color) : OnionSkin()
        data class SetColorAfter(val color: androidx.compose.ui.graphics.Color) : OnionSkin()
        data class SetOpacity(val opacity: Float) : OnionSkin()
        object Reset : OnionSkin()
    }

    sealed class Rigging : StudioIntent() {
        data class SetStudioMode(val mode: StudioMode) : Rigging()
        data class PreparePivotPlacement(
            val sourceFrame: Bitmap,
            val lassoPath: androidx.compose.ui.graphics.Path,
            val dilationRadius: Int = 8
        ) : Rigging()
        data class MovePivot(val x: Float, val y: Float) : Rigging()
        data class AddBoneAt(val x: Float, val y: Float) : Rigging()
        data class SelectBone(val id: String?) : Rigging()
        object ConfirmPivot : Rigging()
        object CancelPivot : Rigging()
        object ToggleSkeletonOverlay : Rigging()
    }

    sealed class Text : StudioIntent() {
        data class AddText(val text: String, val x: Float = 960f, val y: Float = 540f) : Text()
        data class UpdateText(val id: String, val text: String) : Text()
        data class SelectTextNode(val id: String?) : Text()
        data class DeleteTextNode(val id: String) : Text()
        data class SetTextToolOptions(val options: TextToolOptions) : Text()
        data class UpdateTextNode(val node: TextNode) : Text()
        data class TriggerEditDialog(val id: String) : Text()
        data class TriggerAddTextDialog(val x: Float, val y: Float) : Text()
    }

    sealed class Selection : StudioIntent() {
        data class SetTool(val type: SelectionToolType) : Selection()
        data class SelectArea(
            val path: androidx.compose.ui.graphics.Path? = null,
            val rect: Rect? = null,
            val density: Float = 1f,
            val fontRegistry: com.donnclab.kinetiframe.studio.drawing.FontRegistry? = null,
            val actors: com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry? = null
        ) : Selection()
        /** Selects a vector text node without rasterising the pixels beneath it. */
        data class SelectTextNode(
            val id: String,
            val density: Float,
            val fontRegistry: com.donnclab.kinetiframe.studio.drawing.FontRegistry? = null
        ) : Selection()
        data class SetActive(val active: Boolean) : Selection()
        data class UpdateTransform(val matrix: androidx.compose.ui.graphics.Matrix) : Selection()
        data class UpdatePivot(val offset: Offset) : Selection()
        data class SetDockPosition(val offset: Offset) : Selection()
        data class SetContextMenuVisible(val visible: Boolean) : Selection()
        data class SetInteractionMode(val mode: SelectionInteractionMode) : Selection()
        object Cut : Selection()
        object Copy : Selection()
        object Paste : Selection()
        /** Stamps an externally-sourced bitmap (a Library/Pack drawing) onto the current frame as
         *  a fresh, floating raster selection -- see [com.donnclab.kinetiframe.domain.manager.
         *  SelectionManager.pasteBitmap]'s doc comment for why this reuses the copy/paste
         *  machinery rather than a bespoke stamping path. */
        /** [centerX]/[centerY], in stage coordinates, place the stamp's center there instead
         *  of the stage's own center -- how a pack tray's drag-and-drop placement (see
         *  [com.donnclab.kinetiframe.library.ui.packs.PackTray]) lands a drawing exactly where the user dropped it.
         *  Null (the Library picker's plain tap-to-add path for a FRAME) keeps the original
         *  stage-centered behavior. */
        data class StampBitmap(val bitmap: android.graphics.Bitmap, val centerX: Float? = null, val centerY: Float? = null) : Selection()
        object Delete : Selection()
        object ResetTransform : Selection()
        object Cancel : Selection()
        data class SetAspectLock(val locked: Boolean) : Selection()
        data class SetSkewMode(val enabled: Boolean) : Selection()
        data class SetPartialInclude(val include: Boolean) : Selection()
    }

    sealed class Fill : StudioIntent() {
        data class SetPendingTap(val offset: Offset?) : Fill()
        data class SetDockPosition(val offset: Offset) : Fill()
        object Confirm : Fill()
        object Cancel : Fill()
        data class SetPreviewBitmap(val bitmap: Bitmap?) : Fill()
    }

    /** The project's single persistent background image. It applies to the whole scene, so it is
     *  neither a layer nor a timeline placement. */
    sealed class Background : StudioIntent() {
        /** [bitmap] is already decoded from the picked image by the caller, which has Android
         *  Context/ContentResolver access -- mirrors [Fill.SetPreviewBitmap]'s precedent so this
         *  intent, and the manager that handles it, stay Android-Context-free. */
        data class SetImage(val bitmap: Bitmap) : Background()
        object ToggleVisibility : Background()
        object Remove : Background()
    }
    /** Placed audio clips across the project's fixed audio-track lanes -- see
     *  [com.donnclab.kinetiframe.project.model.AudioClipPlacement]'s doc comment. [Add] mirrors
     *  [Background.SetImage]'s precedent: the caller (EditorScreen, which has Android
     *  Context/ContentResolver/MediaRecorder access) has already copied the audio file into the
     *  project's own storage and resolved its duration before this intent is ever dispatched, so
     *  [AudioManager] and this intent stay Android-Context-free. */
    sealed class Audio : StudioIntent() {
        data class VoiceStyle(val clipId: String, val expectedAssetId: String, val effect: com.donnclab.kinetiframe.audio.VoiceEffect,
            val proxy: com.donnclab.kinetiframe.project.model.ProjectAsset?) : Audio()
        data class Crossfade(val clipId: String, val nextClipId: String, val frames: Int) : Audio()
        data class SetFades(val clipId: String, val fadeInFrames: Int, val fadeOutFrames: Int) : Audio()
        data class Add(val placement: com.donnclab.kinetiframe.project.model.AudioClipPlacement, val asset: com.donnclab.kinetiframe.project.model.ProjectAsset) : Audio()
        data class Remove(val clipId: String) : Audio()
        data class Trim(val clipId: String, val startFrame: Int, val durationFrames: Int) : Audio()
        data class Move(val clipId: String, val trackIndex: Int, val startFrame: Int) : Audio()
        data class SetVolume(val clipId: String, val volume: Float) : Audio()
        /** Lane-wide, not clip-wide -- see [com.donnclab.kinetiframe.project.model.AudioTrackSettings]. */
        data class SetTrackMuted(val trackIndex: Int, val muted: Boolean) : Audio()
        data class SetTrackLocked(val trackIndex: Int, val locked: Boolean) : Audio()
        data class Duplicate(val clipId: String) : Audio()
        /** [atFrame] is a timeline frame, not an offset into the clip. */
        data class Split(val clipId: String, val atFrame: Int) : Audio()
    }
    sealed class Video : StudioIntent() {
        data class Framing(val clipId: String, val fit: com.donnclab.kinetiframe.project.model.VideoFit, val cropX: Float, val cropY: Float) : Video()
        data class Split(val clipId: String, val atFrame: Int) : Video()
        data class Select(val clipId: String) : Video()
        data class DetachAudio(val clipId: String) : Video()
        data class Add(val placement: com.donnclab.kinetiframe.project.model.VideoClipPlacement, val asset: com.donnclab.kinetiframe.project.model.ProjectAsset,
            val sourceAudio: com.donnclab.kinetiframe.project.model.AudioClipPlacement? = null) : Video()
        data class Remove(val clipId: String) : Video()
        data class Trim(val clipId: String, val timelineStartFrame: Int, val timelineEndFrame: Int, val videoSourceOffsetSeconds: Double) : Video()
        data class SetTrackOpacity(val opacity: Float) : Video()
    }
    sealed class Camera : StudioIntent() {
        data class Put(val shot: com.donnclab.kinetiframe.studio.camera.CameraShot) : Camera()
        data class Remove(val id: String) : Camera()
        object Undo : Camera()
        object Redo : Camera()
    }
    sealed class Construction : StudioIntent() {
        data class SetPreview(val strokes: List<List<TouchPoint>>) : Construction()
        data class SetDockPosition(val offset: Offset) : Construction()
        object Confirm : Construction()
        object Cancel : Construction()
    }
}


/**
 * One-time events related to the timeline.
 */
sealed class StudioEffect : ViewEffect {
    data class ShowError(val message: String) : StudioEffect()
    data class ShowTextEditDialog(val id: String, val initialText: String) : StudioEffect()
    data class ShowTextAddDialog(val x: Float, val y: Float) : StudioEffect()
}
