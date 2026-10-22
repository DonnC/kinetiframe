package com.donnclab.kinetiframe.ui.components.rigging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.domain.actor.PerformanceRulerTiming
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey
import com.donnclab.kinetiframe.ui.StudioState

/**
 * Frame-facing view of a time-based gesture. The ruler never owns animation data: it simply
 * samples the durable microsecond timeline at the document's current FPS for familiar posing.
 */
@Composable
fun PerformanceFrameRuler(
    fps: Int,
    durationMicros: Long,
    currentTimeMicros: Long,
    keys: List<ActorPoseKey>,
    onFrameSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    artworkKeys: List<com.donnclab.kinetiframe.engine.rigging.model.ActorArtworkKey> = emptyList(),
    thumbnail: (@Composable (Long) -> android.graphics.Bitmap?)? = null,
) {
    val safeFps = fps.coerceAtLeast(1)
    val frameCount = PerformanceRulerTiming.frameCount(durationMicros, safeFps)
    val selectedFrame = if (currentTimeMicros == durationMicros) {
        frameCount
    } else {
        PerformanceRulerTiming.frameAt(currentTimeMicros, safeFps).coerceIn(0, frameCount)
    }
    val allKeys = remember(keys, artworkKeys) { keys + artworkKeys.map { ActorPoseKey(it.timeMicros, emptyMap()) } }
    val keyTimes =
        remember(allKeys, safeFps, durationMicros) {
            allKeys.distinctBy { it.timeMicros }.sortedBy { it.timeMicros }.groupBy {
                if (it.timeMicros ==
                    durationMicros
                ) {
                    frameCount
                } else {
                    PerformanceRulerTiming.frameAt(it.timeMicros, safeFps)
                }
            }
        }
    val keyedFrames = keyTimes.keys
    val poseFrames = remember(keys, safeFps, durationMicros) {
        keys.map {
            if (it.timeMicros == durationMicros) frameCount else PerformanceRulerTiming.frameAt(it.timeMicros, safeFps)
        }.toSet()
    }

    val scroll = rememberLazyListState()
    com.donnclab.kinetiframe.ui.components.timeline.ObserveTimelineScrubbing(
        scroll,
        44.dp,
        selectedFrame,
        frameCount + 1,
        onFrame = { onFrameSelected(PerformanceRulerTiming.timeAt(it, safeFps, durationMicros)) },
        onStart = { onFrameSelected(currentTimeMicros) },
    )
    LaunchedEffect(selectedFrame) {
        val visible = scroll.layoutInfo.visibleItemsInfo
        if (!scroll.isScrollInProgress &&
            com.donnclab.kinetiframe.ui.components.timeline.centeredTimelineFrame(scroll) != selectedFrame
        ) {
            scroll.scrollToItem(selectedFrame)
        }
    }
    BoxWithConstraints(modifier) {
        val edge = ((maxWidth - 44.dp) / 2).coerceAtLeast(0.dp)
        LazyRow(modifier = Modifier.fillMaxWidth(), state = scroll, contentPadding = PaddingValues(horizontal = edge)) {
            items(count = frameCount + 1) { frame ->
                val key = frame in keyedFrames
                val tweened = !key && isTweenedFrame(frame, poseFrames)
                val selected = frame == selectedFrame
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 48.dp)
                        .background(
                            when {
                                selected -> MaterialTheme.colorScheme.primaryContainer
                                tweened -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .semantics {
                            contentDescription =
                                "Frame ${frame + 1}${if (frame in poseFrames) {
                                    ", pose key"
                                } else if (key) {
                                    ", artwork key"
                                } else if (tweened) {
                                    ", interpolated"
                                } else {
                                    ""
                                }}"
                        }
                        .clickable {
                            // Select the authored timestamp, so Delete/replace act on the pose key
                            // even for imported keys that do not fall exactly on this FPS grid.
                            val authored = keyTimes[frame].orEmpty()
                            val next =
                                authored.firstOrNull { it.timeMicros > currentTimeMicros } ?: authored.firstOrNull()
                            onFrameSelected(
                                next?.timeMicros ?: PerformanceRulerTiming.timeAt(frame, safeFps, durationMicros),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    com.donnclab.kinetiframe.ui.components.FrameThumbnailContent(
                        thumbnail?.invoke(
                            if (selected) {
                                currentTimeMicros
                            } else {
                                keyTimes[frame]?.firstOrNull()?.timeMicros
                                    ?: PerformanceRulerTiming.timeAt(frame, safeFps, durationMicros)
                            },
                        ),
                        frame,
                        selected,
                        compact = true,
                    ) {
                        if (key) {
                            Box(
                                Modifier.align(Alignment.BottomStart).size(5.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }
            }
        }
        com.donnclab.kinetiframe.ui.components.timeline.TimelineCenterPointer(Modifier.align(Alignment.TopCenter))
    }
}

private fun isTweenedFrame(frame: Int, keyedFrames: Set<Int>): Boolean {
    val before = keyedFrames.any { it < frame }
    val after = keyedFrames.any { it > frame }
    return before && after
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PerformanceFrameRulerPreview() {
    PerformanceFrameRuler(
        fps = 12,
        durationMicros = 1_000_000L,
        currentTimeMicros = 500_000L,
        keys = listOf(
            ActorPoseKey(0L, emptyMap()),
            ActorPoseKey(500_000L, emptyMap()),
            ActorPoseKey(1_000_000L, emptyMap()),
        ),
        onFrameSelected = {},
    )
}
