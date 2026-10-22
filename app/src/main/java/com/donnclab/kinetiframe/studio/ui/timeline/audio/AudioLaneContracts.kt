package com.donnclab.kinetiframe.studio.ui.timeline.audio

import androidx.compose.foundation.lazy.LazyListState
import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import com.donnclab.kinetiframe.project.model.AudioTrackSettings

/**
 * Everything the audio lanes need to read, bundled so the lane group stays inside the
 * 4-parameter budget instead of trailing one positional argument per fact.
 *
 * [filmstripScrollState] is shared rather than owned: every lane scrolls in lockstep with the
 * frame filmstrip above it, so a clip always sits under the frame it actually plays on.
 */
data class AudioLaneContext(
    val totalFrames: Int,
    val currentFrame: Int,
    val filmstripScrollState: LazyListState,
    val selectedClipId: String?,
    val tracks: Map<Int, AudioTrackSettings>,
)

/**
 * Everything the audio lanes can ask for. Grouped for the same reason as [AudioLaneContext] --
 * ten separate lambda parameters is exactly the signature sprawl the project's rules forbid, and
 * a named bundle also means adding a lane action later does not re-thread every call site.
 *
 * Lane-scoped actions take a track index; clip-scoped ones take a clip id, so it is impossible to
 * pass one where the other is meant.
 */
data class AudioLaneCallbacks(
    val onSelect: (clipId: String) -> Unit,
    val onTrim: (clipId: String, startFrame: Int, durationFrames: Int) -> Unit,
    val onMoveToPlayhead: (clipId: String, trackIndex: Int, startFrame: Int) -> Unit,
    val onRemove: (clipId: String) -> Unit,
    val onSetVolume: (clipId: String, volume: Float) -> Unit,
    val onAddToLibrary: (AudioClipPlacement) -> Unit,
    val onToggleMute: (trackIndex: Int) -> Unit,
    val onToggleLock: (trackIndex: Int) -> Unit,
    val onDuplicate: (clipId: String) -> Unit,
    val onSplit: (clipId: String, atFrame: Int) -> Unit,
)
