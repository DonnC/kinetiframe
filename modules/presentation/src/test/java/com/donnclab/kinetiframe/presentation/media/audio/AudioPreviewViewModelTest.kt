package com.donnclab.kinetiframe.presentation.media.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

@OptIn(ExperimentalCoroutinesApi::class)
class AudioPreviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    @Test fun `load plays pauses seeks and releases on leaving preview`() = runTest(dispatcher) {
        val player = FakePlayer()
        val model = AudioPreviewViewModel(player)
        model.handleIntent(AudioPreviewIntent.Load("sound.wav"))
        runCurrent()
        assertTrue(model.state.value.ready)
        model.handleIntent(AudioPreviewIntent.Toggle)
        assertTrue(player.playing)
        model.handleIntent(AudioPreviewIntent.Seek(5000))
        assertEquals(2400, player.positionMs)
        model.handleIntent(AudioPreviewIntent.Toggle)
        assertFalse(model.state.value.playing)
        model.handleIntent(AudioPreviewIntent.Stop)
        assertFalse(model.state.value.ready)
        assertFalse(player.playing)
        assertTrue(player.releases >= 2)
    }

    @Test fun `completion resets playhead on replay`() = runTest(dispatcher) {
        val player = FakePlayer()
        val model = AudioPreviewViewModel(player)
        model.handleIntent(AudioPreviewIntent.Load("sound.wav"))
        runCurrent()
        model.handleIntent(AudioPreviewIntent.Toggle)
        runCurrent()
        player.playing = false
        advanceTimeBy(110)
        runCurrent()
        assertEquals(2400, model.state.value.positionMs)
        model.handleIntent(AudioPreviewIntent.Toggle)
        assertEquals(0, player.positionMs)
        model.handleIntent(AudioPreviewIntent.Stop)
    }

    @Test fun `prepare failure is visible and transport stays disabled`() = runTest(dispatcher) {
        val player = FakePlayer(fail = true)
        val model = AudioPreviewViewModel(player)
        model.handleIntent(AudioPreviewIntent.Load("missing.wav"))
        runCurrent()
        assertNotNull(model.state.value.error)
        model.handleIntent(AudioPreviewIntent.Toggle)
        assertFalse(player.playing)
    }

    @Test fun `stop cancels an unfinished preparation`() = runTest(dispatcher) {
        val player = FakePlayer(prepareDelay = 1000)
        val model = AudioPreviewViewModel(player)
        model.handleIntent(AudioPreviewIntent.Load("sound.wav"))
        runCurrent()
        model.handleIntent(AudioPreviewIntent.Stop)
        advanceTimeBy(1500)
        runCurrent()
        assertFalse(model.state.value.ready)
        assertFalse(model.state.value.loading)
    }

    private class FakePlayer(val fail: Boolean = false, val prepareDelay: Long = 0) : PreviewAudioPlayer {
        override var positionMs = 0
        override var playing = false
        var releases = 0
        override suspend fun prepare(path: String): Int {
            delay(prepareDelay)
            check(!fail)
            return 2400
        }
        override fun play() {
            playing = true
        }
        override fun pause() {
            playing = false
        }
        override fun seek(positionMs: Int) {
            this.positionMs = positionMs
        }
        override fun release() {
            releases++
            playing = false
        }
    }
}
