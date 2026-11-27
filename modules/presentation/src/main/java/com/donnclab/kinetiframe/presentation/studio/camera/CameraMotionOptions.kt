package com.donnclab.kinetiframe.presentation.studio.camera

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraEasing
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import com.donnclab.kinetiframe.core.document.camera.ShakeDirection
import com.donnclab.kinetiframe.presentation.R
import kotlin.math.roundToInt

/** Advanced controls live behind one context action, leaving the framing surface uncluttered. */
@Composable
internal fun CameraMotionOptions(shot: CameraShot, fps: Int, onChange: (CameraShot) -> Unit, onDismiss: () -> Unit) {
    val span = shot.endFrame - shot.startFrame
    val wait = shot.waitFrames.coerceAtMost(span - 1)
    val settle = shot.settleFrames.coerceAtMost(span - wait - 1)
    val procedural = shot.action == CameraAction.SHAKE || shot.action == CameraAction.HANDHELD
    val waitDescription = stringResource(R.string.camera_wait_duration)
    val settleDescription = stringResource(R.string.camera_settle_duration)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.camera_motion_title)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.camera_wait_seconds, wait.toFloat() / fps))
            Slider(
                wait.toFloat(),
                {
                    onChange(
                        shot.copy(
                            waitFrames = it.roundToInt(),
                            settleFrames = minOf(
                                settle,
                                span - 1 - it.roundToInt(),
                            ),
                        ),
                    )
                },
                modifier = Modifier.semantics { contentDescription = waitDescription },
                valueRange =
                0f..(span - 1).coerceAtLeast(1).toFloat(),
                enabled = span > 1,
            )
            Text(stringResource(R.string.camera_move_seconds, (span - wait - settle).toFloat() / fps))
            Text(stringResource(R.string.camera_settle_seconds, settle.toFloat() / fps))
            Slider(
                settle.toFloat(),
                {
                    onChange(shot.copy(waitFrames = wait, settleFrames = it.roundToInt()))
                },
                modifier = Modifier.semantics { contentDescription = settleDescription },
                valueRange = 0f..(span - wait - 1).coerceAtLeast(1).toFloat(),
                enabled =
                span - wait > 1,
            )
            if (procedural) {
                val frequency = shot.frequencyHz ?: if (shot.action == CameraAction.SHAKE) 13f else 1.3f
                Text(stringResource(R.string.camera_frequency_hz, frequency))
                Slider(frequency, { onChange(shot.copy(frequencyHz = it)) }, valueRange = .2f..30f)
                Text(stringResource(R.string.camera_rotational_movement))
                Slider(shot.rotationStrength, { onChange(shot.copy(rotationStrength = it)) })
                var directions by remember { mutableStateOf(false) }
                TextButton({ directions = true }) {
                    Text(
                        stringResource(R.string.camera_direction_value, stringResource(shot.shakeDirection.labelRes())),
                    )
                }
                DropdownMenu(directions, { directions = false }) {
                    ShakeDirection.entries.forEach { direction ->
                        DropdownMenuItem(text = { Text(stringResource(direction.labelRes())) }, onClick = {
                            onChange(shot.copy(shakeDirection = direction))
                            directions =
                                false
                        })
                    }
                }
            } else {
                if (shot.action == CameraAction.FOLLOW) {
                    Text(stringResource(R.string.camera_follow_smoothing_seconds, shot.followSmoothingSeconds))
                    Slider(shot.followSmoothingSeconds, { onChange(shot.copy(followSmoothingSeconds = it)) })
                }
                var easing by remember { mutableStateOf(false) }
                TextButton({ easing = true }) {
                    Text(stringResource(R.string.camera_curve_value, stringResource(shot.easing.labelRes())))
                }
                DropdownMenu(easing, { easing = false }) {
                    CameraEasing.entries.forEach { curve ->
                        DropdownMenuItem(text = { Text(stringResource(curve.labelRes())) }, onClick = {
                            onChange(shot.copy(easing = curve))
                            easing =
                                false
                        })
                    }
                }
            }
            Text(stringResource(R.string.camera_phases_note), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.action_done)) } })
}

@StringRes
private fun ShakeDirection.labelRes(): Int = when (this) {
    ShakeDirection.BOTH -> R.string.camera_direction_both
    ShakeDirection.HORIZONTAL -> R.string.camera_direction_horizontal
    ShakeDirection.VERTICAL -> R.string.camera_direction_vertical
}

@StringRes
private fun CameraEasing.labelRes(): Int = when (this) {
    CameraEasing.SMOOTH -> R.string.camera_curve_smooth
    CameraEasing.LINEAR -> R.string.camera_curve_linear
    CameraEasing.EASE_IN -> R.string.camera_curve_ease_in
    CameraEasing.EASE_OUT -> R.string.camera_curve_ease_out
}

@Preview
@Composable
private fun MotionOptionsPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        CameraMotionOptions(CameraShot("preview", CameraAction.PAN, 0, 24), 12, {}, {})
    }
}
