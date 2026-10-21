package com.donnclab.kinetiframe.ui.components.dock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FloatingDock(
    modifier: Modifier = Modifier,
    initialOffset: Offset,
    onOffsetChange: (Offset) -> Unit,
    paddingDp: Int = 16,
    content: @Composable () -> Unit,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var dockSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    var localOffset by remember { mutableStateOf(initialOffset) }
    var isDragging by remember { mutableStateOf(false) }

    fun boundedOffset(offset: Offset): Offset {
        if (containerSize == IntSize.Zero || dockSize == IntSize.Zero) return offset

        val paddingPx = with(density) { paddingDp.dp.toPx() }
        val maxX = (containerSize.width - dockSize.width - paddingPx).coerceAtLeast(paddingPx)
        val maxY = (containerSize.height - dockSize.height - paddingPx).coerceAtLeast(paddingPx)
        return Offset(
            offset.x.coerceIn(paddingPx, maxX),
            offset.y.coerceIn(paddingPx, maxY),
        )
    }

    LaunchedEffect(initialOffset, containerSize, dockSize) {
        if (!isDragging) localOffset = boundedOffset(initialOffset)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(localOffset.x.roundToInt(), localOffset.y.roundToInt()) }
                .onSizeChanged { dockSize = it },
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        ) {
            Column(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DragHandle(
                    modifier = Modifier.pointerInput(containerSize, dockSize) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragCancel = {
                                isDragging = false
                                localOffset = boundedOffset(initialOffset)
                            },
                            onDragEnd = {
                                isDragging = false
                                val finalOffset = boundedOffset(localOffset)
                                localOffset = finalOffset
                                onOffsetChange(finalOffset)
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            localOffset = boundedOffset(localOffset + dragAmount)
                        }
                    },
                )
                content()
            }
        }
    }
}
