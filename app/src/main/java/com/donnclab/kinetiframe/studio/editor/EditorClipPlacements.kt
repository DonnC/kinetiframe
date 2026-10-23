// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.audio.AudioTrimSelection
import com.donnclab.kinetiframe.audio.Waveform
import com.donnclab.kinetiframe.project.model.ActorPlacementReference
import com.donnclab.kinetiframe.project.model.AssetKind
import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import com.donnclab.kinetiframe.project.model.MAX_AUDIO_TRACKS
import com.donnclab.kinetiframe.project.model.ProjectAsset
import com.donnclab.kinetiframe.project.model.VideoClipPlacement
import com.donnclab.kinetiframe.project.model.VideoFit
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import java.util.UUID
import kotlin.math.ceil

/**
 * Places an audio file on the first audio track with nothing on it yet, at the current frame. Once
 * every track is in use it falls back to track 0: clips may overlap on one track.
 *
 * [peaks] is the clip's waveform outline. A library item passes none.
 */
internal fun StudioState.addAudioClip(
    assetId: String,
    path: String,
    durationMicros: Long,
    label: String,
    peaks: List<Float> = emptyList(),
): StudioIntent.Audio.Add {
    val placement = AudioClipPlacement(
        clipId = "audioclip-${UUID.randomUUID()}",
        assetId = assetId,
        trackIndex = firstFreeAudioTrack(),
        label = label,
        startFrame = animation.currentFrame,
        durationFrames = framesFor(durationMicros),
        waveformPeaks = peaks,
        // An import is placed whole, so its source length and its placed length are the same.
        // Recording it anyway means the trim editor can widen it back later.
        sourceDurationMicros = durationMicros,
    )
    return StudioIntent.Audio.Add(placement, audioAsset(assetId, path))
}

/**
 * Places a recorded take using the in and out points chosen on the trim screen.
 *
 * The source file is stored whole and the in-point recorded as trimStartMicros rather than the
 * audio being cut on disk, so widening the clip later reaches real audio instead of silence.
 * [sourcePeaks] is the take's full waveform; only the selected slice is stored on the clip.
 */
internal fun StudioState.addTrimmedAudioClip(
    assetId: String,
    path: String,
    selection: AudioTrimSelection,
    sourcePeaks: List<Float>,
): StudioIntent.Audio.Add {
    val placement = AudioClipPlacement(
        clipId = "audioclip-${UUID.randomUUID()}",
        assetId = assetId,
        trackIndex = firstFreeAudioTrack(),
        label = "Voice-over",
        startFrame = animation.currentFrame,
        durationFrames = selection.toDurationFrames(project.fps),
        trimStartMicros = selection.inMicros,
        waveformPeaks = selection.slice(Waveform(sourcePeaks)).peaks,
        sourceDurationMicros = selection.sourceDurationMicros,
    )
    return StudioIntent.Audio.Add(placement, audioAsset(assetId, path))
}

/**
 * Appends an imported video after whatever already occupies the single video track, centred on
 * the stage at half its size. Its sound goes on the first unlocked audio lane, if there is one.
 */
internal fun StudioState.addVideoClip(video: ImportedVideo): StudioIntent.Video.Add {
    val metadata = video.metadata
    val durationMicros = metadata.durationMicros
    val sourceWidth = metadata.width
    val sourceHeight = metadata.height
    val durationFrames = framesFor(durationMicros)
    // The next free slot at the tail of the video track, as VideoTrackManager.nextFreeStartFrame
    // computes it.
    val startFrame = maxOf(animation.currentFrame, project.videoClips.values.maxOfOrNull { it.timelineEndFrame } ?: 0)
    val scale = minOf(project.stage.width * .5f / sourceWidth, project.stage.height * .5f / sourceHeight)
    val width = sourceWidth * scale
    val height = sourceHeight * scale
    val asset = ProjectAsset(
        id = video.assetId,
        kind = AssetKind.VIDEO,
        sourcePath = video.file.absolutePath,
        width = sourceWidth,
        height = sourceHeight,
    )
    val audioLane = (0 until MAX_AUDIO_TRACKS).firstOrNull { project.audioTracks[it]?.isLocked != true }
    val audio = if (metadata.hasAudio && audioLane != null) {
        AudioClipPlacement(
            "audioclip-${UUID.randomUUID()}",
            video.assetId,
            audioLane,
            "Video sound",
            startFrame,
            durationFrames,
            sourceDurationMicros = durationMicros,
            waveformPeaks = metadata.peaks,
        )
    } else {
        null
    }
    val placement = VideoClipPlacement(
        clipId = "videoclip-${UUID.randomUUID()}",
        assetId = video.assetId,
        label = "Imported video",
        timelineStartFrame = startFrame,
        timelineEndFrame = startFrame + durationFrames,
        sourceDurationFrames = durationFrames,
        isSceneContent = true,
        x = (project.stage.width - width) / 2f,
        y = (project.stage.height - height) / 2f,
        width = width,
        height = height,
        fit = VideoFit.FIT,
        linkedAudioClipId = audio?.clipId,
    )
    return StudioIntent.Video.Add(placement, asset, audio)
}

/** A new placement of [performance] at the current frame, lasting as long as the performance. */
internal fun StudioState.placementFor(performance: PerformanceDocument): ActorPlacementReference =
    ActorPlacementReference(
        placementId = "placement-${UUID.randomUUID()}",
        performanceId = performance.performanceId,
        actorId = performance.actorId,
        actorRevisionId = performance.actorRevisionId,
        label = performance.displayName,
        startFrame = animation.currentFrame,
        durationFrames = framesFor(performance.durationMicros),
    )

private fun StudioState.framesFor(durationMicros: Long): Int =
    ceil(durationMicros * project.fps / 1_000_000.0).toInt().coerceAtLeast(1)

private fun StudioState.firstFreeAudioTrack(): Int {
    val usedTracks = project.audioClips.values.map { it.trackIndex }.toSet()
    return (0 until MAX_AUDIO_TRACKS).firstOrNull { it !in usedTracks } ?: 0
}

private fun audioAsset(assetId: String, path: String) =
    ProjectAsset(id = assetId, kind = AssetKind.AUDIO, sourcePath = path, width = 0, height = 0)
