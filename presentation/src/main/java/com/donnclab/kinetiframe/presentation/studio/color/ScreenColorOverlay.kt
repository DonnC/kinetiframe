package com.donnclab.kinetiframe.presentation.studio.color

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
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.ContextIconButton

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
                drawCircle(
                    Color.White,
                    EditorTokens.loupeRadius.toPx(),
                    point,
                    style = Stroke(EditorTokens.loupeRingStroke.toPx()),
                )
                drawCircle(
                    Color.Black,
                    EditorTokens.loupeRadius.toPx(),
                    point,
                    style = Stroke(EditorTokens.loupeInnerStroke.toPx()),
                )
                drawLine(
                    Color.Black,
                    point - Offset(EditorTokens.loupeCross.toPx(), 0f),
                    point + Offset(EditorTokens.loupeCross.toPx(), 0f),
                    EditorTokens.loupeCrossStroke.toPx(),
                )
                drawLine(
                    Color.Black,
                    point - Offset(0f, EditorTokens.loupeCross.toPx()),
                    point + Offset(0f, EditorTokens.loupeCross.toPx()),
                    EditorTokens.loupeCrossStroke.toPx(),
                )
            }
        }
        Surface(
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(Dimens.spaceS),
            shape = MaterialTheme.shapes.large,
            tonalElevation = Dimens.elevationM,
        ) {
            Row(Modifier.padding(horizontal = Dimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
                state.color?.let { color ->
                    Box(Modifier.size(EditorTokens.loupeSwatch).background(Color(color), MaterialTheme.shapes.small))
                }
                if (state.capturing) CircularProgressIndicator(Modifier.size(Dimens.iconL))
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
