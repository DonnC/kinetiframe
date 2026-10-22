package com.donnclab.kinetiframe.studio.ui.frames

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Redo
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.SquarePlus
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Undo
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FrameStudio(
    state: StudioState,
    cacheManager: FrameCacheManager,
    onIntent: (StudioIntent) -> Unit,
    onOpenFrame: (Int) -> Unit,
    onDismiss: () -> Unit,
    actorRendererFactory: (() -> com.donnclab.kinetiframe.actors.render.ActorFrameRenderer)? = null,
) {
    // Frame Studio owns batch selection.  The compact editor timeline only navigates.
    val selected = remember { mutableStateListOf<Int>() }
    val previews = remember { mutableStateMapOf<Int, Bitmap>() }
    val gridState = rememberLazyGridState()

    val thumbnailEditBitmap = state.currentBitmap.takeUnless { state.animation.isPlaying }
    LaunchedEffect(state.project, state.animation.totalFrames, state.layers, thumbnailEditBitmap) {
        previews.clear()
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.index } }.collectLatest { visible ->
            val wanted = visible.flatMap { listOf(it - 1, it, it + 1) }
                .filter { it in 0 until state.animation.totalFrames }.toSet()
            val loaded = com.donnclab.kinetiframe.studio.ui.timeline.sceneFrameThumbnails(
                state,
                cacheManager,
                wanted,
                actorRendererFactory,
            )
            previews.keys.toList().filterNot { it in wanted }.forEach { previews.remove(it) }
            previews.putAll(loaded)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Lucide.X, "Close") }
                    Text(
                        text = if (selected.isEmpty()) {
                            "Frames · ${state.animation.totalFrames}"
                        } else {
                            "${selected.size} / ${state.animation.totalFrames} Selected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onIntent(StudioIntent.Draw.Undo) }) { Icon(Lucide.Undo, "Undo") }
                    IconButton(onClick = { onIntent(StudioIntent.Draw.Redo) }) { Icon(Lucide.Redo, "Redo") }

                    val allSelected = selected.size == state.animation.totalFrames && state.animation.totalFrames > 0
                    IconButton(
                        onClick = {
                            if (allSelected) {
                                selected.clear()
                            } else {
                                selected.clear()
                                selected.addAll((0 until state.animation.totalFrames).toList())
                            }
                        },
                    ) {
                        Icon(
                            if (allSelected) Lucide.Square else Lucide.CheckCheck,
                            if (allSelected) "Deselect All" else "Select All",
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = (0 until state.animation.totalFrames).toList(),
                            key = { it },
                        ) { index ->
                            StudioFrameCard(
                                index = index,
                                bitmap = previews[index],
                                selected = index in selected,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    if (index in selected) selected.remove(index) else selected.add(index)
                                },
                                onOpen = { onOpenFrame(index) },
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedVisibility(
                            visible = selected.isNotEmpty(),
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                            modifier = Modifier.padding(bottom = 16.dp),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                shape = RoundedCornerShape(20.dp),
                                tonalElevation = 6.dp,
                                border = BorderStroke(1.dp, EditorTokens.border.copy(alpha = 0.25f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val minIdx = selected.minOrNull() ?: state.animation.currentFrame
                                    val maxIdx = selected.maxOrNull() ?: state.animation.currentFrame

                                    StudioToolbarAction(
                                        icon = Lucide.ClipboardPaste,
                                        arrowLeft = true,
                                        label = "PASTE",
                                        onClick = { onIntent(StudioIntent.Frame.PasteFrames(minIdx, before = true)) },
                                    )
                                    StudioToolbarAction(
                                        icon = Lucide.SquarePlus,
                                        arrowLeft = true,
                                        label = "ADD",
                                        onClick = { onIntent(StudioIntent.Frame.AddFrameAt(minIdx)) },
                                    )
                                    StudioToolbarAction(
                                        icon = Lucide.Trash2,
                                        label = "ERASE",
                                        onClick = {
                                            if (selected.size < state.animation.totalFrames) {
                                                onIntent(StudioIntent.Frame.DeleteFrames(selected.toList()))
                                                selected.clear()
                                            }
                                        },
                                    )
                                    StudioToolbarAction(
                                        icon = Lucide.Copy,
                                        label = "COPY",
                                        onClick = { onIntent(StudioIntent.Frame.CopyFrames(selected.toList())) },
                                    )
                                    StudioToolbarAction(
                                        icon = Lucide.SquarePlus,
                                        arrowRight = true,
                                        label = "ADD",
                                        onClick = { onIntent(StudioIntent.Frame.AddFrameAt(maxIdx + 1)) },
                                    )
                                    StudioToolbarAction(
                                        icon = Lucide.ClipboardPaste,
                                        arrowRight = true,
                                        label = "PASTE",
                                        onClick = {
                                            onIntent(StudioIntent.Frame.PasteFrames(maxIdx, before = false))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioToolbarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    arrowLeft: Boolean = false,
    arrowRight: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = EditorTokens.dark,
            modifier = Modifier.size(20.dp),
        )
        if (arrowLeft || arrowRight) {
            Icon(
                imageVector = if (arrowLeft) {
                    Icons.AutoMirrored.Rounded.ArrowBack
                } else {
                    Icons.AutoMirrored.Rounded.ArrowForward
                },
                contentDescription = null,
                tint = EditorTokens.accent,
                modifier = Modifier
                    .size(10.dp)
                    .align(if (arrowLeft) Alignment.TopStart else Alignment.TopEnd)
                    .offset(x = if (arrowLeft) (-4).dp else 4.dp, y = (-4).dp),
            )
        }
    }
}
