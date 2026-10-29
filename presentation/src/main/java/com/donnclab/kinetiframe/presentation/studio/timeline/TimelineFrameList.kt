package com.donnclab.kinetiframe.presentation.studio.timeline

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

@Composable
fun TimelineFrameList(
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    listState: LazyListState,
    thumbnails: Map<Int, Bitmap>,
    onContextRequested: (Int) -> Unit,
    onContextDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    onAddFrame: (() -> Unit)? = null,
) {
    ObserveTimelineScrubbing(
        listState,
        FRAME_CELL_WIDTH,
        state.animation.currentFrame,
        state.animation.totalFrames,
        onStart = {
            onContextDismissed()
            onIntent(StudioIntent.Playback.Pause)
        },
        onFrame = { onIntent(StudioIntent.Playback.SetCurrentFrame(it)) },
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        BoxWithConstraints(Modifier.weight(1f)) {
            val edge = ((maxWidth - 36.dp) / 2).coerceAtLeast(0.dp)
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = edge),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(state.animation.totalFrames) { index ->
                    Box {
                        FrameCard(
                            index = index,
                            isCurrent = index == state.animation.currentFrame,
                            thumbnail =
                            thumbnails[index]
                                ?: if (index == state.animation.currentFrame) state.currentBitmap else null,
                            linked = state.layers.layers.getOrNull(state.layers.activeLayerIndex)?.id?.let {
                                state.layers.linkedCels[it]?.containsKey(index)
                            } == true,
                            onClick = {
                                if (index == state.animation.currentFrame) {
                                    onContextRequested(index)
                                } else {
                                    onContextDismissed()
                                    onIntent(StudioIntent.Playback.SetCurrentFrame(index))
                                }
                            },
                        )
                    }
                }
            }
            TimelineCenterPointer(Modifier.align(Alignment.TopCenter))
        }
        // A pinned blank cell stays reachable even on very long timelines. It is
        // outside lazy items so track-to-frame scroll math never includes a fake frame.
        InlineAddFrame {
            onContextDismissed()
            onAddFrame?.invoke() ?: onIntent(StudioIntent.Frame.AddFrame)
        }
    }
}
