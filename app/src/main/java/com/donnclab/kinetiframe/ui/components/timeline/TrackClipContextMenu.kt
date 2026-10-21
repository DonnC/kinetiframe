package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.composables.icons.lucide.ArrowDownToLine
import com.composables.icons.lucide.ArrowUpToLine
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Blend
import com.composables.icons.lucide.BookmarkPlus
import com.composables.icons.lucide.BringToFront
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MoveHorizontal
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Redo
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Scan
import com.composables.icons.lucide.Scissors
import com.composables.icons.lucide.SendToBack
import com.composables.icons.lucide.Split
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Undo
import com.composables.icons.lucide.Undo2
import com.composables.icons.lucide.Unlink
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.Volume2
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import com.donnclab.kinetiframe.engine.model.VideoClipPlacement
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.components.common.ContextIconAction
import com.donnclab.kinetiframe.ui.components.common.ContextSliderButton
import com.donnclab.kinetiframe.ui.components.common.IconContextMenu

/** The clip owns its anchor; the project remains the sole source of committed editing state. */
@Composable
internal fun TrackClipContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: TimelineState,
    actor: ActorPlacementReference?,
    sound: AudioClipPlacement?,
    video: VideoClipPlacement?,
    onIntent: (TimelineIntent) -> Unit,
    onPosition: () -> Unit,
    performances: PerformanceRepository,
    onEdit: (ActorPlacementReference) -> Unit,
    onSaveActor: (ActorPlacementReference) -> Unit,
    onSaveAudio: (AudioClipPlacement) -> Unit,
    onCreatePerformance: ((String, String) -> Unit)?,
) {
    var showFades by remember(sound?.clipId) { mutableStateOf(false) }
    var showCrossfade by remember(sound?.clipId) { mutableStateOf(false) }
    var showFraming by remember(video?.clipId) { mutableStateOf(false) }
    var showVoice by remember(sound?.clipId) { mutableStateOf(false) }
    val nextAudio = sound?.let { first ->
        state.project.audioClips.values.filter {
            it.clipId != first.clipId && it.trackIndex == first.trackIndex &&
                it.startFrame >= first.startFrame + first.durationFrames
        }.minByOrNull { it.startFrame }
    }
    val lockedSound = sound ?: video?.linkedAudioClipId?.let { state.project.audioClips[it] }
    val locked = lockedSound?.let { state.project.audioTracks[it.trackIndex]?.isLocked == true } ?: false
    val linkedVideo = sound?.let { audio ->
        state.project.videoClips.values.firstOrNull {
            it.linkedAudioClipId ==
                audio.clipId
        }
    }
    fun dispatch(intent: TimelineIntent) {
        onDismiss()
        onIntent(intent)
    }
    val actions = buildList {
        add(
            ContextIconAction(
                Lucide.MoveHorizontal,
                "Position clip precisely",
                {
                    onDismiss()
                    onPosition()
                },
                !locked && linkedVideo == null,
            ),
        )
        actor?.let { clip ->
            add(
                ContextIconAction(Lucide.Pen, "Edit performance", {
                    onDismiss()
                    onEdit(clip)
                }),
            )
            add(
                ContextIconAction(
                    Lucide.Scissors,
                    "Split at playhead",
                    {
                        dispatch(TimelineIntent.ActorPlacement.Split(clip.placementId, state.animation.currentFrame))
                    },
                    state.animation.currentFrame > clip.startFrame &&
                        state.animation.currentFrame < clip.startFrame + clip.durationFrames * clip.repeatCount,
                ),
            )
            add(
                ContextIconAction(Lucide.Copy, "Duplicate", {
                    dispatch(TimelineIntent.ActorPlacement.Duplicate(clip.placementId))
                }),
            )
            add(
                ContextIconAction(Lucide.ArrowUpToLine, "Bring to front", {
                    dispatch(TimelineIntent.ActorPlacement.BringToFront(clip.placementId))
                }),
            )
            add(
                ContextIconAction(Lucide.ArrowDownToLine, "Send to back", {
                    dispatch(TimelineIntent.ActorPlacement.SendToBack(clip.placementId))
                }),
            )
            add(
                ContextIconAction(Lucide.BookmarkPlus, "Add to library", {
                    onDismiss()
                    onSaveActor(clip)
                }),
            )
            add(
                ContextIconAction(Lucide.Trash2, "Delete", {
                    dispatch(TimelineIntent.ActorPlacement.Remove(clip.placementId))
                }),
            )
        }
        sound?.let { clip ->
            add(
                ContextIconAction(Lucide.AudioLines, "Voice style", {
                    showVoice = true
                    onDismiss()
                }, !locked),
            )
            linkedVideo?.let { linked ->
                add(
                    ContextIconAction(Lucide.Unlink, "Detach sound from video", {
                        dispatch(TimelineIntent.Video.DetachAudio(linked.clipId))
                    }),
                )
            }
            add(
                ContextIconAction(
                    Lucide.Blend,
                    "Crossfade to next clip",
                    {
                        showCrossfade = true
                        onDismiss()
                    },
                    !locked && linkedVideo == null && nextAudio != null &&
                        state.project.videoClips.values.none { it.linkedAudioClipId == nextAudio.clipId },
                ),
            )
            add(
                ContextIconAction(Lucide.Blend, "Audio fades", {
                    showFades = true
                    onDismiss()
                }, !locked),
            )
            add(
                ContextIconAction(Lucide.Copy, "Duplicate", {
                    dispatch(TimelineIntent.Audio.Duplicate(clip.clipId))
                }, !locked),
            )
            add(
                ContextIconAction(
                    Lucide.Scissors,
                    "Split at playhead",
                    {
                        dispatch(TimelineIntent.Audio.Split(clip.clipId, state.animation.currentFrame))
                    },
                    !locked && linkedVideo == null && state.animation.currentFrame > clip.startFrame &&
                        state.animation.currentFrame < clip.startFrame + clip.durationFrames,
                ),
            )
            add(
                ContextIconAction(Lucide.BookmarkPlus, "Add to library", {
                    onDismiss()
                    onSaveAudio(clip)
                }),
            )
            add(
                ContextIconAction(
                    Lucide.Trash2,
                    "Delete",
                    { dispatch(TimelineIntent.Audio.Remove(clip.clipId)) },
                    !locked && linkedVideo == null,
                ),
            )
        }
        video?.let { clip ->
            if (clip.isSceneContent) {
                add(
                    ContextIconAction(Lucide.Scan, "Video fit and crop", {
                        showFraming = true
                        onDismiss()
                    }),
                )
            }
            add(
                ContextIconAction(
                    Lucide.Scissors,
                    "Split at playhead",
                    {
                        dispatch(TimelineIntent.Video.Split(clip.clipId, state.animation.currentFrame))
                    },
                    !locked && state.animation.currentFrame > clip.timelineStartFrame &&
                        state.animation.currentFrame < clip.timelineEndFrame,
                ),
            )
            if (clip.isSceneContent) {
                add(
                    ContextIconAction(Lucide.Scan, "Transform video", {
                        dispatch(TimelineIntent.Video.Select(clip.clipId))
                    }),
                )
            }
            add(
                ContextIconAction(Lucide.Trash2, "Delete", {
                    dispatch(TimelineIntent.Video.Remove(clip.clipId))
                }, !locked),
            )
        }
        if (sound != null || video != null) {
            add(ContextIconAction(Lucide.Undo2, "Undo effect edit", { dispatch(TimelineIntent.Camera.Undo) }))
            add(ContextIconAction(Lucide.Redo2, "Redo effect edit", { dispatch(TimelineIntent.Camera.Redo) }))
        }
    }
    IconContextMenu(expanded, onDismiss, actions) {
        Row {
            actor?.let { clip ->
                SwitchPerformanceButton(clip, state.project.id, performances, { placement, performance ->
                    dispatch(
                        TimelineIntent.ActorPlacement.SwitchPerformance(
                            placement.placementId,
                            performance.performanceId,
                            performance.actorRevisionId,
                            performance.displayName,
                            durationFramesFor(performance, state.project.fps),
                        ),
                    )
                }, onCreatePerformance)
            }
            sound?.let { clip ->
                ContextSliderButton(
                    Lucide.Volume2,
                    "Volume",
                    clip.volume,
                    { onIntent(TimelineIntent.Audio.SetVolume(clip.clipId, it)) },
                    enabled = !locked,
                )
            }
            video?.takeUnless { it.isSceneContent }?.let {
                ContextSliderButton(
                    Lucide.Blend,
                    "Reference opacity",
                    state.project.videoTrackOpacity,
                    { onIntent(TimelineIntent.Video.SetTrackOpacity(it)) },
                )
            }
        }
    }
    if (showFades &&
        sound != null
    ) {
        AudioFadeDialog(sound, state.project.fps, { showFades = false }, { incoming, outgoing ->
            showFades = false
            onIntent(TimelineIntent.Audio.SetFades(sound.clipId, incoming, outgoing))
        })
    }
    if (showCrossfade && sound != null && nextAudio != null) {
        AudioCrossfadeDialog(
            sound,
            nextAudio,
            state.project.fps,
            { showCrossfade = false },
            { frames ->
                showCrossfade = false
                onIntent(TimelineIntent.Audio.Crossfade(sound.clipId, nextAudio.clipId, frames))
            },
        )
    }
    if (showFraming && video != null) {
        VideoFramingDialog(
            video,
            state.project,
            state.animation.currentFrame,
            { showFraming = false },
            { fit, x, y ->
                showFraming = false
                onIntent(TimelineIntent.Video.Framing(video.clipId, fit, x, y))
            },
        )
    }
    if (showVoice && sound != null) {
        VoiceEffectDialog(sound, state.project, { showVoice = false }, { effect, asset ->
            showVoice = false
            onIntent(TimelineIntent.Audio.VoiceStyle(sound.clipId, sound.assetId, effect, asset))
        })
    }
}
