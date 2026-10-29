package com.donnclab.kinetiframe.presentation.studio.color

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenColorPickerTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    private class Pixels(
        override val sourceWidth: Int = 100,
        override val sourceHeight: Int = 200,
        override val width: Int = 10,
        override val height: Int = 20,
    ) : ScreenColorSnapshot {
        var closed = false
        override fun pixel(x: Int, y: Int): Int {
            check(!closed)
            return y * width + x
        }
        override fun close() {
            closed = true
        }
    }

    @Test fun `capture preserves ordinary resolution and bounds large or extreme aspect ratios`() {
        assertEquals(1080 to 2340, screenCaptureSize(1080, 2340))
        for ((w, h) in listOf(8000 to 6000, 1 to Int.MAX_VALUE, Int.MAX_VALUE to 1, Int.MAX_VALUE to Int.MAX_VALUE)) {
            val (x, y) = screenCaptureSize(w, h)
            assertTrue(x > 0 && y > 0 && x <= w && y <= h)
            assertTrue(x.toLong() * y <= 4_000_000)
        }
        assertEquals(1 to 1, screenCaptureSize(500, 300, 1))
        assertThrows(IllegalArgumentException::class.java) { screenCaptureSize(0, 100) }
    }

    @Test fun `sampling maps window coordinates and clamps outside edges`() {
        val pixels = Pixels()
        assertEquals(105, sampleScreenColor(pixels, 55f, 105f))
        assertEquals(0, sampleScreenColor(pixels, -20f, -30f))
        assertEquals(199, sampleScreenColor(pixels, 100f, 200f))
        assertThrows(IllegalArgumentException::class.java) { sampleScreenColor(pixels, Float.NaN, 1f) }
    }

    @Test fun `confirm emits sampled value only and releases snapshot`() = runTest(dispatcher) {
        val model = ScreenColorPickerViewModel()
        val pixels = Pixels()
        model.handleIntent(ScreenColorIntent.Begin(ScreenColorSource { pixels }))
        runCurrent()
        assertTrue(model.state.value.active)
        model.handleIntent(ScreenColorIntent.Confirm)
        assertFalse(pixels.closed)
        model.handleIntent(ScreenColorIntent.Point(55f, 105f))
        model.handleIntent(ScreenColorIntent.Confirm)
        assertEquals(ScreenColorEffect.Selected(105), model.effect.first())
        assertTrue(pixels.closed)
        assertEquals(ScreenColorState(), model.state.value)
    }

    @Test fun `cancel releases image and new capture never reuses last sampled color`() = runTest(dispatcher) {
        val model = ScreenColorPickerViewModel()
        val pixels = Pixels()
        model.handleIntent(ScreenColorIntent.Begin(ScreenColorSource { pixels }))
        runCurrent()
        model.handleIntent(ScreenColorIntent.Point(55f, 105f))
        model.handleIntent(ScreenColorIntent.Point(Float.NaN, 1f))
        assertEquals(105, model.state.value.color)
        model.handleIntent(ScreenColorIntent.Cancel)
        assertTrue(pixels.closed)
        model.handleIntent(ScreenColorIntent.Begin(ScreenColorSource { Pixels() }))
        runCurrent()
        assertNull(model.state.value.color)
        model.handleIntent(ScreenColorIntent.Cancel)
    }

    @Test fun `cancelling pending capture cannot reactivate overlay`() = runTest(dispatcher) {
        val model = ScreenColorPickerViewModel()
        model.handleIntent(
            ScreenColorIntent.Begin(
                ScreenColorSource {
                    delay(1000)
                    Pixels()
                },
            ),
        )
        runCurrent()
        assertTrue(model.state.value.capturing)
        model.handleIntent(ScreenColorIntent.Cancel)
        advanceUntilIdle()
        assertEquals(ScreenColorState(), model.state.value)
    }

    @Test fun `capture failures are recoverable including low memory`() = runTest(dispatcher) {
        val model = ScreenColorPickerViewModel()
        model.handleIntent(ScreenColorIntent.Begin(ScreenColorSource { throw IllegalStateException() }))
        runCurrent()
        assertNotNull(model.state.value.error)
        assertFalse(model.state.value.active)
        model.handleIntent(ScreenColorIntent.Begin(ScreenColorSource { throw OutOfMemoryError() }))
        runCurrent()
        assertTrue(model.state.value.error!!.contains("memory"))
        model.handleIntent(ScreenColorIntent.Cancel)
        assertEquals(ScreenColorState(), model.state.value)
    }
}
