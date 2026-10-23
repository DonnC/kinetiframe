package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.domain.ActorMotionManager
import com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.actorPreviewDuration
import kotlin.math.roundToLong

/** Compact gesture timeline: frame-facing controls over a durable, time-based performance. */
@Composable
fun ActorPoseTimeline(
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
    assets: RigAssetManager? = null,
) {
    val keys = state.activeActor()?.poseKeys.orEmpty()
    val artworkKeys = state.activeActor()?.artworkKeys.orEmpty()
    val curves = state.activeActor()?.rotationCurves.orEmpty()
    val roots = state.activeActor()?.rootMotionCurves.orEmpty()
    val poseMarkers = remember(keys, curves, roots) {
        (
            keys +
                (curves.flatMap { it.points }.map { it.timeMicros } + roots.flatMap { it.points }.map { it.timeMicros })
                    .map { ActorPoseKey(it, emptyMap()) }
            )
            .distinctBy { it.timeMicros }.sortedBy { it.timeMicros }
    }
    val rulerKeys = remember(poseMarkers, artworkKeys) {
        (
            poseMarkers +
                artworkKeys.map {
                    ActorPoseKey(it.timeMicros, emptyMap())
                }
            ).distinctBy { it.timeMicros }.sortedBy { it.timeMicros }
    }
    val fps = state.project.fps
    val frame = (state.actor.previewTimeMicros * fps / 1_000_000.0).roundToLong()
    val duration = state.actorPreviewDuration()
    val frameMicros = 1_000_000L / fps.coerceAtLeast(1)
    val preset = state.actor.presetPreview ?: state.activeActor()?.motionPreset
    val presetDuration = preset?.let { (it.recipe.durationSeconds * 1_000_000 / it.speed).toLong() } ?: 0L
    val minimumDuration = maxOf(frameMicros, rulerKeys.maxOfOrNull { it.timeMicros } ?: 0L, presetDuration)
    val hasPreset = state.activeActor()?.motionPreset != null || state.actor.presetPreview != null
    val taking = state.actor.takePhase != PerformanceTakePhase.IDLE
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PerformanceTakeControls(state, onIntent)
                if (!taking) {
                    Text(
                        "Frame ${frame + 1} · ${rulerKeys.size} keys",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.widthIn(min = 84.dp).padding(end = 6.dp),
                        maxLines = 1,
                    )
                    PerformanceDurationMenu(
                        duration,
                        minimumDuration,
                        ActorMotionManager.MAX_DURATION_MICROS,
                        frameMicros,
                    ) {
                        onIntent(StudioIntent.ActorMotion.SetDuration(it))
                    }
                    val previous = rulerKeys.lastOrNull { it.timeMicros < state.actor.previewTimeMicros }
                    val next = rulerKeys.firstOrNull { it.timeMicros > state.actor.previewTimeMicros }
                    ActorToolButton(Icons.Default.SkipPrevious, "Previous pose key", previous != null) {
                        previous?.let { onIntent(StudioIntent.ActorMotion.Scrub(it.timeMicros)) }
                    }
                    ActorToolButton(Icons.Default.SkipNext, "Next pose key", next != null) {
                        next?.let { onIntent(StudioIntent.ActorMotion.Scrub(it.timeMicros)) }
                    }
                    ActorToolButton(
                        if (state.actor.previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play or pause actor preview",
                        rulerKeys.isNotEmpty() || !state.activeActor()?.rotationCurves.isNullOrEmpty() || hasPreset,
                    ) {
                        onIntent(StudioIntent.ActorMotion.TogglePlayback)
                    }
                    ActorMotionTimingMenu(state, onIntent, enabled = !hasPreset)
                    val contact = state.actorDefinition()?.sceneGraph?.let {
                        ActorMotionPose.contactBone(
                            it,
                            state.scene.selectedBoneId,
                        )
                    }
                    ActorContactMenu(
                        !hasPreset && contact != null,
                        contact in
                            ActorPoseTrack.contacts(
                                keys,
                                state.actor.previewTimeMicros,
                            ),
                    ) {
                        onIntent(StudioIntent.ActorMotion.ToggleContact)
                    }
                    ActorToolButton(Icons.Default.Add, "Record pose key", !hasPreset) {
                        onIntent(StudioIntent.ActorMotion.RecordKey)
                    }
                    ActorToolButton(
                        Icons.Default.DeleteOutline,
                        "Delete pose key",
                        !hasPreset && keys.any { it.timeMicros == state.actor.previewTimeMicros },
                    ) {
                        onIntent(StudioIntent.ActorMotion.DeleteKey)
                    }
                    if (artworkKeys.any { it.timeMicros == state.actor.previewTimeMicros }) {
                        ActorToolButton(Icons.Default.DeleteOutline, "Delete artwork key") {
                            onIntent(StudioIntent.ActorMotion.DeleteArtworkKey)
                        }
                    }
                }
            }
            PerformanceFrameRuler(
                fps = fps,
                durationMicros = duration,
                currentTimeMicros = state.actor.previewTimeMicros,
                keys = poseMarkers,
                artworkKeys = artworkKeys,
                thumbnail = { time -> performancePoseThumbnail(state, assets, time) },
                onFrameSelected = {
                    if (!taking ||
                        state.actor.takePhase == PerformanceTakePhase.REVIEW
                    ) {
                        onIntent(StudioIntent.ActorMotion.Scrub(it))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
        }
    }
}

@Preview(widthDp = 500)
@Composable
private fun ActorPoseTimelinePreview() {
    ActorPoseTimeline(StudioState(), {})
}
