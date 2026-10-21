package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.camera.CameraShot
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.ui.TimelineState

/** Bounded non-destructive effect history. Retains references, not decoded media or frame pixels.
 * Undo restores only effect/media references; unrelated drawings, actors and viewport survive. */
class SceneEffectsManager(
    private val getState: () -> TimelineState,
    private val updateState: ((TimelineState) -> TimelineState) -> Unit,
) {
    private data class Snapshot(
        val projectId: String,
        val shots: List<CameraShot>,
        val audio: Map<String, com.donnclab.kinetiframe.engine.model.AudioClipPlacement>,
        val video: Map<String, com.donnclab.kinetiframe.engine.model.VideoClipPlacement>,
    )
    private val undo = ArrayDeque<Snapshot>()
    private val redo = ArrayDeque<Snapshot>()
    private var projectId = ""

    /** Other media mutations invalidate effect history rather than undoing later unrelated edits. */
    fun clear() {
        undo.clear()
        redo.clear()
    }
    private fun prepare() {
        val id = getState().project.id
        if (id != projectId) {
            undo.clear()
            redo.clear()
            projectId = id
        }
    }
    private fun snapshot(project: ProjectDocument) =
        Snapshot(project.id, project.cameraShots, project.audioClips, project.videoClips)
    fun edit(change: (ProjectDocument) -> ProjectDocument) {
        prepare()
        val old = getState().project
        val next = change(old)
        if (next == old) return
        if (undo.size == 32) undo.removeFirst()
        undo.addLast(snapshot(old))
        redo.clear()
        updateState { it.copy(project = next) }
    }
    fun put(shot: CameraShot) = edit { project ->
        project.copy(
            cameraShots = (
                project.cameraShots.filterNot {
                    it.id == shot.id
                } + shot
                ).sortedBy { it.startFrame },
        )
    }
    fun remove(id: String) = edit { it.copy(cameraShots = it.cameraShots.filterNot { shot -> shot.id == id }) }
    fun voiceStyle(
        id: String,
        expected: String,
        effect: com.donnclab.kinetiframe.engine.audio.VoiceEffect,
        proxy: com.donnclab.kinetiframe.engine.model.ProjectAsset?,
    ) = edit { project ->
        val clip = project.audioClips[id] ?: return@edit project
        if (clip.assetId != expected || project.audioTracks[clip.trackIndex]?.isLocked == true) return@edit project
        val original = clip.originalAssetId ?: clip.assetId
        val target = if (effect ==
            com.donnclab.kinetiframe.engine.audio.VoiceEffect.ORIGINAL
        ) {
            original
        } else {
            proxy?.id ?: return@edit project
        }
        project.copy(
            audioClips =
            project.audioClips + (id to clip.copy(assetId = target, originalAssetId = original, voiceEffect = effect)),
            assets = if (proxy == null) project.assets else project.assets + (proxy.id to proxy),
        )
    }
    fun videoFraming(id: String, fit: com.donnclab.kinetiframe.engine.model.VideoFit, x: Float, y: Float) =
        edit { project ->
            val clip = project.videoClips[id] ?: return@edit project
            project.copy(
                videoClips =
                project.videoClips +
                    (id to clip.copy(fit = fit, cropX = x.coerceIn(0f, 1f), cropY = y.coerceIn(0f, 1f))),
            )
        }
    fun crossfade(id: String, nextId: String, frames: Int) = edit { project ->
        val a = project.audioClips[id] ?: return@edit project
        val b = project.audioClips[nextId] ?: return@edit project
        if (project.videoClips.values.any {
                it.linkedAudioClipId == id || it.linkedAudioClipId == nextId
            }
        ) {
            return@edit project
        }
        if (project.audioTracks[a.trackIndex]?.isLocked == true || a.trackIndex != b.trackIndex ||
            b.startFrame < a.startFrame + a.durationFrames
        ) {
            return@edit project
        }
        val (first, second) = com.donnclab.kinetiframe.engine.audio.AudioCrossfade.apply(a, b, frames)
        if (project.audioClips.values.any {
                it.clipId != id && it.clipId != nextId && it.trackIndex == second.trackIndex &&
                    second.startFrame < it.startFrame + it.durationFrames &&
                    it.startFrame < second.startFrame + second.durationFrames
            }
        ) {
            return@edit project
        }
        project.copy(audioClips = project.audioClips + (id to first) + (nextId to second))
    }
    fun fades(id: String, incoming: Int, outgoing: Int) = edit { project ->
        val clip = project.audioClips[id] ?: return@edit project
        if (project.audioTracks[clip.trackIndex]?.isLocked == true) return@edit project
        val next = clip.copy(
            fadeInFrames = incoming.coerceIn(0, clip.durationFrames),
            fadeOutFrames = outgoing.coerceIn(0, clip.durationFrames),
            envelopeOffsetFrames = 0,
            envelopeDurationFrames = clip.durationFrames,
        )
        project.copy(audioClips = project.audioClips + (id to next))
    }
    fun splitVideo(id: String, frame: Int) = edit { project ->
        val clip = project.videoClips[id] ?: return@edit project
        if (frame <= clip.timelineStartFrame || frame >= clip.timelineEndFrame) return@edit project
        val audio = clip.linkedAudioClipId?.let { project.audioClips[it] }
        if (audio != null && project.audioTracks[audio.trackIndex]?.isLocked == true) return@edit project
        val cut = frame - clip.timelineStartFrame
        val span =
            clip.sourceDurationFrames.takeIf { it > 0 }
                ?: clip.timelineEndFrame - clip.timelineStartFrame + clip.sourceOffsetFrames
        val left = clip.copy(timelineEndFrame = frame, sourceDurationFrames = span)
        val right = clip.copy(
            clipId = "videoclip-${java.util.UUID.randomUUID()}",
            timelineStartFrame = frame,
            linkedAudioClipId = audio?.let { "audioclip-${java.util.UUID.randomUUID()}" },
            videoSourceOffsetSeconds = clip.videoSourceOffsetSeconds + cut.toDouble() / project.fps,
            sourceDurationFrames = span,
            sourceOffsetFrames = clip.sourceOffsetFrames + cut,
        )
        val audioClips = if (audio == null) {
            project.audioClips
        } else {
            project.audioClips +
                (
                    audio.clipId to com.donnclab.kinetiframe.engine.video.VideoAudioLink.synchronize(
                        left,
                        audio,
                        project.fps,
                    )
                    ) +
                (
                    requireNotNull(right.linkedAudioClipId) to
                        com.donnclab.kinetiframe.engine.video.VideoAudioLink.synchronize(
                            right,
                            audio.copy(clipId = requireNotNull(right.linkedAudioClipId)),
                            project.fps,
                        )
                    )
        }
        project.copy(videoClips = project.videoClips + (id to left) + (right.clipId to right), audioClips = audioClips)
    }
    fun undo() = restore(undo, redo)
    fun redo() = restore(redo, undo)
    private fun restore(from: ArrayDeque<Snapshot>, to: ArrayDeque<Snapshot>) {
        prepare()
        val target = from.removeLastOrNull() ?: return
        to.addLast(snapshot(getState().project))
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    cameraShots = target.shots,
                    audioClips = target.audio,
                    videoClips = target.video,
                ),
            )
        }
    }
}
