package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens
import com.donnclab.kinetiframe.presentation.R

/** A layer's opacity. Tap for a slider; dragging the chip up or down also changes it. */
@Composable
fun OpacityDrag(opacity: Float, onOpacityChange: (Float) -> Unit, isVisible: Boolean, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var sliderOpen by remember { mutableStateOf(false) }
    val description = stringResource(R.string.layer_opacity, (opacity * 100).toInt())

    Box(
        modifier = modifier
            .width(TimelineTokens.opacityChipWidth)
            .height(TimelineTokens.opacityChipHeight)
            .clip(RoundedCornerShape(TimelineTokens.chipCorner))
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
                        sliderOpen = true
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
            }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.value_percent, (opacity * 100).toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = if (isVisible) EditorTokens.accent else EditorTokens.muted,
            fontWeight = FontWeight.ExtraBold,
        )
        DropdownMenu(expanded = sliderOpen, onDismissRequest = { sliderOpen = false }) {
            Column(Modifier.width(TimelineTokens.opacitySliderWidth).padding(horizontal = Dimens.spaceM)) {
                Text(description, style = MaterialTheme.typography.labelLarge)
                Slider(value = opacity, onValueChange = onOpacityChange, valueRange = 0f..1f)
            }
        }
    }
}
