package com.donnclab.kinetiframe.library.ui.packs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.GripHorizontal
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.library.model.PackTrayItem
import com.donnclab.kinetiframe.studio.ui.dock.dockContainer
import com.donnclab.kinetiframe.studio.ui.dock.dockDragHandle
import com.donnclab.kinetiframe.studio.ui.dock.dockSurface
import com.donnclab.kinetiframe.studio.ui.dock.rememberDraggableDockState

/** The drag lifecycle a [PackTray] item cell reports upward. This composable only ever deals in
 *  on-screen pixels ([positionInWindow] on start, a per-move [delta] afterward) -- it has no idea
 *  where the stage is or what the canvas's current pan/zoom looks like, so it can't turn a drop
 *  point into a stamp by itself. Only the caller (EditorScreen) holds the canvas matrix needed to
 *  do that, which is why interpreting [End] lives there, not here. */
sealed class PackDragEvent {
    data class Start(val item: PackTrayItem, val positionInWindow: Offset) : PackDragEvent()
    data class Move(val delta: Offset) : PackDragEvent()
    data object End : PackDragEvent()
    data object Cancel : PackDragEvent()
}

/**
 * A floating panel of the drawings inside one pack. The user drags an item out and drops it
 * anywhere on the active frame -- a mouth pack's "Ah" might land dead center, an eye pack's
 * "Wink" off to one side -- rather than tapping to stamp a single item dead-center. This tray
 * doesn't know or care what kind of drawings it holds; it just shows them and reports drags.
 *
 * Uses the same [com.donnclab.kinetiframe.studio.ui.dock.DraggableDockState] every floating panel in
 * the editor shares (see [com.donnclab.kinetiframe.studio.ui.dock.ToolDock]), plus its own minimize
 * toggle so it can be tucked out of the way without being dismissed while its items are still
 * being placed.
 */
@Composable
fun PackTray(
    packName: String,
    items: List<PackTrayItem>,
    loading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onDrag: (PackDragEvent) -> Unit,
) {
    val dockState = rememberDraggableDockState(Offset(24f, 140f))
    var minimized by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().dockContainer(dockState)) {
        Surface(
            modifier = Modifier
                .dockSurface(dockState)
                .widthIn(max = 220.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dockDragHandle(dockState)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Lucide.GripHorizontal, contentDescription = "Move pack tray", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        packName,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { minimized = !minimized }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (minimized) Lucide.ChevronRight else Lucide.ChevronDown,
                            contentDescription = if (minimized) "Expand pack" else "Minimize pack",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Lucide.X, contentDescription = "Close pack", modifier = Modifier.size(16.dp))
                    }
                }
                if (!minimized) {
                    HorizontalDivider()
                    Box(Modifier.padding(8.dp).heightIn(min = 56.dp, max = 260.dp)) {
                        when {
                            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }

                            error != null -> Text(
                                error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )

                            items.isEmpty() -> Text(
                                "This pack has no drawings.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            else -> LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(items, key = { it.fileName }) { item -> PackTrayItemCell(item, onDrag) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One draggable stamp inside the tray -- tap-and-drag reports [PackDragEvent.Start] with this
 *  cell's own on-screen position (captured once at layout via [onGloballyPositioned], since the
 *  cell itself never moves) plus the finger's own offset within it, so the caller can render a
 *  ghost that starts exactly under the finger rather than snapping to the cell's corner. */
@Composable
private fun PackTrayItemCell(item: PackTrayItem, onDrag: (PackDragEvent) -> Unit) {
    var positionInWindow by remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier = Modifier
            .size(64.dp)
            .onGloballyPositioned { coordinates -> positionInWindow = coordinates.positionInWindow() }
            .pointerInput(item.fileName) {
                detectDragGestures(
                    onDragStart = { touch -> onDrag(PackDragEvent.Start(item, positionInWindow + touch)) },
                    onDragEnd = { onDrag(PackDragEvent.End) },
                    onDragCancel = { onDrag(PackDragEvent.Cancel) },
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(PackDragEvent.Move(dragAmount))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = item.thumbnail.asImageBitmap(),
            contentDescription = item.name,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x11000000)),
        )
        Text(
            item.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
