package com.donnclab.kinetiframe.studio.ui.timeline

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.ClipboardPlus
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Files
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.SquarePlus
import com.composables.icons.lucide.Trash2
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TimelineControls(
    modifier: Modifier = Modifier,
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    cacheManager: FrameCacheManager? = null,
    onLayerPanelToggle: () -> Unit = {},
    /** Shared with the actor/audio/background track lanes in [com.donnclab.kinetiframe.ui.components.
     *  timeline.TimelineTracksPanel] docked above this panel, so scrolling either one moves the
     *  other in lockstep -- see [com.donnclab.kinetiframe.studio.ui.timeline.TimelineScrollSync].
     *  Defaults to a freshly created one so existing callers/previews still compile unchanged. */
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    /** True only inside the Pack Creator micro-editor -- hides Play/Loop/FPS entirely (a pack never
     *  plays back as a movie) and always shows the frame list instead of the FPS slider. */
    packMode: Boolean = false,
    onAddPackItem: (() -> Unit)? = null,
    onEditPackItem: ((Int) -> Unit)? = null,
    actorRendererFactory: (() -> com.donnclab.kinetiframe.actors.render.ActorFrameRenderer)? = null,
) {
    val thumbnails = remember { mutableStateMapOf<Int, Bitmap>() }
    var contextFrameIndex by remember { mutableStateOf<Int?>(null) }
    var holdFrame by remember { mutableStateOf<Int?>(null) }
    var scopeFrame by remember { mutableStateOf<Int?>(null) }
    val activeLayer = state.layers.layers.getOrNull(state.layers.activeLayerIndex)
    fun linkedCount(frame: Int): Int {
        val exposures = state.layers.linkedCels[activeLayer?.id].orEmpty()
        val cel = exposures[frame] ?: return 0
        return exposures.values.count { it == cel }
    }
    holdFrame?.let { frame ->
        HoldDrawingDialog(frame, state.animation.totalFrames, { holdFrame = null }, { last ->
            holdFrame = null
            onIntent(StudioIntent.Frame.HoldDrawing(frame, last))
        })
    }
    scopeFrame?.let { frame ->
        LinkedDrawingScopeDialog(
            linkedCount(frame),
            { scopeFrame = null },
            {
                scopeFrame = null
                onIntent(StudioIntent.Frame.EditDrawingScope(frame, false))
            },
            {
                scopeFrame = null
                onIntent(StudioIntent.Frame.EditDrawingScope(frame, true))
            },
            {
                scopeFrame = null
                holdFrame = frame
            },
        )
    }
    val contextMenuOffset = with(LocalDensity.current) { IntOffset(0, -56.dp.roundToPx()) }

    LaunchedEffect(state.animation.currentFrame) {
        val frame = state.animation.currentFrame
        if (!listState.isScrollInProgress && centeredTimelineFrame(listState) != frame) {
            listState.scrollToItem(frame)
        }
    }
    // Playback replaces the full composite every tick. Thumbnails are scene composites;
    // restarting their decoder collector at playback FPS creates needless churn/flicker.
    val thumbnailEditBitmap = state.currentBitmap.takeUnless {
        state.animation.isPlaying || listState.isScrollInProgress
    }
    LaunchedEffect(
        state.project,
        state.animation.totalFrames,
        state.layers.layers,
        state.layers.linkedCels,
        thumbnailEditBitmap,
        cacheManager,
    ) {
        thumbnails.clear()
        if (cacheManager != null) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }.collectLatest { visible ->
                val wanted = visible.flatMap { listOf(it - 1, it, it + 1) }
                    .filter { it in 0 until state.animation.totalFrames }.toSet()
                val loaded = sceneFrameThumbnails(state, cacheManager, wanted, actorRendererFactory)
                thumbnails.keys.toList().filterNot { it in wanted }.forEach { thumbnails.remove(it) }
                thumbnails.putAll(loaded)
            }
        }
    }

    BoxWithConstraints(modifier) {
        val compactTransport = maxWidth < 600.dp
        contextFrameIndex?.let { frameIndex ->
            Popup(
                alignment = Alignment.TopCenter,
                offset = contextMenuOffset,
                onDismissRequest = { contextFrameIndex = null },
                properties = PopupProperties(focusable = true),
            ) {
                FrameContextToolbar(
                    index = frameIndex,
                    onIntent = onIntent,
                    onDismiss = { contextFrameIndex = null },
                    onEditDetails = if (packMode) onEditPackItem?.let { edit -> { edit(frameIndex) } } else null,
                    onHeldDrawing = if (!packMode &&
                        activeLayer?.type == com.donnclab.kinetiframe.core.document.LayerType.RASTER &&
                        !activeLayer.isLocked
                    ) {
                        (
                            {
                                if (linkedCount(frameIndex) > 0) scopeFrame = frameIndex else holdFrame = frameIndex
                            }
                            )
                    } else {
                        null
                    },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .width(IntrinsicSize.Min),
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, EditorTokens.border.copy(alpha = 0.2f)),
        ) {
            val hasAudio = !packMode && state.project.audioClips.isNotEmpty()
            Row(
                Modifier.fillMaxWidth().height(if (hasAudio) 76.dp else 56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!packMode) {
                    PlaybackControls(
                        isPlaying = state.animation.isPlaying,
                        onIntent = onIntent,
                        compact = compactTransport,
                    )
                }
                Column(Modifier.weight(1f)) {
                    if (hasAudio) {
                        com.donnclab.kinetiframe.studio.ui.timeline.audio.ProjectAudioScrubRibbon(
                            clips = state.project.audioClips.values,
                            mutedTracks = state.project.audioTracks.filterValues { it.isMuted }.keys,
                            currentFrame = if (state.animation.isPlaying) {
                                state.animation.playbackFrame
                            } else {
                                state.animation.currentFrame
                            },
                            totalFrames = state.animation.totalFrames,
                            listState = listState,
                            onSeek = { onIntent(StudioIntent.Playback.SetCurrentFrame(it)) },
                            modifier = Modifier.fillMaxWidth().padding(end = 40.dp),
                        )
                    }
                    TimelineFrameList(
                        state = state,
                        onIntent = onIntent,
                        listState = listState,
                        thumbnails = thumbnails,
                        onContextRequested = { contextFrameIndex = it },
                        onContextDismissed = { contextFrameIndex = null },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onAddFrame = onAddPackItem,
                    )
                }
                IconButton(onClick = onLayerPanelToggle, modifier = Modifier.size(48.dp)) {
                    Icon(Lucide.Layers, "Layers", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimelineControlsPreview() {
    TimelineControls(
        state = StudioState(animation = AnimationState(currentFrame = 12, isPlaying = false)),
        onIntent = {},
    )
}
