package com.donnclab.kinetiframe.studio.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.common.ui.ContextIconAction
import com.donnclab.kinetiframe.common.ui.ContextIconButton
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@Composable
fun ScreenColorOverlay(state: ScreenColorState, origin: Offset, onIntent: (ScreenColorIntent) -> Unit) {
    var pointer by remember { mutableStateOf<Offset?>(null) }
    val latestIntent by rememberUpdatedState(onIntent)
    Box(
        Modifier.fillMaxSize().pointerInput(origin) {
            awaitEachGesture {
                val down = awaitFirstDown()
                pointer = down.position
                latestIntent(ScreenColorIntent.Point(down.position.x + origin.x, down.position.y + origin.y))
                down.consume()
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change != null && change.pressed) {
                        pointer = change.position
                        latestIntent(
                            ScreenColorIntent.Point(change.position.x + origin.x, change.position.y + origin.y),
                        )
                    }
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            pointer?.let { point ->
                drawCircle(Color.White, 24.dp.toPx(), point, style = Stroke(4.dp.toPx()))
                drawCircle(Color.Black, 24.dp.toPx(), point, style = Stroke(1.dp.toPx()))
                drawLine(Color.Black, point - Offset(8.dp.toPx(), 0f), point + Offset(8.dp.toPx(), 0f), 2.dp.toPx())
                drawLine(Color.Black, point - Offset(0f, 8.dp.toPx()), point + Offset(0f, 8.dp.toPx()), 2.dp.toPx())
            }
        }
        Surface(
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                state.color?.let { color ->
                    Box(Modifier.size(32.dp).background(Color(color), MaterialTheme.shapes.small))
                }
                if (state.capturing) CircularProgressIndicator(Modifier.size(24.dp))
                ContextIconButton(
                    ContextIconAction(
                        Icons.Filled.Check,
                        "Use sampled color",
                        {
                            onIntent(ScreenColorIntent.Confirm)
                        },
                        enabled =
                        state.color != null,
                    ),
                )
                ContextIconButton(
                    ContextIconAction(Icons.Filled.Close, "Cancel eyedropper", {
                        onIntent(ScreenColorIntent.Cancel)
                    }),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorOverlayPreview() {
    KinetiFrameTheme {
        ScreenColorOverlay(ScreenColorState(active = true, color = 0xFFFFAA00.toInt()), Offset.Zero, {})
    }
}
