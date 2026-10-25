package com.donnclab.kinetiframe.studio

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.AudioTrackSettings
import com.donnclab.kinetiframe.core.document.BrushId
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.LayerType
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.core.document.VideoFit
import com.donnclab.kinetiframe.core.document.VoiceEffect
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import com.donnclab.kinetiframe.library.ui.packs.PackFrameTag
import com.donnclab.kinetiframe.studio.domain.actorstudio.ActorPlacementManager
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import com.donnclab.kinetiframe.studio.drawing.FontRegistry
import com.donnclab.kinetiframe.ui.ViewIntent

/**
 * Everything the user can ask the studio to do. The ViewModel handles each intent explicitly.
 */
sealed class StudioIntent : ViewIntent {
    sealed class PackItem : StudioIntent() {
        data class SetTag(val index: Int, val tag: PackFrameTag) : PackItem()
        data class Add(val index: Int, val tag: PackFrameTag) : PackItem()
    }
    sealed class Actor : StudioIntent() {
        data class CommitDrawing(
            val definitionId: String,
            val ink: Bitmap,
            val colour: Bitmap,
            val document: ActorDrawing,
            val restGraph: SceneGraph? = null,
        ) : Actor()
        data class UseDrawing(val enabled: Boolean) : Actor()
        data class SetDrawingOverlay(val partId: String, val enabled: Boolean, val behind: Boolean = false) : Actor()
        data class SetEditTarget(val target: ActorEditTarget) : Actor()

        /** Bitmap ownership transfers to the actor asset manager, never to a library file. */
        data class AttachArtwork(
            val partId: String,
            val name: String,
            val bitmap: Bitmap,
            val alignment: TextureBinding,
            val asVariant: Boolean,
            val continuousChildId: String? = null,
            val replaceVariantId: String? = null,
            val attachmentBlend: Boolean = false,
        ) : Actor()
        data class SelectArtwork(val partId: String, val variantId: String) : Actor()
        data class UpdateArtwork(
            val partId: String,
            val variantId: String,
            val name: String,
            val alignment: TextureBinding,
        ) : Actor()
        data class RemoveArtwork(val partId: String, val variantId: String = "") : Actor()
        data class SetAttachmentBlend(val partId: String, val variantId: String = "", val enabled: Boolean) : Actor()
        data class AddSampleArtwork(val color: Int) : Actor()
        data class Create(val template: ActorTemplate) : Actor()
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
        data class SetInterpolation(val value: CurveInterpolation) : ActorMotion()
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
        data class Add(val reference: ActorPlacementReference) : ActorPlacement()
        data class Duplicate(val placementId: String) : ActorPlacement()
        data class Remove(val placementId: String) : ActorPlacement()
        data class UpdateTransform(val placementId: String, val transform: Transform) : ActorPlacement()

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
            val durationFrames: Int,
        ) : ActorPlacement()

        /** Cuts a placed clip into two independent placements at [atFrame], which must fall
         *  strictly inside the clip's active range. See [ActorPlacementManager.split]
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
            val targetBoneId: String? = null,
        ) : Draw()
        data class SelectTool(val tool: DrawingTool) : Draw()
        data class SelectConstructionShape(val shape: ConstructionShape?) : Draw()
        data class SetBrushColor(val color: Color) : Draw()
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
        data class UpdateCanvasTransform(val matrix: Matrix) : View()
        object ResetCanvasTransform : View()
        object ToggleSafeFrame : View()
        data class SetViewportSize(val width: Int, val height: Int) : View()
        data class UpdateTransform(
            val panDelta: Offset,
            val zoomDelta: Float,
            val rotationDelta: Float,
            val pivot: Offset,
        ) : View()
        data class SetDockPosition(val edge: DockEdge, val offset: Offset) : View()
    }

    sealed class OnionSkin : StudioIntent() {
        data class SetEnabled(val enabled: Boolean) : OnionSkin()
        data class SetFramesBefore(val frames: Int) : OnionSkin()
        data class SetFramesAfter(val frames: Int) : OnionSkin()
        data class SetColorBefore(val color: Color) : OnionSkin()
        data class SetColorAfter(val color: Color) : OnionSkin()
        data class SetOpacity(val opacity: Float) : OnionSkin()
        object Reset : OnionSkin()
    }

    sealed class Rigging : StudioIntent() {
        data class SetStudioMode(val mode: StudioMode) : Rigging()
        data class PreparePivotPlacement(val sourceFrame: Bitmap, val lassoPath: Path, val dilationRadius: Int = 8) :
            Rigging()
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
            val path: Path? = null,
            val rect: Rect? = null,
            val density: Float = 1f,
            val fontRegistry: FontRegistry? = null,
            val actors: ActorSelectionGeometry? = null,
        ) : Selection()

        /** Selects a vector text node without rasterising the pixels beneath it. */
        data class SelectTextNode(val id: String, val density: Float, val fontRegistry: FontRegistry? = null) :
            Selection()
        data class SetActive(val active: Boolean) : Selection()
        data class UpdateTransform(val matrix: androidx.compose.ui.graphics.Matrix) : Selection()
        data class UpdatePivot(val offset: Offset) : Selection()
        data class SetDockPosition(val offset: Offset) : Selection()
        data class SetContextMenuVisible(val visible: Boolean) : Selection()
        data class SetInteractionMode(val mode: SelectionInteractionMode) : Selection()
        object Cut : Selection()
        object Copy : Selection()
        object Paste : Selection()

        /**
         * Stamps a library or pack drawing onto the current frame as a new floating selection,
         * using the same path as paste.
         *
         * [centerX] and [centerY], in stage coordinates, place the stamp's center where a pack tray
         * drag was dropped. When they are null, as for a tap in the library picker, the stamp is
         * centered on the stage.
         */
        data class StampBitmap(val bitmap: Bitmap, val centerX: Float? = null, val centerY: Float? = null) :
            Selection()
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
     *  [AudioClipPlacement]'s doc comment. [Add] mirrors
     *  [Background.SetImage]'s precedent: the caller (EditorScreen, which has Android
     *  Context/ContentResolver/MediaRecorder access) has already copied the audio file into the
     *  project's own storage and resolved its duration before this intent is ever dispatched, so
     *  [AudioManager] and this intent stay Android-Context-free. */
    sealed class Audio : StudioIntent() {
        data class VoiceStyle(
            val clipId: String,
            val expectedAssetId: String,
            val effect: VoiceEffect,
            val proxy: ProjectAsset?,
        ) : Audio()
        data class Crossfade(val clipId: String, val nextClipId: String, val frames: Int) : Audio()
        data class SetFades(val clipId: String, val fadeInFrames: Int, val fadeOutFrames: Int) : Audio()
        data class Add(val placement: AudioClipPlacement, val asset: ProjectAsset) : Audio()
        data class Remove(val clipId: String) : Audio()
        data class Trim(val clipId: String, val startFrame: Int, val durationFrames: Int) : Audio()
        data class Move(val clipId: String, val trackIndex: Int, val startFrame: Int) : Audio()
        data class SetVolume(val clipId: String, val volume: Float) : Audio()

        /** Lane-wide, not clip-wide -- see [AudioTrackSettings]. */
        data class SetTrackMuted(val trackIndex: Int, val muted: Boolean) : Audio()
        data class SetTrackLocked(val trackIndex: Int, val locked: Boolean) : Audio()
        data class Duplicate(val clipId: String) : Audio()

        /** [atFrame] is a timeline frame, not an offset into the clip. */
        data class Split(val clipId: String, val atFrame: Int) : Audio()
    }
    sealed class Video : StudioIntent() {
        data class Framing(val clipId: String, val fit: VideoFit, val cropX: Float, val cropY: Float) : Video()
        data class Split(val clipId: String, val atFrame: Int) : Video()
        data class Select(val clipId: String) : Video()
        data class DetachAudio(val clipId: String) : Video()
        data class Add(
            val placement: VideoClipPlacement,
            val asset: ProjectAsset,
            val sourceAudio: AudioClipPlacement? = null,
        ) : Video()
        data class Remove(val clipId: String) : Video()
        data class Trim(
            val clipId: String,
            val timelineStartFrame: Int,
            val timelineEndFrame: Int,
            val videoSourceOffsetSeconds: Double,
        ) : Video()
        data class SetTrackOpacity(val opacity: Float) : Video()
    }
    sealed class Camera : StudioIntent() {
        data class Put(val shot: CameraShot) : Camera()
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
