package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import com.donnclab.kinetiframe.audio.AudioPlaybackManager
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.domain.AnimationEngine
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager for playback and frame navigation.
 */
class PlaybackManager(
    private val scope: CoroutineScope,
    private val engineDispatcher: CoroutineDispatcher,
    private val engine: AnimationEngine,
    private val cacheManager: FrameCacheManager,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val loadFrame: (Int) -> Unit,
    private val composeFrame: suspend (Int, Bitmap?) -> Bitmap?,
    private val activeLayerId: () -> String,
    /** Lets audio playback (see [AudioPlaybackManager]) stay in lockstep with frame playback
     *  without this class knowing MediaPlayer exists -- defaulted to a no-op so every existing
     *  test call site that constructs a PlaybackManager positionally keeps compiling unchanged. */
    private val onFrameChanged: (Int) -> Unit = {},
    private val onPlaybackStateChanged: (Boolean) -> Unit = {},
    private val onPlaybackError: (Exception) -> Unit = {},
    private val clock: PlaybackClock = PlaybackClock(),
) {
    private var renderJob: Job? = null
    private var playbackGeneration = 0L

    fun play(state: StudioState) {
        if (state.animation.isPlaying || state.animation.totalFrames < 1) return
        playbackGeneration++
        clock.start(state.animation.currentFrame, state.project.fps)

        updateState {
            it.copy(animation = it.animation.copy(isPlaying = true, playbackFrame = it.animation.currentFrame))
        }
        engine.start(state.project.fps)
        onPlaybackStateChanged(true)
        onFrameChanged(state.animation.currentFrame)
    }

    fun pause(state: StudioState) {
        if (!state.animation.isPlaying) return
        playbackGeneration++
        renderJob?.cancel()
        renderJob = null

        updateState { it.copy(animation = it.animation.copy(isPlaying = false)) }
        loadFrame(state.animation.currentFrame)
        engine.stop()
        onPlaybackStateChanged(false)
    }

    fun setFps(fps: Int) {
        val validatedFps = fps.coerceIn(6, 30)
        updateState {
            if (it.animation.isPlaying) clock.changeFps(validatedFps, it.animation.totalFrames)
            it.copy(
                project = it.project.copy(fps = validatedFps),
                animation = it.animation.copy(fps = validatedFps),
            )
        }
        engine.updateFps(validatedFps)
    }

    fun incrementFrame(state: StudioState) {
        if (state.animation.isPlaying) {
            // Backpressure: never queue full-size composites faster than disk decoding/rendering.
            // The owning scope and tick collector run on Main; only pixel work leaves that thread.
            if (state.animation.totalFrames < 1) return
            val nextPlaybackFrame = clock.frame(state.animation.totalFrames)
            if (nextPlaybackFrame != state.animation.playbackFrame) {
                updateState { it.copy(animation = it.animation.copy(playbackFrame = nextPlaybackFrame)) }
                onFrameChanged(nextPlaybackFrame)
            }
            if (renderJob?.isActive == true) return
            val generation = playbackGeneration
            val layerId = activeLayerId()
            renderJob = scope.launch {
                try {
                    // Empty active layers must not suppress other layers or actor-only frames.
                    val composite = withContext(engineDispatcher) {
                        composeFrame(nextPlaybackFrame, cacheManager.getFrame(layerId, nextPlaybackFrame))
                    }
                    if (generation == playbackGeneration) {
                        updateState { it.copy(currentBitmap = composite) }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (generation == playbackGeneration) {
                        engine.stop()
                        updateState { it.copy(animation = it.animation.copy(isPlaying = false)) }
                        onPlaybackStateChanged(false)
                        onPlaybackError(error)
                    }
                }
            }
            return
        }
        val nextFrame = (state.animation.currentFrame + 1) % state.animation.totalFrames
        updateState { it.copy(animation = it.animation.copy(currentFrame = nextFrame)) }
        loadFrame(nextFrame)
        onFrameChanged(nextFrame)
    }

    fun decrementFrame(state: StudioState) {
        val prevFrame = if (state.animation.currentFrame > 0) {
            state.animation.currentFrame - 1
        } else {
            state.animation.totalFrames - 1
        }
        updateState { it.copy(animation = it.animation.copy(currentFrame = prevFrame)) }
        loadFrame(prevFrame)
        onFrameChanged(prevFrame)
    }

    fun setCurrentFrame(frame: Int, totalFrames: Int) {
        val targetFrame = frame.coerceIn(0, maxOf(0, totalFrames - 1))
        updateState { state ->
            state.copy(animation = state.animation.copy(currentFrame = targetFrame))
        }
        loadFrame(targetFrame)
        onFrameChanged(targetFrame)
    }
}
