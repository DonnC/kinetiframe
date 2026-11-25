// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
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

/** A ball crossing and bouncing once a second, drawn only at the frames [fps] allows. */
@Composable
private fun BouncePreview(fps: Int) {
    var frame by remember { mutableIntStateOf(0) }
    val rate = fps.coerceIn(1, 60)
    LaunchedEffect(rate) {
        frame = 0
        while (true) {
            delay(1000L / rate)
            frame = (frame + 1) % rate
        }
    }
    val ball = MaterialTheme.colorScheme.primary
    val ground = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(R.string.fps_preview_description, rate)
    Canvas(
        Modifier.fillMaxWidth().height(Dimens.fpsPreviewHeight)
            .clip(RoundedCornerShape(Dimens.radiusM))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description },
    ) {
        val t = frame / rate.toFloat()
        val radius = size.height * 0.12f
        val floor = size.height * 0.85f
        val lift = abs(sin(PI * 2 * t)).toFloat()
        val squash = if (lift < 0.12f) 1f - (0.12f - lift) * 2.5f else 1f
        val x = radius * 2 + (size.width - radius * 4) * t
        val y = floor - radius * squash - lift * (floor - radius * 3)
        drawLine(ground, Offset(0f, floor), Offset(size.width, floor), strokeWidth = 2f)
        drawOval(
            ball,
            topLeft = Offset(x - radius / squash, y - radius * squash),
            size = Size(radius * 2 / squash, radius * 2 * squash),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FrameRatePickerPreview() {
    KinetiFrameTheme { FrameRatePicker(12, {}) }
}
