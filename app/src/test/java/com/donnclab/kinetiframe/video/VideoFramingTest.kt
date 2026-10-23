package com.donnclab.kinetiframe.video

import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.core.document.VideoFit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VideoFramingTest {
    private val clip = VideoClipPlacement("video", "source", "Video", 0, 24, width = 100f, height = 100f)

    @Test fun `fit preserves aspect ratio with letterbox`() {
        val framing = VideoFraming.resolve(clip.copy(fit = VideoFit.FIT), 200, 100)
        assertEquals(VideoFraming.Rect(0f, 25f, 100f, 75f), framing.destination)
        assertEquals(VideoFraming.Rect(0f, 0f, 200f, 100f), framing.source)
    }

    @Test fun `fill crops source instead of stretching and finger can choose either edge`() {
        assertEquals(
            VideoFraming.Rect(0f, 0f, 100f, 100f),
            VideoFraming.resolve(clip.copy(fit = VideoFit.FILL, cropX = 0f), 200, 100).source,
        )
        assertEquals(
            VideoFraming.Rect(100f, 0f, 200f, 100f),
            VideoFraming.resolve(clip.copy(fit = VideoFit.FILL, cropX = 1f), 200, 100).source,
        )
    }

    @Test fun `linked audio uses exactly the video source and timeline clocks`() {
        val video = clip.copy(
            timelineStartFrame = 12,
            timelineEndFrame = 24,
            videoSourceOffsetSeconds = 1.0,
            sourceDurationFrames = 24,
            sourceOffsetFrames = 12,
        )
        val audio = AudioClipPlacement("audio", "source", 0, "Sound", 0, 24, waveformPeaks = listOf(.1f, .2f, .3f, .4f))
        val synced = VideoAudioLink.synchronize(video, audio, 12)
        assertEquals(12, synced.startFrame)
        assertEquals(12, synced.durationFrames)
        assertEquals(1_000_000L, synced.trimStartMicros)
        assertEquals(listOf(.3f, .4f), synced.waveformPeaks)
    }
}
