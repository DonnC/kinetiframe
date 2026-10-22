package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens

@Composable
fun OpacityDrag(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    onVisibilityToggle: () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .width(44.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isVisible) {
                    EditorTokens.accent.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .pointerInput(opacity, isVisible) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var currentOpacity = opacity
                    val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (dragStart == null) {
                        onVisibilityToggle()
                    } else {
                        fun applyDrag(change: androidx.compose.ui.input.pointer.PointerInputChange) {
                            val previous = currentOpacity
                            currentOpacity = (currentOpacity - change.positionChange().y / 180f).coerceIn(0f, 1f)
                            if ((currentOpacity * 100).toInt() != (previous * 100).toInt()) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onOpacityChange(currentOpacity)
                            change.consume()
                        }
                        applyDrag(dragStart)
                        verticalDrag(down.id) { change -> applyDrag(change) }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${(opacity * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = if (isVisible) EditorTokens.accent else EditorTokens.muted,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
