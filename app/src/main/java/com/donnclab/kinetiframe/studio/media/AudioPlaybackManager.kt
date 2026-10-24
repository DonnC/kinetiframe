package com.donnclab.kinetiframe.studio.media

import android.media.MediaPlayer
import android.util.Log
import com.donnclab.kinetiframe.media.audio.AudioTrackSync
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.domain.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/**
 * Drives real audio playback from frame position alone -- a small pool of [MediaPlayer] instances
 * keyed by clip ID, kept in lockstep with [AudioTrackSync]'s pure membership/offset math. This
 * class owns the only Android-runtime-dependent (MediaPlayer) audio code; everything upstream of
 * it ([AudioTrackSync], [AudioManager]) stays plain and unit-testable.
 *
 * Called from two places, both wired in [com.donnclab.kinetiframe.studio.StudioViewModel]:
 *  - [PlaybackManager] on every real Play frame tick and on Play/Pause, via the `onFrameChanged`/
 *    `onPlaybackStateChanged` callbacks threaded into its constructor.
 *  - Directly on every frame change from scrubbing (`Playback.SetCurrentFrame`/`NextFrame`/
 *    `PreviousFrame` while not playing), so dragging the frame scrubber previews audio too, not
 *    just real playback -- the same [onFrameChanged] entry point serves both, since from this
 *    class's point of view "the current frame changed" is the only fact that matters.
 *
 * Scrubbing previews a short slice, renewed while the playhead moves. All calls must be made
 * on the owning Main scope: MediaPlayer is not thread-safe. Preparation is asynchronous and
 * cancellation releases the native player even when its source has not finished loading.
 */
class AudioPlaybackManager(
    private val getState: () -> StudioState,
    private val scope: CoroutineScope,
    private val createPlayer: () -> MediaPlayer = { MediaPlayer() },
    private val onAudioError: (Throwable) -> Unit = {},
) {
    private class Session(val player: MediaPlayer) {
        var prepared = false
        var preparation: Job? = null
        var seekPending = false
        var requestedMillis = 0L
        var seekingMillis = 0L
        var grain: Job? = null
        var envelope: Job? = null
    }

    // All entry points are owned by the ViewModel's Main scope, including scrub-stop jobs.
    private val players = mutableMapOf<String, Session>()
    private var latestFrame = 0
    private val failedClips = mutableSetOf<String>()
    private val sources = mutableMapOf<String, String>()

    /** Stops the audio a scrub started. Restarted on every scrubbed frame, so continuous dragging
     *  keeps renewing it and only the final position is left to play out. */
    private var scrubStopJob: Job? = null
    private var idleReleaseJob: Job? = null
    private var auditionActive = false

    /** Play stopping (or the timeline exiting raster-drawing mode) always stops every clip
     *  outright -- there is no "paused mid-clip, resume exactly here" state to preserve, since
     *  the next Play or scrub will resync from whatever frame is current anyway. */
    @androidx.annotation.MainThread
    fun onPlaybackStateChanged(playing: Boolean) {
        scrubStopJob?.cancel()
        scrubStopJob = null
        failedClips.clear()
        if (!playing) stopAll()
    }

    @androidx.annotation.MainThread
    fun onFrameChanged(frame: Int) {
        idleReleaseJob?.cancel()
        idleReleaseJob = null
        auditionActive = true
        scrubStopJob?.cancel()
        scrubStopJob = if (getState().animation.isPlaying) {
            null
        } else {
            scope.launch {
                delay(SCRUB_SLICE_MS)
                auditionActive = false
                // Keep prepared decoders warm briefly: repeated lip-sync taps must not re-prepare.
                players.values.filter { it.prepared }.forEach { runCatching { it.player.pause() } }
                idleReleaseJob = scope.launch {
                    delay(WARM_IDLE_MS)
                    stopAll()
                }
            }
        }
        syncFrame(frame)
    }

    private fun syncFrame(frame: Int) {
        latestFrame = frame
        val project = getState().project
        // Filtered before the sync maths rather than after: a muted lane's players must be torn
        // down like any other inactive clip, so muting mid-playback stops the sound immediately
        // instead of leaving a prepared player running until the playhead happens to leave it.
        val audible = project.audioClips.values.filterNot { clip ->
            project.audioTracks[clip.trackIndex]?.isMuted == true
        }
        val active = AudioTrackSync.activeClips(audible, frame, project.fps).associateBy { it.clipId }
        failedClips.retainAll(active.keys)
        sources.keys.retainAll(active.keys)

        val noLongerActive = players.keys - active.keys
        noLongerActive.forEach { clipId -> releasePlayer(clipId) }

        active.values.forEach { activeClip ->
            val clip = project.audioClips[activeClip.clipId] ?: return@forEach
            val sourcePath = project.assets[clip.assetId]?.sourcePath ?: return@forEach
            if (sources.put(activeClip.clipId, sourcePath) != sourcePath) {
                releasePlayer(activeClip.clipId)
                failedClips.remove(activeClip.clipId)
            }
            if (activeClip.clipId in failedClips) return@forEach
            var session = players[activeClip.clipId]
            if (session == null) {
                val newSession = runCatching { Session(createPlayer()) }.getOrElse {
                    Log.w("AudioPlaybackManager", "Could not create audio player", it)
                    failedClips.add(activeClip.clipId)
                    onAudioError(it)
                    return@forEach
                }
                players[activeClip.clipId] = newSession
                newSession.preparation = scope.launch {
                    try {
                        newSession.player.setDataSource(sourcePath)
                        awaitPrepared(newSession.player)
                        newSession.prepared = true
                        newSession.envelope = scope.launch {
                            // Media clock, not animation ticks: even a 6fps project gets smooth
                            // volume ramps. One cancellable Main job per prepared audible clip.
                            while (players[activeClip.clipId] === newSession) {
                                delay(16)
                                val current = getState().project
                                val placed = current.audioClips[activeClip.clipId] ?: break
                                runCatching {
                                    if (newSession.player.isPlaying && !newSession.seekPending) {
                                        val relative =
                                            newSession.player.currentPosition * 1000L - placed.trimStartMicros
                                        val gain = com.donnclab.kinetiframe.media.audio.AudioEnvelope.gain(
                                            placed,
                                            relative,
                                            current.fps,
                                        )
                                        newSession.player.setVolume(gain, gain)
                                        if (relative >=
                                            placed.durationFrames * 1_000_000L / current.fps
                                        ) {
                                            newSession.player.pause()
                                        }
                                    }
                                }.onFailure {
                                    failedClips.add(activeClip.clipId)
                                    onAudioError(it)
                                    releasePlayer(activeClip.clipId)
                                }
                            }
                        }
                        // Preparation may take several ticks. Resync to the latest position,
                        // not the position when the file started loading.
                        if (players[activeClip.clipId] === newSession &&
                            (auditionActive || getState().animation.isPlaying)
                        ) {
                            syncFrame(latestFrame)
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("AudioPlaybackManager", "Could not prepare clip ${clip.clipId}", error)
                        failedClips.add(activeClip.clipId)
                        onAudioError(error)
                        if (players[activeClip.clipId] === newSession) releasePlayer(activeClip.clipId)
                    }
                }
                session = newSession
            }
            if (!session.prepared) return@forEach
            val player = session.player
            runCatching {
                val gain = com.donnclab.kinetiframe.media.audio.AudioEnvelope.gain(
                    clip,
                    (frame - clip.startFrame) * 1_000_000L / project.fps,
                    project.fps,
                )
                player.setVolume(gain, gain)
                val targetMs = (activeClip.offsetMicros / 1000L).toInt()
                // Only reseek when drifted more than a small threshold: once a clip is already
                // playing during real Play, its own clock already advances in step with the
                // engine's wall-clock frame loop, so reseeking on every single tick would fight
                // the player's own playback instead of merely correcting drift.
                session.requestedMillis = targetMs.toLong()
                if (session.seekPending || session.grain?.isActive == true) return@runCatching
                if (!getState().animation.isPlaying || !player.isPlaying ||
                    abs(player.currentPosition - targetMs) > 80
                ) {
                    // SEEK_CLOSEST, not the int overload: the default seek lands on the nearest
                    // sync sample, which for AAC can be tens of milliseconds away -- audible as
                    // the audio failing to line up with the frame you scrubbed to. minSdk is 28,
                    // so the precise overload is always available here.
                    if (player.isPlaying) player.pause()
                    seekLatest(activeClip.clipId, session)
                } else if (!player.isPlaying) {
                    player.start()
                }
            }.onFailure { error ->
                Log.w("AudioPlaybackManager", "Could not play audio clip ${clip.clipId}", error)
                failedClips.add(activeClip.clipId)
                onAudioError(error)
                releasePlayer(activeClip.clipId)
            }
        }
    }

    /** One native seek in flight per clip; newer frame requests replace its pending destination. */
    private fun seekLatest(clipId: String, session: Session) {
        session.seekPending = true
        session.seekingMillis = session.requestedMillis
        session.player.setOnSeekCompleteListener {
            if (players[clipId] !== session) return@setOnSeekCompleteListener
            session.seekPending = false
            if (!auditionActive && !getState().animation.isPlaying) return@setOnSeekCompleteListener
            runCatching {
                if (!session.player.isPlaying) session.player.start()
                if (!getState().animation.isPlaying) {
                    // Give each completed seek a small audible grain. Immediately chasing every
                    // newer frame can starve audio entirely during a fast continuous drag.
                    session.grain = scope.launch {
                        delay(MIN_GRAIN_MS)
                        if (players[clipId] === session && auditionActive &&
                            session.requestedMillis != session.seekingMillis
                        ) {
                            runCatching {
                                session.player.pause()
                                seekLatest(clipId, session)
                            }.onFailure { error ->
                                failedClips.add(clipId)
                                onAudioError(error)
                                releasePlayer(clipId)
                            }
                        }
                    }
                }
            }.onFailure { error ->
                failedClips.add(clipId)
                onAudioError(error)
                releasePlayer(clipId)
            }
        }
        session.player.seekTo(session.seekingMillis, MediaPlayer.SEEK_CLOSEST)
    }

    private fun releasePlayer(clipId: String) {
        players.remove(clipId)?.let { session ->
            session.preparation?.cancel()
            session.grain?.cancel()
            session.envelope?.cancel()
            // release is legal even while preparing; isPlaying/stop are not.
            runCatching { session.player.release() }
        }
    }

    /** Native callbacks are bridged once to a cancellable coroutine; no blocking prepare on UI. */
    private suspend fun awaitPrepared(player: MediaPlayer) = suspendCancellableCoroutine<Unit> { continuation ->
        player.setOnPreparedListener {
            if (continuation.isActive) continuation.resume(Unit)
        }
        player.setOnErrorListener { _, what, extra ->
            if (continuation.isActive) {
                continuation.resumeWithException(java.io.IOException("Audio decoder error $what/$extra"))
            } else {
                players.entries.firstOrNull { it.value.player === player }?.key?.let { clipId ->
                    failedClips.add(clipId)
                    onAudioError(java.io.IOException("Audio decoder error $what/$extra"))
                    releasePlayer(clipId)
                }
            }
            true
        }
        player.prepareAsync()
    }

    @androidx.annotation.MainThread
    fun stopAll() {
        auditionActive = false
        scrubStopJob?.cancel()
        scrubStopJob = null
        idleReleaseJob?.cancel()
        idleReleaseJob = null
        players.keys.toList().forEach { releasePlayer(it) }
    }

    private companion object {
        /** Long enough to identify a sound, short enough that dragging across a clip reads as
         *  scrubbing rather than as stuttering playback. */
        const val SCRUB_SLICE_MS = 140L
        const val WARM_IDLE_MS = 2_000L
        const val MIN_GRAIN_MS = 35L
    }

    /** Must be called from the owning ViewModel's onCleared -- MediaPlayer instances are native
     *  resources and are not garbage-collected away on their own. */
    @androidx.annotation.MainThread
    fun release() = stopAll()
}
