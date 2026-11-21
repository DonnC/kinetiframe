package com.donnclab.kinetiframe.media.audio

import com.donnclab.kinetiframe.core.document.AudioClipPlacement

/**
 * Pure frame<->audio-offset math, kept separate from the audio playback manager. It has no
 * Android runtime dependency, so a plain JVM unit test can exercise it directly rather than only
 * indirectly through `android.media.MediaPlayer` behavior.
 */
object AudioTrackSync {
    /** [clipId] is playing, and should be positioned at [offsetMicros] into its source file. */
    data class ActiveClip(val clipId: String, val offsetMicros: Long)

    /** Every clip in [clips] whose placed range covers [frame], with the exact offset into its
     *  source audio (honoring [AudioClipPlacement.trimStartMicros]) that frame corresponds to.
     *  Multiple clips can come back at once -- one per track that has a clip there -- since
     *  tracks mix together rather than being mutually exclusive. Returns nothing for a
     *  non-positive [fps] rather than dividing by zero. */
    fun activeClips(clips: Collection<AudioClipPlacement>, frame: Int, fps: Int): List<ActiveClip> {
        if (fps <= 0) return emptyList()
        return clips.filter { frame >= it.startFrame && frame < it.startFrame + it.durationFrames }
            .map { clip ->
                val framesIntoClip = (frame - clip.startFrame).toLong()
                val offsetMicros = clip.trimStartMicros + framesIntoClip * 1_000_000L / fps
                ActiveClip(clip.clipId, offsetMicros)
            }
    }
}
