package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import com.donnclab.kinetiframe.data.video.VideoFrameDecoder
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.ui.StudioState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Decode off the UI thread, serialized by the decoder. */
class VideoFrameProvider(private val getState: () -> StudioState) {
    data class DecodedFrame(
        val projectId: String,
        val clipId: String?,
        val sourcePath: String?,
        val timelineStartFrame: Int?,
        val sourceInSeconds: Double?,
        val frame: Int,
        val bitmap: Bitmap?,
    )
    private data class Request(val frame: Int, val project: ProjectDocument)
    private val decoder = VideoFrameDecoder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Channel<Request>(Channel.CONFLATED)
    private val mutableFrames = MutableStateFlow<DecodedFrame?>(null)
    val frames = mutableFrames.asStateFlow()

    init {
        scope.launch {
            try {
                for (request in requests) {
                    val active = VideoTrackSync.activeClip(
                        request.project.videoClips.values,
                        request.frame,
                        request.project.fps,
                    )
                    val clip = active?.let { request.project.videoClips[it.clipId] }
                    val decoded = decoder.decode(request.project, request.frame)
                    ensureActive()
                    mutableFrames.value = DecodedFrame(
                        request.project.id,
                        clip?.clipId,
                        clip?.let { request.project.assets[it.assetId]?.sourcePath },
                        clip?.timelineStartFrame,
                        clip?.videoSourceOffsetSeconds,
                        request.frame,
                        decoded,
                    )
                }
            } finally {
                decoder.close()
            }
        }
    }

    /** Conflate pointer/playback requests, not decoding itself: a slow decoder still produces
     * frames instead of cancelling every request before it can ever display one. */
    fun request(frame: Int, project: ProjectDocument = getState().project) {
        requests.trySend(Request(frame, project))
    }
    fun release() {
        requests.close()
        scope.cancel()
        mutableFrames.value = null
    }
}
