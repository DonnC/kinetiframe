package com.donnclab.kinetiframe.media.video

import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.media.audio.AudioTrackSync

/** Pure frame -> active-clip lookup for the single video reference track, mirroring
 *  [AudioTrackSync]'s shape. Since the track is single-lane and clips are enforced
 *  non-overlapping (see [VideoTrackManager]), at most one clip is ever active at a given frame --
 *  unlike audio's simultaneous-multi-track [AudioTrackSync.activeClips]. */
object VideoTrackSync {
    data class ActiveClip(val clipId: String, val sourceOffsetSeconds: Double)

    /** [frame] within a clip's [clip.timelineStartFrame, clip.timelineEndFrame} range maps to a source-file offset
     *  of [VideoClipPlacement.videoSourceOffsetSeconds] plus however many seconds into the clip
     *  the frame is, at [fps]. Returns null outside every clip's range, or for a non-positive fps
     *  (division by it would be meaningless). */
    fun activeClip(clips: Collection<VideoClipPlacement>, frame: Int, fps: Int): ActiveClip? {
        if (fps <= 0) return null
        val clip = clips.firstOrNull { frame in it.timelineStartFrame until it.timelineEndFrame } ?: return null
        val framesIntoClip = frame - clip.timelineStartFrame
        val offsetSeconds = clip.videoSourceOffsetSeconds + framesIntoClip.toDouble() / fps
        return ActiveClip(clip.clipId, offsetSeconds)
    }
}
