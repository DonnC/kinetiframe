package com.donnclab.kinetiframe.project.model

import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.audio.VoiceEffect
import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.project.model.BaseNode
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.studio.camera.CameraShot
import com.donnclab.kinetiframe.studio.camera.SceneCamera
import com.donnclab.kinetiframe.studio.drawing.Affine2D
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AssetKind { RASTER_TEXTURE, IMPORTED_IMAGE, IMPORTED_MEDIA, AUDIO, VIDEO }

/** Persistent asset metadata. Decoded bitmaps remain in the runtime asset cache. */
@Serializable
data class ProjectAsset(
    val id: String,
    val kind: AssetKind,
    val sourcePath: String? = null,
    val width: Int,
    val height: Int,
)

@Serializable
data class TextureSlot(val id: String, val defaultAssetId: String, val alternateAssetIds: Set<String> = emptySet())

/** Reusable rest-pose puppet data, edited outside scene-timeline placement. */
@Serializable
data class PuppetDefinition(
    val id: String,
    val name: String,
    val sceneGraph: SceneGraph = SceneGraph(),
    val textureSlots: Map<String, TextureSlot> = emptyMap(),
)

/** A scene placement of a reusable [PuppetDefinition]. */
@Serializable
data class PuppetInstance(
    val id: String,
    val definitionId: String,
    val transform: Transform = Transform(),
    /** Current instance pose, independent of the reusable rest skeleton. */
    val pose: Map<String, Transform> = emptyMap(),
    val poseKeys: List<ActorPoseKey> = emptyList(),
    val motionPreset: AppliedActorPreset? = null,
    /** Keep explicit imported STEP/LINEAR channels intact in Studio until a pose is edited. */
    val rotationCurves: List<RotationCurve> = emptyList(),
    val poseInterpolation: CurveInterpolation = CurveInterpolation.SMOOTH,
    val motionPlayback: PerformancePlayback = PerformancePlayback.LOOP,
    val artworkKeys: List<ActorArtworkKey> = emptyList(),
    val rootMotionCurves: List<RootMotionCurve> = emptyList(),
)

/** Lightweight FxF timeline reference to durable performance math stored outside frame pixels. */
@Serializable
data class ActorPlacementReference(
    val placementId: String,
    val performanceId: String,
    val actorId: String,
    val actorRevisionId: String,
    val label: String,
    val startFrame: Int,
    val durationFrames: Int,
    val repeatCount: Int = 1,
    val zIndex: Int = 0,
    /** Scene-only placement; never mutates the actor revision or performance mathematics. */
    val transform: Transform = Transform(),
    /** Retained performance time and loop period when a placed performance is cut. */
    val sourceOffsetFrames: Int = 0,
    val sourceLoopFrames: Int = 0,
    val sourceSpanFrames: Int = 0,
    /** Applied after the original TRS. Keeps selection skew exact without changing rig math. */
    val selectionTransform: Affine2D = Affine2D(),
) {
    init {
        require(startFrame >= 0 && durationFrames > 0 && repeatCount > 0)
        require(sourceOffsetFrames >= 0 && sourceLoopFrames >= 0 && sourceSpanFrames >= 0)
        require(
            listOf(
                transform.x,
                transform.y,
                transform.rotationDegrees,
                transform.scaleX,
                transform.scaleY,
            ).all(Float::isFinite),
        )
    }
}

/** Fixed lane count for the audio track group, matching FlipaClip's own multi-track audio UI --
 *  a project-wide ceiling rather than a per-clip concept, since a track index only means
 *  something in relation to how many lanes actually exist. */
const val MAX_AUDIO_TRACKS = 6

/**
 * Per-lane audio settings.
 *
 * Kept on the lane rather than on each clip because muting is a mixing decision about a whole
 * voice ("drop the ambience while I check the dialogue"), not about one take -- muting a lane must
 * not require touching, or later un-touching, every clip sitting on it. [isLocked] guards a lane
 * whose timing is settled from accidental drags while the animator works on a neighbouring lane.
 *
 * Absent from the map means default (audible, unlocked), so a project saved before lanes had
 * settings loads with every lane behaving exactly as it did.
 */
@Serializable
data class AudioTrackSettings(val isMuted: Boolean = false, val isLocked: Boolean = false)

/** A placed audio clip on one of the project's [MAX_AUDIO_TRACKS] audio lanes -- structurally the
 *  audio counterpart to [ActorPlacementReference]: a lightweight timeline pointer into an
 *  [AssetKind.AUDIO] entry in [ProjectDocument.assets], never the audio bytes themselves. Unlike
 *  an actor placement, clips on different tracks may freely overlap in time (separate lanes mix
 *  together on playback, like separate voice/music/SFX tracks). Clips on the *same* track are not
 *  prevented from overlapping either, but such overlap has no defined meaning. */
@Serializable
data class AudioClipPlacement(
    val clipId: String,
    val assetId: String,
    val trackIndex: Int,
    val label: String,
    val startFrame: Int,
    val durationFrames: Int,
    /** In-point within the source audio file, for a clip trimmed from a longer recording/import. */
    val trimStartMicros: Long = 0L,
    val volume: Float = 1f,
    /**
     * Normalised 0f..1f amplitude peaks for drawing this clip's waveform, persisted with the
     * placement so a lane can render without decoding audio on every recomposition.
     *
     * Captured for free while recording (the live meter's own peaks) and decoded once on import
     * by the waveform extractor. Empty is a valid, non-fatal state --
     * a clip whose source could not be decoded still places and still plays; its lane just draws
     * a flat bar instead of an outline.
     */
    val waveformPeaks: List<Float> = emptyList(),
    /**
     * Full length of the underlying source file, so a placed clip can be re-trimmed later against
     * the whole recording rather than only the part currently on the timeline.
     *
     * Without it, trimming would be one-way: once a clip was shortened, the audio beyond its
     * out-point would be unreachable even though the file still holds it. 0 means "unknown",
     * which is what every clip saved before this field existed reports -- the trim editor falls
     * back to the placed duration in that case rather than refusing to open.
     */
    val sourceDurationMicros: Long = 0L,
    /** Retain the original selected source window so lane trims can be widened again. */
    val trimSource: AudioClipSourceWindow? = null,
    val trimOffsetFrames: Int = 0,
    val fadeInFrames: Int = 0,
    val fadeOutFrames: Int = 0,
    /** Preserve a fade's original clock when splitting; zero duration means this clip's window. */
    val envelopeOffsetFrames: Int = 0,
    val envelopeDurationFrames: Int = 0,
    val equalPowerFades: Boolean = false,
    val voiceEffect: VoiceEffect = VoiceEffect.ORIGINAL,
    /** Effect proxies are replaceable; this source remains available for undo/re-edit/archive. */
    val originalAssetId: String? = null,
) {
    init {
        require(trackIndex in 0 until MAX_AUDIO_TRACKS) {
            "Audio track index must be between 0 and ${MAX_AUDIO_TRACKS - 1}"
        }
        require(startFrame >= 0 && durationFrames > 0) {
            "Audio clip must start at or after frame 0 with a positive duration"
        }
        require(trimStartMicros >= 0) { "Audio clip's in-point cannot be negative" }
        require(trimOffsetFrames >= 0)
        require(fadeInFrames >= 0 && fadeOutFrames >= 0)
        require(envelopeOffsetFrames >= 0 && envelopeDurationFrames >= 0)
        require(volume in 0f..1f) { "Audio clip volume must be between 0 and 1" }
    }
}

@Serializable
data class AudioClipSourceWindow(val inMicros: Long, val durationFrames: Int, val peaks: List<Float>) {
    init {
        require(inMicros >= 0 && durationFrames > 0)
    }
}

/**
 * A clip on the project's single video lane -- either embedded scene content or a legacy
 * non-destructive tracing reference,
 * deliberately built to avoid splitting an imported video into hundreds of individual frame
 * images. The source video file is copied into the project once and never re-encoded or cut;
 * [videoSourceOffsetSeconds] is the only trim mechanism, an in-point into that one file. Playback
 * decodes on demand for whichever frame is current rather than pre-splitting.
 *
 * There is only ever one video track (unlike audio's [MAX_AUDIO_TRACKS] lanes) -- a deliberate
 * "single-track, multi-clip" compromise for Android hardware decoder concurrency limits (a
 * low-end phone may only decode ~2 video streams at once). Multiple clips may exist end to end on
 * that one track, but must never overlap in time; the video track manager enforces that when
 * clips are added or trimmed.
 */
@Serializable
data class VideoClipPlacement(
    val clipId: String,
    val assetId: String,
    val label: String,
    val timelineStartFrame: Int,
    val timelineEndFrame: Int,
    /** In-point into the source video file, in seconds -- the sole trim mechanism; the file
     *  itself is never cut or re-encoded. */
    val videoSourceOffsetSeconds: Double = 0.0,
    val sourceDurationFrames: Int = 0,
    val sourceOffsetFrames: Int = 0,
    /** Old archives remain tracing aids; new imports explicitly opt into scene content. */
    val isSceneContent: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 640f,
    val height: Float = 360f,
    val selectionTransform: Affine2D = Affine2D(),
    val fit: VideoFit = VideoFit.STRETCH,
    val cropX: Float = .5f,
    val cropY: Float = .5f,
    /** Audio reads the same owned container, without duplicating or re-encoding it. */
    val linkedAudioClipId: String? = null,
) {
    init {
        require(timelineStartFrame >= 0) { "Video clip must start at or after frame 0" }
        require(timelineEndFrame > timelineStartFrame) { "Video clip's end frame must be after its start frame" }
        require(videoSourceOffsetSeconds >= 0.0) { "Video clip's in-point cannot be negative" }
        require(videoSourceOffsetSeconds.isFinite())
        require(x.isFinite() && y.isFinite() && width.isFinite() && height.isFinite() && width > 0f && height > 0f)
        require(cropX.isFinite() && cropY.isFinite() && cropX in 0f..1f && cropY in 0f..1f)
    }
}

@Serializable
enum class VideoFit { STRETCH, FIT, FILL }

/** Durable project model; selection and playback remain transient UI state. */
@Serializable
data class ProjectDocument(
    val version: Int = 1,
    /** Stable key for project-scoped performances and placements; persisted once project save is introduced. */
    val id: String = "project-${UUID.randomUUID()}",
    val stage: StageDimensions = StageDimensions.LandscapeHd,
    val fps: Int = 12,
    val layers: List<Layer> = emptyList(),
    /** Editable scene content, not a viewport preference. Selection flags are cleared on Save. */
    val nodes: Map<String, BaseNode> = emptyMap(),
    val nodeDensity: Float = 1f,
    val assets: Map<String, ProjectAsset> = emptyMap(),
    val puppetDefinitions: Map<String, PuppetDefinition> = emptyMap(),
    val puppetInstances: Map<String, PuppetInstance> = emptyMap(),
    val actorPlacements: Map<String, ActorPlacementReference> = emptyMap(),
    /** The single persistent background image, if one has been set -- an ID into [assets], kind
     *  [AssetKind.IMPORTED_IMAGE]. Rendered behind every layer on every frame; it is not a layer
     *  or a timeline placement because it applies to the whole scene.
     *  Null means "no background," not "inherit" or "same as last frame" -- there is no per-frame
     *  background state to inherit from. */
    val backgroundAssetId: String? = null,
    /** Placed audio clips across up to [MAX_AUDIO_TRACKS] lanes -- see [AudioClipPlacement]. */
    val audioClips: Map<String, AudioClipPlacement> = emptyMap(),
    /** Per-lane mute/lock, keyed by track index. A missing entry means the lane's defaults --
     *  see [AudioTrackSettings]. */
    val audioTracks: Map<Int, AudioTrackSettings> = emptyMap(),
    /** Clips on the single video lane -- see [VideoClipPlacement]. Sequential and
     *  non-overlapping by construction. */
    val videoClips: Map<String, VideoClipPlacement> = emptyMap(),
    /** Track-wide "rotoscope" opacity for the video reference layer, so the artist can trace over
     *  the video while it plays back faintly beneath their strokes. This is a single
     *  project-wide value, not per-clip, matching the track-header slider that controls it. */
    val videoTrackOpacity: Float = 0.5f,
    val cameraShots: List<CameraShot> = emptyList(),
) {
    init {
        require(fps in 1..30) { "Project FPS must be between 1 and 30." }
        require(nodeDensity.isFinite() && nodeDensity > 0f) { "Invalid text drawing density" }
        require(videoTrackOpacity in 0f..1f) { "Video track opacity must be between 0 and 1" }
        SceneCamera.validate(cameraShots)
    }

    fun instanceForLayer(layer: Layer): PuppetInstance? = layer.puppetInstanceId?.let(puppetInstances::get)
}
