package com.donnclab.kinetiframe.video

import com.donnclab.kinetiframe.audio.AudioManager
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.studio.StudioState
import kotlin.math.roundToInt

/** Pure project-reference mutations for the single video track's clips -- structurally close to
 *  [AudioManager], but every mutation here also enforces the "single track, sequential,
 *  non-overlapping clips" rule described on [com.donnclab.kinetiframe.core.document.VideoClipPlacement].
 *  Decoding and copying video files happens at the UI layer, which has the Android Context; this
 *  class only receives an already-resolved [ProjectAsset] and placement. */
class VideoTrackManager(
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
) {
    /** [asset] is inserted alongside [placement] atomically, same as [AudioManager.add]. Throws
     *  if the new clip's timeline range would overlap any existing video clip; existing clips are
     *  never pushed along. Callers must propose a free range, such as right after the last clip
     *  or at the start of an empty track. */
    fun add(
        placement: VideoClipPlacement,
        asset: ProjectAsset,
        audio: com.donnclab.kinetiframe.core.document.AudioClipPlacement? = null,
    ) {
        val project = getState().project
        require(placement.clipId !in project.videoClips) { "Video clip already exists" }
        require(asset.id == placement.assetId) { "Video clip references a different asset than the one provided" }
        require(
            project.videoClips.values.none {
                it.overlaps(placement)
            },
        ) { "Video clips on the single video track cannot overlap" }
        require(
            if (audio ==
                null
            ) {
                placement.linkedAudioClipId == null
            } else {
                audio.assetId == asset.id &&
                    placement.linkedAudioClipId == audio.clipId &&
                    audio.clipId !in project.audioClips
            },
        )
        updateState { state ->
            state.copy(
                animation = state.animation.copy(
                    totalFrames = maxOf(state.animation.totalFrames, placement.timelineEndFrame),
                ),
                project = state.project.copy(
                    videoClips = state.project.videoClips + (placement.clipId to placement),
                    assets = state.project.assets + (asset.id to asset),
                    audioClips = if (audio ==
                        null
                    ) {
                        state.project.audioClips
                    } else {
                        state.project.audioClips + (audio.clipId to audio)
                    },
                ),
            )
        }
    }

    /** Its source asset is left behind, mirroring [AudioManager.remove]'s reasoning exactly. */
    fun remove(clipId: String) = updateState { state ->
        val audioId = state.project.videoClips[clipId]?.linkedAudioClipId
        val audio = audioId?.let { state.project.audioClips[it] }
        if (audio != null && state.project.audioTracks[audio.trackIndex]?.isLocked == true) return@updateState state
        state.copy(
            project = state.project.copy(
                videoClips = state.project.videoClips - clipId,
                audioClips = if (audioId == null) state.project.audioClips else state.project.audioClips - audioId,
            ),
        )
    }
    fun detachAudio(clipId: String) = updateState { state ->
        val clip = state.project.videoClips[clipId] ?: return@updateState state
        state.copy(
            project = state.project.copy(
                videoClips =
                state.project.videoClips + (clipId to clip.copy(linkedAudioClipId = null)),
            ),
        )
    }

    /** Re-trims a clip's timeline range and/or source in-point. Silently clamped/no-op on an
     *  invalid or now-overlapping result rather than throwing, matching [AudioManager.trim]'s
     *  drag-handle-friendly behavior -- a live drag gesture must never crash mid-drag. */
    fun trim(clipId: String, timelineStartFrame: Int, timelineEndFrame: Int, videoSourceOffsetSeconds: Double) {
        val project = getState().project
        val existing = project.videoClips[clipId] ?: return
        val audio = existing.linkedAudioClipId?.let { project.audioClips[it] }
        if (audio != null && project.audioTracks[audio.trackIndex]?.isLocked == true) return
        val safeStart = timelineStartFrame.coerceAtLeast(0)
        if (!videoSourceOffsetSeconds.isFinite()) return
        val fps = project.fps.coerceAtLeast(1)
        val span =
            existing.sourceDurationFrames.takeIf { it > 0 }
                ?: (existing.timelineEndFrame - existing.timelineStartFrame + existing.sourceOffsetFrames)
        val cut = (
            existing.sourceOffsetFrames +
                ((videoSourceOffsetSeconds - existing.videoSourceOffsetSeconds) * fps).roundToInt()
            ).coerceIn(0, span - 1)
        val kept = (timelineEndFrame - safeStart).coerceIn(1, span - cut)
        val cutSeconds = (cut - existing.sourceOffsetFrames).toDouble() / fps
        val safeOffset = (existing.videoSourceOffsetSeconds + cutSeconds).coerceAtLeast(0.0)
        val updated = existing.copy(
            timelineStartFrame = safeStart,
            timelineEndFrame = safeStart + kept,
            videoSourceOffsetSeconds = safeOffset,
            sourceDurationFrames = span,
            sourceOffsetFrames = cut,
        )
        if (project.videoClips.values.any { it.clipId != clipId && it.overlaps(updated) }) return
        updateState { state ->
            state.copy(
                animation = state.animation.copy(
                    totalFrames = maxOf(state.animation.totalFrames, updated.timelineEndFrame),
                ),
                project = state.project.copy(
                    videoClips = state.project.videoClips + (clipId to updated),
                    audioClips = if (audio == null) {
                        state.project.audioClips
                    } else {
                        state.project.audioClips +
                            (
                                audio.clipId to
                                    com.donnclab.kinetiframe.video.VideoAudioLink.synchronize(
                                        updated,
                                        audio,
                                        fps,
                                    )
                                )
                    },
                ),
            )
        }
    }

    fun setTrackOpacity(opacity: Float) = updateState { state ->
        state.copy(project = state.project.copy(videoTrackOpacity = opacity.coerceIn(0f, 1f)))
    }

    /** The first frame at or after every existing clip's end -- i.e. the next free slot at the
     *  tail of the track. 0 when the track is empty. Exposed so EditorScreen's video-import flow
     *  always proposes a range guaranteed not to overlap, without duplicating this scan itself. */
    fun nextFreeStartFrame(): Int = getState().project.videoClips.values.maxOfOrNull { it.timelineEndFrame } ?: 0

    private fun VideoClipPlacement.overlaps(other: VideoClipPlacement): Boolean =
        timelineStartFrame < other.timelineEndFrame && other.timelineStartFrame < timelineEndFrame
}
