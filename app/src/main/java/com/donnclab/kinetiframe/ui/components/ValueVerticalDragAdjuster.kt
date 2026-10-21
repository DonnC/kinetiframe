package com.donnclab.kinetiframe.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ValueVerticalDragAdjuster(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    sensitivity: Float = 200f,
    onTap: (() -> Unit)? = null,
    content: @Composable (Float) -> Unit,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(onTap) {
                if (onTap != null) {
                    detectTapGestures(onTap = { onTap() })
                }
            }
            .pointerInput(value, range) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag -= dragAmount
                        val delta = accumulatedDrag / sensitivity
                        if (kotlin.math.abs(delta) > 0.001f) {
                            val newValue = (value + delta).coerceIn(range)
                            onValueChange(newValue)
                            accumulatedDrag = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content(value)
    }
}
