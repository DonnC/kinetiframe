package com.donnclab.kinetiframe.studio

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.donnclab.kinetiframe.actors.rig.model.PivotPlacement
import com.donnclab.kinetiframe.project.model.BaseNode
import com.donnclab.kinetiframe.project.model.DrawCommand
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.project.model.TextNode
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.studio.drawing.BrushId
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import com.donnclab.kinetiframe.ui.ViewState

/** Legacy names kept for older callers; the editor now uses a continuous slider. */
enum class GridSize(val sizeDp: Int) {
    SMALL(20),
    MEDIUM(50),
    LARGE(100),
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
    val audio: Map<String, com.donnclab.kinetiframe.project.model.AudioClipPlacement> = emptyMap(),
)

data class RulerGuide(
    val mode: RulerMode,
    val start: Offset = Offset(420f, 540f),
    val end: Offset = Offset(1500f, 540f),
    val center: Offset = Offset(960f, 540f),
    val radius: Offset = Offset(360f, 240f),
    val rotation: Float = 0f,
    val locked: Boolean = false,
)

data class TextToolOptions(
    val fontFileName: String = "inter_regular.ttf",
    val fontSizeSp: Float = 36f,
    val textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    val alignment: TextNode.TextAlignment = TextNode.TextAlignment.CENTER,
)

data class FrameTransform(
    val matrixValues: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f,
    ),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrameTransform) return false
        return matrixValues.contentEquals(other.matrixValues)
    }

    override fun hashCode(): Int = matrixValues.contentHashCode()
}

data class CanvasState(
    val matrixValues: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f,
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
    val selectionDockOffset: Offset = Offset(1100f, 300f),
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
    val brushId: BrushId = BrushId.PEN,
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
            DrawingTool.SMUDGE -> ToolConfig(
                size = 40f,
                strength = com.donnclab.kinetiframe.studio.drawing.ManipulationDefaults.SMUDGE,
            )

            DrawingTool.BLUR -> ToolConfig(
                size = 40f,
                strength = com.donnclab.kinetiframe.studio.drawing.ManipulationDefaults.BLUR,
            )

            else -> ToolConfig()
        }
    },
    val constructionConfig: ToolConfig = ToolConfig(),
    val eraserCursor: Offset? = null,
    /** Null keeps freehand input active; a value starts a transient construction-shape gesture. */
    val constructionShape: ConstructionShape? = null,
    val textToolOptions: TextToolOptions = TextToolOptions(),
) {
    val activeConfig: ToolConfig
        get() {
            if (constructionShape != null) return constructionConfig
            val config = configs[activeTool] ?: ToolConfig()
            return if (activeTool == DrawingTool.BLUR || activeTool == DrawingTool.SMUDGE) {
                config.copy(
                    strength = com.donnclab.kinetiframe.studio.drawing.ManipulationDefaults.strength(activeTool),
                )
            } else {
                config
            }
        }
}

data class OnionSkinState(
    val enabled: Boolean = true,
    val depthBefore: Int = 1,
    val depthAfter: Int = 1,
    val colorBefore: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF92003B),
    val colorAfter: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF00C853),
    val opacity: Float = 0.2f,
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
    val skewMode: Boolean = false,
)

data class AnimationState(
    val currentFrame: Int = 0,
    val totalFrames: Int = 1,
    val fps: Int = 12,
    val isPlaying: Boolean = false,
    val playbackFrame: Int = 0,
    val frameTransforms: Map<Int, FrameTransform> = emptyMap(),
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
    val linkedEditTarget: Pair<String, Int>? = null,
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
    val skeletonOverlayVisible: Boolean = true,
)

data class FillState(
    val pendingTap: Offset? = null,
    val previewActive: Boolean = false,
    val dockOffset: Offset = Offset(1100f, 500f),
    val previewBitmap: Bitmap? = null,
)

/** Transient generated-shape output; it becomes permanent only after explicit confirmation. */
data class ConstructionShapeState(
    val previewStrokes: List<List<TouchPoint>> = emptyList(),
    val previewActive: Boolean = false,
    val dockOffset: Offset = Offset(1100f, 500f),
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
    val packTags: Map<Int, com.donnclab.kinetiframe.library.ui.packs.PackFrameTag> = emptyMap(),
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
