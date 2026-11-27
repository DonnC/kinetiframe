// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R
import kotlin.math.pow

/** Frame rates animators reach for most: 6 and 8 for limited animation, 12 the classic "on twos". */
private val COMMON_FPS = listOf(6, 8, 12, 15, 24, 30)

/**
 * Picks a frame rate: chips for the common rates, a slider for anything between 1 and 30, and a live
 * preview that plays the same one-second bounce at [fps], so a low rate looks choppy and a high one
 * smooth before anything is drawn.
 */
@Composable
fun FrameRatePicker(fps: Int, onFpsChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        BouncePreview(fps)
        // Wraps instead of scrolling: in the studio's narrow dialog a scrolling row hid 24 and 30 (R3-29).
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
        ) {
            COMMON_FPS.forEach { rate ->
                FilterChip(
                    selected = rate == fps,
                    onClick = { onFpsChange(rate) },
                    label = { Text(stringResource(R.string.fps_chip, rate)) },
                )
            }
        }
        StandardSlider(
            value = fps.toFloat(),
            onValueChange = { onFpsChange(it.toInt()) },
            range = 1f..30f,
            steps = 28,
            label = stringResource(R.string.add_project_frame_rate),
            valueDisplay = { resources.getString(R.string.add_project_fps_value, it.toInt()) },
        )
        Text(
            stringResource(R.string.fps_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A ball crossing in one arc a second, drawn only at the frames [fps] allows. Every position of that
 * second shows faintly, so 6 fps reads as six spaced balls and 30 as a near-solid trail even in a
 * glance; a caption says it in words. A ball bouncing in place looked much the same at 12 and 24
 * (owner feedback).
 */
@Composable
private fun BouncePreview(fps: Int) {
    var frame by remember { mutableIntStateOf(0) }
    val rate = fps.coerceIn(1, 60)
    LaunchedEffect(rate) {
        // The frame comes from the clock, not from adding up waits, so the ball keeps the exact rate:
        // a wait plus the screen's next refresh would run slow (24 fps played at about 20).
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> frame = (((now - start) * rate / NANOS_PER_SECOND) % rate).toInt() }
        }
    }
    val ball = MaterialTheme.colorScheme.primary
    val ground = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(R.string.fps_preview_description, rate)
    Box(
        Modifier.fillMaxWidth().height(Dimens.fpsPreviewHeight)
            .clip(RoundedCornerShape(Dimens.radiusM))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.height * BALL_SIZE
            val floor = size.height * FLOOR
            // The arc peaks below the caption, so the two never overlap.
            val top = size.height * ARC_TOP
            drawLine(ground, Offset(0f, floor), Offset(size.width, floor), strokeWidth = GROUND_STROKE)
            // Every drawing of the second, faintly: the gaps between them are the rate.
            for (step in 0 until rate) {
                drawBall(ball.copy(alpha = TRAIL_ALPHA), step / rate.toFloat(), radius, floor, top)
            }
            drawBall(ball, frame / rate.toFloat(), radius, floor, top)
        }
        Text(
            pluralStringResource(R.plurals.fps_preview_caption, rate, rate),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart).padding(Dimens.spaceS),
        )
    }
}

/** The ball at time [t] (0 to 1) of a one-second arc across the box, squashed where it lands. */
private fun DrawScope.drawBall(colour: Color, t: Float, radius: Float, floor: Float, top: Float) {
    // A thrown ball rises and falls along a parabola: 0 on the floor, 1 at the top.
    val height = 1f - (2 * t - 1).pow(2)
    val squash = if (height < SQUASH_ZONE) 1f - (SQUASH_ZONE - height) * SQUASH_AMOUNT else 1f
    val y = floor - radius * squash - height * (floor - top - radius)
    val x = radius * 2 + t * (size.width - radius * 4)
    drawOval(
        colour,
        topLeft = Offset(x - radius / squash, y - radius * squash),
        size = Size(radius * 2 / squash, radius * 2 * squash),
    )
}

private const val TRAIL_ALPHA = 0.18f
private const val ARC_TOP = 0.32f
private const val BALL_SIZE = 0.09f
private const val FLOOR = 0.92f
private const val GROUND_STROKE = 2f
private const val SQUASH_ZONE = 0.08f
private const val SQUASH_AMOUNT = 3f

private const val NANOS_PER_SECOND = 1_000_000_000L

@Preview(showBackground = true)
@Composable
private fun FrameRatePickerPreview() {
    KinetiFrameTheme { FrameRatePicker(12, {}) }
}
