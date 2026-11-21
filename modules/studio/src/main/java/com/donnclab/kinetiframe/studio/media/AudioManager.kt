package com.donnclab.kinetiframe.studio.media

import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.AudioTrackSettings
import com.donnclab.kinetiframe.core.document.MAX_AUDIO_TRACKS
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.domain.actorstudio.ActorPlacementManager

/** Pure project-reference mutations for placed audio clips -- structurally identical to
 *  [ActorPlacementManager], since [AudioClipPlacement] is the audio counterpart to
 *  [com.donnclab.kinetiframe.core.document.ActorPlacementReference]. Decoding, recording and copying
 *  audio files happens at the UI layer, which has the Android Context; this class only receives
 *  an already-resolved [ProjectAsset] and placement, never a Uri or a path to copy. */
class AudioManager(
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
) {
    /** [asset] is inserted alongside [placement] in the same state update so a placement can
     *  never exist without its backing asset entry, even momentarily. */
    fun add(placement: AudioClipPlacement, asset: ProjectAsset) {
        require(placement.clipId !in getState().project.audioClips) { "Audio clip already exists" }
        require(asset.id == placement.assetId) { "Audio clip references a different asset than the one provided" }
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    audioClips = state.project.audioClips + (placement.clipId to placement),
                    assets = state.project.assets + (asset.id to asset),
                ),
            )
        }
    }

    /** The clip's own asset is intentionally left in [com.donnclab.kinetiframe.core.document.ProjectDocument.assets]
     *  -- unlike the single persistent background, a removed audio clip's source file is cheap to
     *  keep (an entry pointing at a file already in project-assets/audio/). Reclaiming storage
     *  belongs in a project-wide sweep of unused assets, not in clip removal. */
    fun remove(clipId: String) = updateState { state ->
        if (state.project.videoClips.values.any { it.linkedAudioClipId == clipId }) return@updateState state
        if (state.project.audioClips[clipId]?.let { state.project.audioTracks[it.trackIndex]?.isLocked } ==
            true
        ) {
            return@updateState state
        }
        state.copy(project = state.project.copy(audioClips = state.project.audioClips - clipId))
    }

    /** A trim changes the source window; a move changes only timeline placement. */
    fun trim(clipId: String, startFrame: Int, durationFrames: Int) {
        if (isLinked(clipId)) return
        val existing = getState().project.audioClips[clipId] ?: return
        if (isLocked(existing.trackIndex)) return
        replace(
            com.donnclab.kinetiframe.media.audio.AudioClipTrimming.shorten(
                existing,
                startFrame,
                durationFrames,
                getState().project.fps,
            ),
        )
    }

    /** Moves a clip along its lane. The lane UI only drags horizontally, so [trackIndex] is
     *  normally the clip's current track; it is accepted so moving between tracks needs no
     *  schema change. */
    fun move(clipId: String, trackIndex: Int, startFrame: Int) {
        if (isLinked(clipId)) return
        val existing = getState().project.audioClips[clipId] ?: return
        // Lock is enforced here, not merely drawn in the lane header: a guard that only dims a
        // button still lets a fling or a stray drag reach the manager and move the clip anyway.
        val safeTrack = trackIndex.coerceIn(0, MAX_AUDIO_TRACKS - 1)
        if (isLocked(existing.trackIndex) || isLocked(safeTrack)) return
        val safeStart = startFrame.coerceAtLeast(0)
        if (safeTrack == existing.trackIndex && safeStart == existing.startFrame) return
        replace(existing.copy(trackIndex = safeTrack, startFrame = safeStart))
    }

    fun setVolume(clipId: String, volume: Float) {
        val existing = getState().project.audioClips[clipId] ?: return
        replace(existing.copy(volume = volume.coerceIn(0f, 1f)))
    }

    /**
     * Copies a clip onto the first lane with room for it, starting where the original ends.
     *
     * Placed after the original rather than on top of it because a duplicate stacked at the same
     * start would play as one doubled, phase-smeared sound -- audible as a fault rather than as a
     * copy. Falls back to the original's own lane when every lane is occupied at that point, since
     * a refused duplicate with no feedback is worse than an overlap the animator can see and drag.
     */
    fun duplicate(clipId: String) {
        val source = getState().project.audioClips[clipId] ?: return
        val landingFrame = source.startFrame + source.durationFrames
        val lane = (0 until MAX_AUDIO_TRACKS).firstOrNull { candidate ->
            !isLocked(candidate) && getState().project.audioClips.values.none { other ->
                other.trackIndex == candidate &&
                    landingFrame < other.startFrame + other.durationFrames &&
                    other.startFrame < landingFrame + source.durationFrames
            }
        } ?: source.trackIndex
        if (isLocked(lane)) return
        val copy = source.copy(
            clipId = "audioclip-${java.util.UUID.randomUUID()}",
            trackIndex = lane,
            startFrame = landingFrame,
        )
        updateState { state ->
            state.copy(project = state.project.copy(audioClips = state.project.audioClips + (copy.clipId to copy)))
        }
    }

    /**
     * Cuts a clip in two at [atFrame], keeping both halves pointing at the same source file.
     *
     * The right-hand half inherits an in-point advanced by exactly the frames the left half kept,
     * which is what makes a split non-destructive: nothing is re-encoded, and butting the halves
     * back together reproduces the original audio sample-for-sample. Its waveform is re-sliced
     * proportionally from the peaks already stored, so neither half has to decode again.
     *
     * A cut on (or outside) either boundary is ignored -- it would produce a zero-length clip,
     * which the placement model rightly refuses.
     */
    fun split(clipId: String, atFrame: Int) {
        if (isLinked(clipId)) return
        val source = getState().project.audioClips[clipId] ?: return
        if (isLocked(source.trackIndex)) return
        val fps = getState().project.fps
        if (fps <= 0) return
        val leftFrames = atFrame - source.startFrame
        val rightFrames = source.durationFrames - leftFrames
        if (leftFrames < 1 || rightFrames < 1) return

        val splitRatio = leftFrames.toFloat() / source.durationFrames
        val peakCut = (source.waveformPeaks.size * splitRatio).toInt()
            .coerceIn(0, source.waveformPeaks.size)

        val left = source.copy(
            durationFrames = leftFrames,
            envelopeDurationFrames = source.envelopeDurationFrames.takeIf { it > 0 } ?: source.durationFrames,
            waveformPeaks = source.waveformPeaks.take(peakCut),
        )
        val right = source.copy(
            clipId = "audioclip-${java.util.UUID.randomUUID()}",
            startFrame = atFrame,
            durationFrames = rightFrames,
            envelopeDurationFrames = source.envelopeDurationFrames.takeIf { it > 0 } ?: source.durationFrames,
            envelopeOffsetFrames = source.envelopeOffsetFrames + leftFrames,
            trimStartMicros = source.trimStartMicros + leftFrames.toLong() * 1_000_000L / fps,
            trimOffsetFrames = if (source.trimSource != null) source.trimOffsetFrames + leftFrames else 0,
            waveformPeaks = source.waveformPeaks.drop(peakCut),
        )
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    audioClips = state.project.audioClips + (left.clipId to left) + (right.clipId to right),
                ),
            )
        }
    }

    /** Silences or restores a whole lane. Honoured by both the editor's monitoring
     *  ([com.donnclab.kinetiframe.studio.media.AudioPlaybackManager]) and the exported mix
     *  ([com.donnclab.kinetiframe.media.audio.AudioMixer]), so what the animator hears while working is
     *  what renders. */
    fun setTrackMuted(trackIndex: Int, muted: Boolean) = updateTrack(trackIndex) { it.copy(isMuted = muted) }

    /** Freezes a lane's clip positions and durations against further drags. Deliberately does not
     *  silence it -- a locked lane is one whose timing is settled, not one you have stopped
     *  listening to. */
    fun setTrackLocked(trackIndex: Int, locked: Boolean) = updateTrack(trackIndex) { it.copy(isLocked = locked) }

    private fun isLocked(trackIndex: Int) = getState().project.audioTracks[trackIndex]?.isLocked == true
    private fun isLinked(id: String) = getState().project.videoClips.values.any { it.linkedAudioClipId == id }

    private fun updateTrack(trackIndex: Int, transform: (AudioTrackSettings) -> AudioTrackSettings) {
        if (trackIndex !in 0 until MAX_AUDIO_TRACKS) return
        updateState { state ->
            val existing = state.project.audioTracks[trackIndex] ?: AudioTrackSettings()
            state.copy(
                project = state.project.copy(
                    audioTracks = state.project.audioTracks + (trackIndex to transform(existing)),
                ),
            )
        }
    }

    private fun replace(updated: AudioClipPlacement) = updateState { state ->
        state.copy(project = state.project.copy(audioClips = state.project.audioClips + (updated.clipId to updated)))
    }

    fun handle(intent: StudioIntent.Audio) {
        when (intent) {
            is StudioIntent.Audio.Add -> add(intent.placement, intent.asset)

            is StudioIntent.Audio.Remove -> remove(intent.clipId)

            is StudioIntent.Audio.Trim -> trim(intent.clipId, intent.startFrame, intent.durationFrames)

            is StudioIntent.Audio.Move -> move(intent.clipId, intent.trackIndex, intent.startFrame)

            is StudioIntent.Audio.SetVolume -> setVolume(intent.clipId, intent.volume)

            is StudioIntent.Audio.SetTrackMuted -> setTrackMuted(intent.trackIndex, intent.muted)

            is StudioIntent.Audio.SetTrackLocked -> setTrackLocked(intent.trackIndex, intent.locked)

            is StudioIntent.Audio.Duplicate -> duplicate(intent.clipId)

            is StudioIntent.Audio.Split -> split(intent.clipId, intent.atFrame)

            // Fades, crossfades and voice styles are scene effects, applied by SceneEffectsManager.
            is StudioIntent.Audio.VoiceStyle, is StudioIntent.Audio.Crossfade, is StudioIntent.Audio.SetFades -> Unit
        }
    }
}
