package com.donnclab.kinetiframe.studio.domain

import android.media.MediaPlayer
import com.donnclab.kinetiframe.audio.AudioPlaybackManager
import com.donnclab.kinetiframe.project.model.AssetKind
import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import com.donnclab.kinetiframe.project.model.ProjectAsset
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.StudioState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AudioPlaybackLifecycleTest {
    private class Player(private val failSource: Boolean = false) : MediaPlayer() {
        var preparedCallback: OnPreparedListener? = null
        var errorCallback: OnErrorListener? = null
        var seekCallback: OnSeekCompleteListener? = null
        var completeSeeks = true
        var playing = false
        var pauses = 0
        var starts = 0
        var releases = 0
        var seekMillis = -1L
        var gain = -1f
        var gainUpdates = 0

        // MediaPlayer's constructor may invoke overridable methods before subclass fields init.
        var accessThreads: MutableSet<Thread>? = null
        private fun access() {
            val threads = accessThreads ?: mutableSetOf<Thread>().also { accessThreads = it }
            threads += Thread.currentThread()
        }
        override fun setDataSource(path: String) {
            access()
            if (failSource) throw java.io.IOException("missing source")
        }
        override fun setOnPreparedListener(listener: OnPreparedListener?) {
            access()
            preparedCallback = listener
        }
        override fun setOnErrorListener(listener: OnErrorListener?) {
            access()
            errorCallback = listener
        }
        override fun prepareAsync() {
            access()
        }
        override fun setVolume(leftVolume: Float, rightVolume: Float) {
            access()
            gain = leftVolume
            gainUpdates++
        }
        override fun isPlaying(): Boolean {
            access()
            return playing
        }
        override fun getCurrentPosition(): Int {
            access()
            return seekMillis.coerceAtLeast(0).toInt()
        }
        override fun setOnSeekCompleteListener(listener: OnSeekCompleteListener?) {
            access()
            seekCallback = listener
        }
        override fun seekTo(msec: Long, mode: Int) {
            access()
            seekMillis = msec
            if (completeSeeks) seekCallback?.onSeekComplete(this)
        }
        override fun start() {
            access()
            starts++
            playing = true
        }
        override fun pause() {
            access()
            pauses++
            playing = false
        }
        override fun release() {
            access()
            releases++
        }
    }

    private fun state() = StudioState(
        project = ProjectDocument(
            fps = 24,
            assets = mapOf("sfx" to ProjectAsset("sfx", AssetKind.AUDIO, "demo.wav", 0, 0)),
            audioClips = mapOf("clip" to AudioClipPlacement("clip", "sfx", 0, "SFX", 0, 144)),
        ),
        animation = AnimationState(totalFrames = 144, isPlaying = true),
    )

    @Test fun replacingSoundSourceReleasesTheOldDecoderEvenWithTheSameClipId() = runTest {
        val players = mutableListOf<Player>()
        var current = state()
        val manager = AudioPlaybackManager({ current }, backgroundScope, { Player().also { players += it } })
        manager.onFrameChanged(0)
        runCurrent()
        players[0].preparedCallback!!.onPrepared(players[0])
        runCurrent()
        current =
            current.copy(
                project = current.project.copy(
                    assets = mapOf("sfx" to ProjectAsset("sfx", AssetKind.AUDIO, "processed.wav", 0, 0)),
                ),
            )
        manager.onFrameChanged(1)
        runCurrent()
        assertEquals(2, players.size)
        assertEquals(1, players[0].releases)
        manager.release()
        assertEquals(1, players[1].releases)
    }

    @Test fun fadesFollowMediaClockBetweenAnimationTicksAndStopAfterRelease() = runTest {
        val player = Player()
        val state = state().let { current ->
            current.copy(
                project = current.project.copy(
                    audioClips =
                    current.project.audioClips.mapValues { (_, clip) -> clip.copy(fadeInFrames = 24) },
                ),
            )
        }
        val manager = AudioPlaybackManager({ state }, backgroundScope, { player })
        manager.onFrameChanged(0)
        runCurrent()
        player.preparedCallback!!.onPrepared(player)
        runCurrent()
        assertEquals(0f, player.gain, .0001f)
        player.seekMillis = 500L
        advanceTimeBy(17)
        runCurrent()
        assertEquals(.5f, player.gain, .0001f)
        manager.release()
        val stopped = player.gainUpdates
        advanceTimeBy(100)
        runCurrent()
        assertEquals(stopped, player.gainUpdates)
        assertEquals(1, player.releases)
    }

    @Test fun preparationIsNonblockingAndSeeksTheLatestFrameOnOneThread() = runTest {
        val player = Player()
        val manager = AudioPlaybackManager({ state() }, backgroundScope, { player })
        manager.onFrameChanged(0)
        runCurrent()
        assertEquals(0, player.starts)
        manager.onFrameChanged(24)
        player.preparedCallback!!.onPrepared(player)
        runCurrent()
        assertEquals(1000L, player.seekMillis)
        assertEquals(1, player.starts)
        manager.onPlaybackStateChanged(false)
        assertEquals(1, player.releases)
        assertEquals(1, player.accessThreads!!.size)
    }

    @Test fun pauseWhilePreparingReleasesAndLateCallbackCannotRestartSound() = runTest {
        val player = Player()
        val manager = AudioPlaybackManager({ state() }, backgroundScope, { player })
        manager.onFrameChanged(0)
        runCurrent()
        manager.onPlaybackStateChanged(false)
        player.preparedCallback!!.onPrepared(player)
        runCurrent()
        assertEquals(0, player.starts)
        assertEquals(1, player.releases)
    }

    @Test fun sourceFailureReleasesThePlayerAndDoesNotRetryEveryTick() = runTest {
        val player = Player(failSource = true)
        val errors = mutableListOf<Throwable>()
        var creations = 0
        val manager = AudioPlaybackManager({ state() }, backgroundScope, {
            creations++
            player
        }, { errors += it })
        manager.onFrameChanged(0)
        runCurrent()
        repeat(24) { manager.onFrameChanged(it) }
        runCurrent()
        assertEquals(1, player.releases)
        assertEquals(1, creations)
        assertEquals("missing source", errors.single().message)
    }

    @Test fun nativeDecoderErrorAfterPreparationReleasesWithoutCrashing() = runTest {
        val player = Player()
        val errors = mutableListOf<Throwable>()
        val manager = AudioPlaybackManager({ state() }, backgroundScope, { player }, { errors += it })
        manager.onFrameChanged(0)
        runCurrent()
        player.preparedCallback!!.onPrepared(player)
        runCurrent()
        assertTrue(player.errorCallback!!.onError(player, 1, -1004))
        manager.onFrameChanged(1)
        runCurrent()
        assertEquals(1, player.starts)
        assertEquals(1, player.releases)
        assertEquals(1, errors.size)
    }

    @Test fun scrubWaitsForLatestSeekAndSupportsAdjacentAndBackwardFrames() = runTest {
        val player = Player().apply { completeSeeks = false }
        val paused = state().let { it.copy(animation = it.animation.copy(isPlaying = false)) }
        val manager = AudioPlaybackManager({ paused }, backgroundScope, { player })
        manager.onFrameChanged(24)
        runCurrent()
        player.preparedCallback!!.onPrepared(player)
        runCurrent()
        assertEquals(0, player.starts)
        manager.onFrameChanged(25)
        manager.onFrameChanged(23)
        player.seekCallback!!.onSeekComplete(player)
        assertEquals(1, player.starts)
        runCurrent()
        advanceTimeBy(36)
        runCurrent()
        assertEquals(958L, player.seekMillis)
        player.seekCallback!!.onSeekComplete(player)
        assertEquals(2, player.starts)
        advanceTimeBy(141)
        runCurrent()
        assertFalse(player.playing)
        assertEquals(0, player.releases)
        manager.onFrameChanged(24)
        assertEquals(1000L, player.seekMillis)
        player.seekCallback!!.onSeekComplete(player)
        assertEquals(3, player.starts)
        advanceTimeBy(2141)
        runCurrent()
        assertEquals(1, player.releases)
    }

    @Test fun latePreparationAfterScrubDeadlineDoesNotAuditionOldPosition() = runTest {
        val player = Player()
        val paused = state().let { it.copy(animation = it.animation.copy(isPlaying = false)) }
        val manager = AudioPlaybackManager({ paused }, backgroundScope, { player })
        manager.onFrameChanged(0)
        runCurrent()
        advanceTimeBy(141)
        runCurrent()
        player.preparedCallback!!.onPrepared(player)
        runCurrent()
        assertEquals(0, player.starts)
        manager.onFrameChanged(12)
        assertEquals(500L, player.seekMillis)
        assertEquals(1, player.starts)
        manager.release()
    }

    @Test fun lateSeekCompletionAfterScrubTimeoutCannotRestartSound() = runTest {
        val player = Player().apply { completeSeeks = false }
        val paused = state().let { it.copy(animation = it.animation.copy(isPlaying = false)) }
        val manager = AudioPlaybackManager({ paused }, backgroundScope, { player })
        manager.onFrameChanged(0)
        runCurrent()
        player.preparedCallback!!.onPrepared(player)
        runCurrent()
        advanceTimeBy(141)
        runCurrent()
        player.seekCallback!!.onSeekComplete(player)
        assertEquals(0, player.starts)
        manager.release()
    }
}
