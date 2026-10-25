package com.donnclab.kinetiframe.studio.domain

import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManagerTest {
    @TempDir lateinit var directory: File

    @Test fun `slow renders cannot accumulate jobs and empty active frames still compose`() = runTest {
        var state =
            StudioState(
                project = com.donnclab.kinetiframe.core.document.ProjectDocument(fps = 24),
                animation = AnimationState(totalFrames = 144),
            )
        val gate = CompletableDeferred<Unit>()
        var renders = 0
        val audioFrames = mutableListOf<Int>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = PlaybackManager(
            backgroundScope, dispatcher, AnimationEngine(backgroundScope, dispatcher),
            FrameCacheManager(directory, backgroundScope), { state = it(state) }, {},
            { _, bitmap ->
                assertNull(bitmap)
                renders++
                gate.await()
                null
            }, { "empty" },
            onFrameChanged = { audioFrames += it },
            clock = com.donnclab.kinetiframe.studio.domain.PlaybackClock {
                testScheduler.currentTime *
                    1_000_000
            },
        )
        manager.play(state)
        manager.incrementFrame(state)
        runCurrent()
        advanceTimeBy(1_000)
        repeat(100) { manager.incrementFrame(state) }
        runCurrent()
        assertEquals(1, renders)
        assertEquals(24, state.animation.playbackFrame)
        gate.complete(Unit)
        runCurrent()
        assertEquals(24, state.animation.playbackFrame)
        assertEquals(listOf(0, 24), audioFrames)
        manager.pause(state)
    }

    @Test fun `pause cancels pending frame publication and audio restart`() = runTest {
        var state = StudioState(animation = AnimationState(totalFrames = 144))
        val gate = CompletableDeferred<Unit>()
        val audioFrames = mutableListOf<Int>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = PlaybackManager(
            backgroundScope, dispatcher, AnimationEngine(backgroundScope, dispatcher),
            FrameCacheManager(directory, backgroundScope), { state = it(state) }, {},
            { _, _ ->
                gate.await()
                null
            }, {
                "main"
            }, onFrameChanged = { audioFrames += it },
            clock = com.donnclab.kinetiframe.studio.domain.PlaybackClock {
                testScheduler.currentTime *
                    1_000_000
            },
        )
        manager.play(state)
        manager.incrementFrame(state)
        runCurrent()
        manager.pause(state)
        gate.complete(Unit)
        runCurrent()
        assertFalse(state.animation.isPlaying)
        assertEquals(0, state.animation.playbackFrame)
        assertEquals(listOf(0), audioFrames)
    }

    @Test fun `render failure stops playback and exposes the error instead of crashing scope`() = runTest {
        var state = StudioState(animation = AnimationState(totalFrames = 144))
        var reported: Exception? = null
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = PlaybackManager(
            backgroundScope, dispatcher, AnimationEngine(backgroundScope, dispatcher),
            FrameCacheManager(directory, backgroundScope), { state = it(state) }, {},
            { _, _ -> throw java.io.IOException("broken frame") }, { "main" },
            onPlaybackError = { reported = it },
            clock = com.donnclab.kinetiframe.studio.domain.PlaybackClock {
                testScheduler.currentTime *
                    1_000_000
            },
        )
        manager.play(state)
        manager.incrementFrame(state)
        runCurrent()
        assertFalse(state.animation.isPlaying)
        assertEquals("broken frame", reported?.message)
    }

    @Test fun `last frame loops to zero without advancing editing frame`() = runTest {
        var state =
            StudioState(
                project = com.donnclab.kinetiframe.core.document.ProjectDocument(fps = 24),
                animation = AnimationState(totalFrames = 144, currentFrame = 143),
            )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = PlaybackManager(
            backgroundScope, dispatcher, AnimationEngine(backgroundScope, dispatcher),
            FrameCacheManager(directory, backgroundScope), {
                state = it(state)
            }, {}, { _, _ -> null }, { "main" },
            clock = com.donnclab.kinetiframe.studio.domain.PlaybackClock {
                testScheduler.currentTime *
                    1_000_000
            },
        )
        manager.play(state)
        advanceTimeBy(42)
        manager.incrementFrame(state)
        runCurrent()
        assertEquals(0, state.animation.playbackFrame)
        assertEquals(143, state.animation.currentFrame)
        manager.pause(state)
    }
}
