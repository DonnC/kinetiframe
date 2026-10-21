package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.Text
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import com.donnclab.kinetiframe.engine.model.AudioTrackSettings
import com.donnclab.kinetiframe.engine.model.MAX_AUDIO_TRACKS
import com.donnclab.kinetiframe.engine.model.VideoClipPlacement
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.components.timeline.audio.AudioLaneHeader

/** A bounded track workspace. One horizontal viewport owns every asset lane. */
@Composable
fun TimelineTracksPanel(
    modifier: Modifier = Modifier,
    state: TimelineState,
    onIntent: (TimelineIntent) -> Unit,
    performanceRepository: PerformanceRepository,
    onEditPlacement: (ActorPlacementReference) -> Unit,
    onAddToLibrary: (ActorPlacementReference) -> Unit,
    onAddAudioToLibrary: (AudioClipPlacement) -> Unit = {},
    onCreatePerformance: ((String, String) -> Unit)? = null,
    filmstripScrollState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    workspaceMode: Boolean = false,
) {
    val actors =
        remember(state.project.actorPlacements) { state.project.actorPlacements.values.sortedBy { it.startFrame } }
    val audio = remember(state.project.audioClips) { state.project.audioClips.values.sortedBy { it.startFrame } }
    val videos =
        remember(state.project.videoClips) { state.project.videoClips.values.sortedBy { it.timelineStartFrame } }
    val camera = state.project.cameraShots
    if (actors.isEmpty() && audio.isEmpty() && videos.isEmpty() && camera.isEmpty()) {
        if (workspaceMode) Text("No clips yet", Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val laneHeight = if (workspaceMode) {
        (LocalConfiguration.current.screenHeightDp.dp - 120.dp).coerceAtLeast(48.dp)
    } else {
        (LocalConfiguration.current.screenHeightDp.dp * .24f).coerceIn(48.dp, 168.dp)
    }
    var dropLane by remember { mutableStateOf<Int?>(null) }
    // A typed key prevents an actor and an audio clip from exposing competing controls.
    var selected by remember { mutableStateOf<String?>(null) }
    var actionsOpen by remember { mutableStateOf(false) }
    var moveOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val actor = actors.firstOrNull { "actor:${it.placementId}" == selected }
    val sound = audio.firstOrNull { "audio:${it.clipId}" == selected }
    val video = videos.firstOrNull { "video:${it.clipId}" == selected }
    val label = actor?.label ?: sound?.label ?: video?.label
    val selectedStart = actor?.startFrame ?: sound?.startFrame ?: video?.timelineStartFrame ?: 0
    LaunchedEffect(label) {
        if (label == null) {
            selected = null
            actionsOpen = false
            moveOpen = false
        }
    }
    fun select(key: String) {
        selected = key
        actionsOpen = true
        notice = null
    }
    fun placeVideo(clip: VideoClipPlacement, start: Int, end: Int, sourceOffset: Double) {
        val linkedAudio = clip.linkedAudioClipId?.let { state.project.audioClips[it] }
        if (linkedAudio != null && state.project.audioTracks[linkedAudio.trackIndex]?.isLocked == true) {
            notice = "Unlock the linked sound lane to change video timing"
        } else if (videos.any {
                it.clipId != clip.clipId && start < it.timelineEndFrame && it.timelineStartFrame < end
            }
        ) {
            notice = "Video clips cannot overlap"
        } else {
            onIntent(TimelineIntent.Video.Trim(clip.clipId, start, end, sourceOffset))
        }
    }
    fun move(start: Int, lane: Int = sound?.trackIndex ?: 0) {
        when {
            actor != null -> onIntent(TimelineIntent.ActorPlacement.Move(actor.placementId, start))

            sound != null -> {
                if (state.project.videoClips.values.any { it.linkedAudioClipId == sound.clipId }) {
                    notice =
                        "Detach sound from video before moving it separately"
                } else if (state.project.audioTracks[lane]?.isLocked == true) {
                    notice = "Track A${lane + 1} is locked"
                } else {
                    onIntent(TimelineIntent.Audio.Move(sound.clipId, lane, start))
                }
            }

            video != null -> {
                val end = start + video.timelineEndFrame - video.timelineStartFrame
                placeVideo(video, start, end, video.videoSourceOffsetSeconds)
            }
        }
    }
    val clipMenu: @Composable () -> Unit = {
        TrackClipContextMenu(
            actionsOpen, { actionsOpen = false }, state, actor, sound, video,
            onIntent, { moveOpen = true }, performanceRepository, onEditPlacement,
            onAddToLibrary, onAddAudioToLibrary, onCreatePerformance,
        )
    }
    // Show contiguous audio lanes, including one empty drop target. Never renumber sparse tracks.
    val audioLaneCount = if (audio.isEmpty()) {
        0
    } else {
        (
            (
                audio.maxOf {
                    it.trackIndex
                } + 2
                ).coerceAtMost(MAX_AUDIO_TRACKS)
            )
    }
    val frameCount = maxOf(
        state.animation.totalFrames,
        actors.maxOfOrNull { it.startFrame + it.durationFrames * it.repeatCount } ?: 0,
        audio.maxOfOrNull { it.startFrame + it.durationFrames } ?: 0,
        videos.maxOfOrNull { it.timelineEndFrame } ?: 0,
        camera.maxOfOrNull { it.endFrame } ?: 0,
    ).coerceAtLeast(1)
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Column {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!workspaceMode) {
                    IconButton(onClick = {
                        expanded = !expanded
                    }) {
                        Icon(
                            if (expanded) Lucide.ChevronDown else Lucide.ChevronRight,
                            "Expand or collapse asset tracks",
                        )
                    }
                }
                Text(
                    notice ?: label ?: "Tracks · ${actors.size + audio.size + videos.size} clips",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                if (label != null) {
                    IconButton(onClick = { actionsOpen = true }) { Icon(Lucide.Ellipsis, "Clip actions") }
                    IconButton(onClick = {
                        selected = null
                        actionsOpen = false
                    }) { Icon(Lucide.X, "Deselect clip") }
                }
            }
            if (expanded || workspaceMode) {
                Row(Modifier.heightIn(max = laneHeight).verticalScroll(rememberScrollState())) {
                    Column(Modifier.width(TRACK_GUTTER)) {
                        Text(
                            "Frames",
                            Modifier.height(24.dp).padding(start = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (camera.isNotEmpty()) TrackLabel("Camera")
                        actors.forEachIndexed { i, _ -> TrackLabel("Actor ${i + 1}") }
                        repeat(audioLaneCount) { lane ->
                            val settings = state.project.audioTracks[lane] ?: AudioTrackSettings()
                            Row(Modifier.height(TRACK_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "A${lane + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(24.dp),
                                )
                                AudioLaneHeader(
                                    settings.isMuted,
                                    settings.isLocked,
                                    { onIntent(TimelineIntent.Audio.SetTrackMuted(lane, !settings.isMuted)) },
                                    { onIntent(TimelineIntent.Audio.SetTrackLocked(lane, !settings.isLocked)) },
                                )
                            }
                        }
                        if (videos.isNotEmpty()) TrackLabel("Video")
                    }
                    TimelineTrackViewport(
                        frameCount,
                        filmstripScrollState,
                        Modifier.weight(1f),
                        currentFrame = state.animation.currentFrame,
                        onScrubFrame = {
                            onIntent(TimelineIntent.Playback.SetCurrentFrame(it))
                        },
                    ) { offsetFrames, visibleFrames ->
                        Column(Modifier.fillMaxWidth()) {
                            TimelineFrameRuler(frameCount, offsetFrames, visibleFrames) {
                                onIntent(TimelineIntent.Playback.SetCurrentFrame(it))
                            }
                            if (camera.isNotEmpty()) {
                                com.donnclab.kinetiframe.ui.components.camera.CameraTimelineLane(
                                    camera,
                                    state.animation.totalFrames,
                                    offsetFrames,
                                    visibleFrames,
                                    onIntent,
                                )
                            }
                            actors.forEach { placement ->
                                Box(Modifier.fillMaxWidth().height(TRACK_HEIGHT)) {
                                    TimelineAssetClip(
                                        placement.placementId, placement.label, placement.startFrame,
                                        placement.durationFrames * placement.repeatCount, actor == placement,
                                        onSelect = { select("actor:${placement.placementId}") },
                                        onMove = { start, _ ->
                                            onIntent(TimelineIntent.ActorPlacement.Move(placement.placementId, start))
                                        },
                                        onTrim = { start, duration ->
                                            onIntent(
                                                TimelineIntent.ActorPlacement.Trim(
                                                    placement.placementId,
                                                    start,
                                                    duration,
                                                ),
                                            )
                                        },
                                        minStart = (placement.startFrame - placement.sourceOffsetFrames).coerceAtLeast(
                                            0,
                                        ),
                                        maxEnd =
                                        placement.startFrame - placement.sourceOffsetFrames +
                                            (
                                                placement.sourceSpanFrames.takeIf { it > 0 }
                                                    ?: (
                                                        placement.durationFrames * placement.repeatCount +
                                                            placement.sourceOffsetFrames
                                                        )
                                                ),
                                        offsetFrames = offsetFrames,
                                        visibleFrames = visibleFrames,
                                        contextMenu = clipMenu,
                                    )
                                }
                            }
                            repeat(audioLaneCount) { lane ->
                                Box(
                                    Modifier.fillMaxWidth().height(TRACK_HEIGHT).background(
                                        if (dropLane ==
                                            lane
                                        ) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = .18f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = if (lane %
                                                    2 ==
                                                    0
                                                ) {
                                                    .025f
                                                } else {
                                                    .05f
                                                },
                                            )
                                        },
                                    ),
                                ) {
                                    audio.filter { it.trackIndex == lane }.forEach { clip ->
                                        key(clip.clipId) {
                                            TimelineAssetClip(
                                                clip.clipId, clip.label, clip.startFrame, clip.durationFrames,
                                                sound == clip,
                                                locked = state.project.audioTracks[lane]?.isLocked == true,
                                                peaks = clip.trimSource?.peaks ?: clip.waveformPeaks,
                                                muted =
                                                state.project.audioTracks[lane]?.isMuted == true,
                                                track = lane, trackCount = audioLaneCount,
                                                onSelect = { select("audio:${clip.clipId}") },
                                                onMove = { start, target ->
                                                    if (state.project.audioTracks[target]?.isLocked ==
                                                        true
                                                    ) {
                                                        notice = "Track A${target + 1} is locked"
                                                    } else {
                                                        onIntent(
                                                            TimelineIntent.Audio.Move(clip.clipId, target, start),
                                                        )
                                                    }
                                                },
                                                onTrim = { start, duration ->
                                                    onIntent(TimelineIntent.Audio.Trim(clip.clipId, start, duration))
                                                },
                                                minStart = (clip.startFrame - clip.trimOffsetFrames).coerceAtLeast(0),
                                                maxEnd =
                                                clip.startFrame - clip.trimOffsetFrames +
                                                    (clip.trimSource?.durationFrames ?: clip.durationFrames),
                                                waveformStart = clip.startFrame - clip.trimOffsetFrames,
                                                waveformDuration =
                                                clip.trimSource?.durationFrames ?: clip.durationFrames,
                                                offsetFrames = offsetFrames, visibleFrames = visibleFrames,
                                                onDragTarget = { dropLane = it }, contextMenu = clipMenu,
                                            )
                                        }
                                    }
                                }
                            }
                            if (videos.isNotEmpty()) {
                                Box(Modifier.fillMaxWidth().height(TRACK_HEIGHT)) {
                                    videos.forEach { clip ->
                                        key(clip.clipId) {
                                            TimelineAssetClip(
                                                clip.clipId, clip.label, clip.timelineStartFrame,
                                                clip.timelineEndFrame - clip.timelineStartFrame, video == clip,
                                                onSelect = { select("video:${clip.clipId}") },
                                                onMove = { start, _ ->
                                                    val end = start + clip.timelineEndFrame - clip.timelineStartFrame
                                                    placeVideo(clip, start, end, clip.videoSourceOffsetSeconds)
                                                },
                                                onTrim = { start, duration ->
                                                    placeVideo(
                                                        clip,
                                                        start,
                                                        start + duration,
                                                        clip.videoSourceOffsetSeconds +
                                                            (start - clip.timelineStartFrame).toDouble() /
                                                            state.project.fps.coerceAtLeast(1),
                                                    )
                                                },
                                                minStart =
                                                (clip.timelineStartFrame - clip.sourceOffsetFrames).coerceAtLeast(
                                                    0,
                                                ),
                                                maxEnd =
                                                clip.timelineStartFrame - clip.sourceOffsetFrames +
                                                    (
                                                        clip.sourceDurationFrames.takeIf { it > 0 }
                                                            ?: (
                                                                clip.timelineEndFrame - clip.timelineStartFrame +
                                                                    clip.sourceOffsetFrames
                                                                )
                                                        ),
                                                offsetFrames = offsetFrames,
                                                visibleFrames = visibleFrames,
                                                contextMenu = clipMenu,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Box(
                            Modifier.offset(x = FRAME_CELL_WIDTH * (state.animation.currentFrame - offsetFrames))
                                .width(2.dp).height(
                                    24.dp +
                                        TRACK_HEIGHT *
                                        (
                                            actors.size + audioLaneCount + (if (videos.isEmpty()) 0 else 1) +
                                                (if (camera.isEmpty()) 0 else 1)
                                            ),
                                )
                                .background(MaterialTheme.colorScheme.error),
                        )
                    }
                }
            }
        }
    }
    if (moveOpen && label != null) {
        ClipPositionDialog(
            label,
            selectedStart,
            sound?.trackIndex,
            onDismiss = { moveOpen = false },
            onConfirm = { start, lane ->
                move(start, lane)
                moveOpen = false
            },
        )
    }
}

@Composable
private fun TrackLabel(label: String) {
    Box(Modifier.width(TRACK_GUTTER).height(TRACK_HEIGHT), contentAlignment = Alignment.CenterStart) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Only user drags propagate back. Programmatic sync must never become another scroll source. */
@Composable
internal fun rememberLaneScrollState(filmstripScrollState: LazyListState, cellWidthPx: Float): ScrollState {
    val lane = rememberScrollState()
    val dragging by lane.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(filmstripScrollState, cellWidthPx) {
        snapshotFlow { filmstripScrollState.firstVisibleItemIndex to filmstripScrollState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (!dragging) lane.scrollTo(TimelineScrollSync.itemToPixels(index, offset, cellWidthPx))
            }
    }
    LaunchedEffect(lane, filmstripScrollState, cellWidthPx) {
        snapshotFlow { lane.value to dragging }.collect { (px, userDragging) ->
            if (userDragging) {
                val (index, offset) = TimelineScrollSync.pixelsToItem(px, cellWidthPx)
                filmstripScrollState.scrollToItem(
                    index.coerceAtMost(
                        (
                            filmstripScrollState.layoutInfo.totalItemsCount -
                                1
                            ).coerceAtLeast(0),
                    ),
                    offset,
                )
            }
        }
    }
    return lane
}
