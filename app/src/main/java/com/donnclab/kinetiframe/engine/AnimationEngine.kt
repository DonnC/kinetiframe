package com.donnclab.kinetiframe.engine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-frequency rendering engine for animation playback.
 */
class AnimationEngine(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private var job: Job? = null
    private val _frameTicks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val frameTicks: SharedFlow<Unit> = _frameTicks

    @Volatile
    private var targetFps: Int = 60

    /**
     * Starts the rendering loop at the specified FPS.
     */
    fun start(fps: Int) {
        targetFps = fps.coerceIn(6, 30)
        if (job?.isActive == true) return

        job = scope.launch(dispatcher) {
            var nextFrameTimeNanos = System.nanoTime()

            while (isActive) {
                // Emit tick to subscribers
                _frameTicks.emit(Unit)

                val currentFps = targetFps
                val frameDurationNanos = 1_000_000_000L / currentFps
                nextFrameTimeNanos += frameDurationNanos

                val now = System.nanoTime()
                val sleepTimeNanos = nextFrameTimeNanos - now

                if (sleepTimeNanos > 0) {
                    // Precision delay: convert nanos to millis for Coroutines delay
                    delay(sleepTimeNanos / 1_000_000L)
                } else if (now > nextFrameTimeNanos + frameDurationNanos) {
                    // If we're lagging by more than one frame, reset the baseline
                    // to prevent "catch-up" bursts that exceed the target FPS.
                    nextFrameTimeNanos = now
                }
            }
        }
    }

    /**
     * Updates the target FPS while the engine is running.
     */
    fun updateFps(fps: Int) {
        targetFps = fps.coerceIn(6, 30)
    }

    /**
     * Stops the rendering loop.
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}
