package com.donnclab.kinetiframe.presentation.studio.frames

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
import androidx.compose.ui.res.stringResource
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
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
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
    val drag = remember(gridState) { FrameGridDragState(gridState) }

    val thumbnailEditBitmap = state.currentBitmap.takeUnless { state.animation.isPlaying }
    LaunchedEffect(state.project, state.animation.totalFrames, state.layers, thumbnailEditBitmap) {
        previews.clear()
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.index } }.collectLatest { visible ->
            val wanted = visible.flatMap { listOf(it - 1, it, it + 1) }
                .filter { it in 0 until state.animation.totalFrames }.toSet()
            val loaded = com.donnclab.kinetiframe.presentation.studio.timeline.sceneFrameThumbnails(
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
            modifier = Modifier.fillMaxSize().padding(Dimens.spaceM),
            shape = RoundedCornerShape(EditorTokens.frameStudioCorner),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Dimens.elevationM,
        ) {
            Column(Modifier.fillMaxSize().padding(Dimens.spaceL)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Lucide.X, stringResource(R.string.action_close)) }
                    Text(
                        text = if (selected.isEmpty()) {
                            "Frames · ${state.animation.totalFrames}"
                        } else {
                            "${selected.size} / ${state.animation.totalFrames} Selected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        onIntent(StudioIntent.Draw.Undo)
                    }) { Icon(Lucide.Undo, stringResource(R.string.editor_undo)) }
                    IconButton(onClick = {
                        onIntent(StudioIntent.Draw.Redo)
                    }) { Icon(Lucide.Redo, stringResource(R.string.editor_redo)) }

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
                            stringResource(
                                if (allSelected) R.string.action_deselect_all else R.string.action_select_all,
                            ),
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(EditorTokens.frameStudioCell),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
                    ) {
                        items(
                            items = (0 until state.animation.totalFrames).toList(),
                            key = { it },
                        ) { index ->
                            StudioFrameCard(
                                index = index,
                                bitmap = previews[index],
                                // While dragging, the frame under the finger shows where it lands.
                                selected = index in selected || (drag.dragging != null && drag.target == index),
                                modifier = with(drag) { Modifier.animateItem().lifted(index) }
                                    .frameDrag(drag, index) { from, to ->
                                        onIntent(StudioIntent.Frame.MoveFrame(from, to))
                                        selected.clear()
                                        selected.add(to)
                                    },
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
                            modifier = Modifier.padding(bottom = Dimens.spaceL),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Dimens.spaceS),
                            ) {
                                // One frame selected: move it a place earlier or later (R3-20). A bar of
                                // its own, since the main toolbar is already full on a phone.
                                selected.singleOrNull()?.let { only ->
                                    FrameMoveBar(
                                        canMoveEarlier = only > 0,
                                        canMoveLater = only < state.animation.totalFrames - 1,
                                        onMove = { step ->
                                            onIntent(StudioIntent.Frame.MoveFrame(only, only + step))
                                            selected.clear()
                                            selected.add(only + step)
                                        },
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                    shape = RoundedCornerShape(Dimens.radiusPill),
                                    tonalElevation = Dimens.elevationFloating,
                                    border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.25f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = Dimens.spaceM,
                                            vertical = Dimens.spaceXsPlus,
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceL),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val minIdx = selected.minOrNull() ?: state.animation.currentFrame
                                        val maxIdx = selected.maxOrNull() ?: state.animation.currentFrame

                                        StudioToolbarAction(
                                            icon = Lucide.ClipboardPaste,
                                            arrowLeft = true,
                                            label = stringResource(R.string.frame_studio_paste_before),
                                            onClick = {
                                                onIntent(StudioIntent.Frame.PasteFrames(minIdx, before = true))
                                            },
                                        )
                                        StudioToolbarAction(
                                            icon = Lucide.SquarePlus,
                                            arrowLeft = true,
                                            label = stringResource(R.string.frame_studio_add_before),
                                            onClick = { onIntent(StudioIntent.Frame.AddFrameAt(minIdx)) },
                                        )
                                        StudioToolbarAction(
                                            icon = Lucide.Trash2,
                                            label = stringResource(R.string.frame_studio_delete),
                                            onClick = {
                                                if (selected.size < state.animation.totalFrames) {
                                                    onIntent(StudioIntent.Frame.DeleteFrames(selected.toList()))
                                                    selected.clear()
                                                }
                                            },
                                        )
                                        StudioToolbarAction(
                                            icon = Lucide.Copy,
                                            label = stringResource(R.string.frame_studio_copy),
                                            onClick = { onIntent(StudioIntent.Frame.CopyFrames(selected.toList())) },
                                        )
                                        StudioToolbarAction(
                                            icon = Lucide.SquarePlus,
                                            arrowRight = true,
                                            label = stringResource(R.string.frame_studio_add_after),
                                            onClick = { onIntent(StudioIntent.Frame.AddFrameAt(maxIdx + 1)) },
                                        )
                                        StudioToolbarAction(
                                            icon = Lucide.ClipboardPaste,
                                            arrowRight = true,
                                            label = stringResource(R.string.frame_studio_paste_after),
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
            .clip(RoundedCornerShape(Dimens.radiusS))
            .clickable(onClick = onClick)
            .padding(Dimens.spaceXsPlus),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = EditorTokens.dark,
            modifier = Modifier.size(Dimens.iconM),
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
                    .size(EditorTokens.frameStudioArrow)
                    .align(if (arrowLeft) Alignment.TopStart else Alignment.TopEnd)
                    .offset(x = if (arrowLeft) -Dimens.spaceXs else Dimens.spaceXs, y = -Dimens.spaceXs),
            )
        }
    }
}
