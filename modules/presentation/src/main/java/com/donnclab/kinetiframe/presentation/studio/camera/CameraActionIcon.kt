// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.camera

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import kotlin.math.PI
import kotlin.math.sin

/** How long one loop of a camera icon's preview takes. */
private const val LOOP_MS = 1600

/**
 * A camera move drawn as what it does to the picture (R3 tablet feedback): the frame pushing in or
 * pulling out, a sweep, a shake, a fade. No icon set has these, so they are drawn here. With
 * [playing] the icon loops the move, so the effect can be seen before it is chosen.
 */
@Composable
fun CameraActionIcon(action: CameraAction, colour: Color, playing: Boolean, modifier: Modifier = Modifier) {
    val t = if (playing) {
        val loop by rememberInfiniteTransition(label = "camera-icon").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "camera-icon-time",
        )
        loop
    } else {
        IDLE_T
    }
    Canvas(modifier) { drawCameraAction(action, colour, t) }
}

/** The pose a still icon shows: part way through the move, so its direction reads. */
private const val IDLE_T = 0.6f

private fun DrawScope.drawCameraAction(action: CameraAction, colour: Color, t: Float) {
    val line = Stroke(width = size.minDimension * STROKE, cap = StrokeCap.Round)
    val w = size.width
    val h = size.height

    // The picture is a subject in a frame; the move changes how the frame sees it.
    fun subject(scale: Float = 1f, dx: Float = 0f, dy: Float = 0f) {
        val r = h * SUBJECT * scale
        drawCircle(colour, r, Offset(w / 2 + dx, h / 2 + dy - r * 0.3f), style = line)
        drawLine(
            colour,
            Offset(w / 2 + dx - r, h / 2 + dy + r * 1.2f),
            Offset(w / 2 + dx + r, h / 2 + dy + r * 1.2f),
            line.width,
        )
    }
    fun frame(inset: Float = INSET) {
        drawRect(colour, Offset(w * inset, h * inset), Size(w * (1 - 2 * inset), h * (1 - 2 * inset)), style = line)
    }
    when (action) {
        CameraAction.PUSH_IN -> {
            frame()
            subject(scale = 1f + t * 0.8f)
            arrowsCorners(colour, line, inward = true)
        }

        CameraAction.PULL_OUT -> {
            frame()
            subject(scale = 1.8f - t * 0.8f)
            arrowsCorners(colour, line, inward = false)
        }

        CameraAction.PAN -> {
            frame()
            subject(dx = w * (0.25f - t * 0.5f))
            arrow(colour, line, Offset(w * 0.3f, h * 0.85f), Offset(w * 0.7f, h * 0.85f))
        }

        CameraAction.TILT -> {
            frame()
            subject(dy = h * (0.2f - t * 0.4f))
            arrow(colour, line, Offset(w * 0.88f, h * 0.7f), Offset(w * 0.88f, h * 0.3f))
        }

        CameraAction.SHAKE -> {
            val jolt = sin(t * 2 * PI * SHAKES).toFloat() * w * 0.05f
            translate(left = jolt) { frame() }
            subject()
            zigzag(colour, line, w * 0.06f)
            zigzag(colour, line, w * 0.94f)
        }

        CameraAction.HANDHELD -> {
            rotate(sin(t * 2 * PI).toFloat() * SWAY_DEGREES) { frame() }
            subject()
        }

        CameraAction.FADE_IN, CameraAction.FADE_OUT -> {
            frame()
            subject()
            val dark = if (action == CameraAction.FADE_IN) 1f - t else t
            drawRect(
                colour.copy(alpha = dark * 0.85f),
                Offset(w * INSET, h * INSET),
                Size(
                    w * (1 - 2 * INSET),
                    h * (1 - 2 * INSET),
                ),
            )
        }

        CameraAction.WHIP_PAN -> {
            frame()
            for (i in 0..2) {
                val y = h * (0.35f + i * 0.15f)
                val start = w * (0.2f + ((t + i * 0.2f) % 1f) * 0.3f)
                drawLine(colour, Offset(start, y), Offset(start + w * 0.35f, y), line.width)
            }
        }

        CameraAction.FOLLOW -> {
            frame()
            val x = w * (0.35f + t * 0.3f)
            drawCircle(colour, h * SUBJECT * 0.7f, Offset(x, h * 0.55f), style = line)
            drawLine(colour, Offset(x - w * 0.22f, h * 0.55f), Offset(x - w * 0.12f, h * 0.55f), line.width)
            drawLine(colour, Offset(x - w * 0.3f, h * 0.55f), Offset(x - w * 0.26f, h * 0.55f), line.width)
        }
    }
}

private fun DrawScope.arrow(colour: Color, line: Stroke, from: Offset, to: Offset) {
    drawLine(colour, from, to, line.width)
    val head = size.minDimension * HEAD
    val dir = (to - from).let { it / it.getDistance() }
    val side = Offset(-dir.y, dir.x)
    drawLine(colour, to, to - dir * head + side * head * 0.6f, line.width)
    drawLine(colour, to, to - dir * head - side * head * 0.6f, line.width)
}

/** Short arrows at the frame's corners, pointing in (push) or out (pull). */
private fun DrawScope.arrowsCorners(colour: Color, line: Stroke, inward: Boolean) {
    val corners =
        listOf(Offset(0f, 0f), Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height))
    val centre = Offset(size.width / 2, size.height / 2)
    corners.forEach { corner ->
        val near = corner + (centre - corner) * 0.08f
        val far = corner + (centre - corner) * 0.24f
        if (inward) arrow(colour, line, near, far) else arrow(colour, line, far, near)
    }
}

private fun DrawScope.zigzag(colour: Color, line: Stroke, x: Float) {
    val path = Path().apply {
        moveTo(x, size.height * 0.3f)
        lineTo(x - size.width * 0.03f, size.height * 0.42f)
        lineTo(x + size.width * 0.03f, size.height * 0.54f)
        lineTo(x, size.height * 0.66f)
    }
    drawPath(path, colour, style = line)
}

private const val STROKE = 0.045f
private const val INSET = 0.12f
private const val SUBJECT = 0.12f
private const val HEAD = 0.08f
private const val SHAKES = 3
private const val SWAY_DEGREES = 6f
