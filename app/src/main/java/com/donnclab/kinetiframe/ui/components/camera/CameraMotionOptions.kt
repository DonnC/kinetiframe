package com.donnclab.kinetiframe.ui.components.camera

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.engine.camera.CameraAction
import com.donnclab.kinetiframe.engine.camera.CameraEasing
import com.donnclab.kinetiframe.engine.camera.CameraShot
import com.donnclab.kinetiframe.engine.camera.ShakeDirection
import kotlin.math.roundToInt

/** Advanced controls live behind one context action, leaving the framing surface uncluttered. */
@Composable
internal fun CameraMotionOptions(shot: CameraShot, fps: Int, onChange: (CameraShot) -> Unit, onDismiss: () -> Unit) {
    val span = shot.endFrame - shot.startFrame
    val wait = shot.waitFrames.coerceAtMost(span - 1)
    val settle = shot.settleFrames.coerceAtMost(span - wait - 1)
    val procedural = shot.action == CameraAction.SHAKE || shot.action == CameraAction.HANDHELD
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Movement timing") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Wait · ${"%.2f".format(wait.toFloat() / fps)} s")
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
                modifier = Modifier.semantics { contentDescription = "Wait duration" },
                valueRange =
                0f..(span - 1).coerceAtLeast(1).toFloat(),
                enabled = span > 1,
            )
            Text("Move · ${"%.2f".format((span - wait - settle).toFloat() / fps)} s")
            Text("Settle · ${"%.2f".format(settle.toFloat() / fps)} s")
            Slider(
                settle.toFloat(),
                {
                    onChange(shot.copy(waitFrames = wait, settleFrames = it.roundToInt()))
                },
                modifier = Modifier.semantics {
                    contentDescription =
                        "Settle duration"
                },
                valueRange = 0f..(span - wait - 1).coerceAtLeast(1).toFloat(),
                enabled =
                span - wait > 1,
            )
            if (procedural) {
                val frequency = shot.frequencyHz ?: if (shot.action == CameraAction.SHAKE) 13f else 1.3f
                Text("Frequency · ${"%.1f".format(frequency)} Hz")
                Slider(frequency, { onChange(shot.copy(frequencyHz = it)) }, valueRange = .2f..30f)
                Text("Rotational movement")
                Slider(shot.rotationStrength, { onChange(shot.copy(rotationStrength = it)) })
                var directions by remember { mutableStateOf(false) }
                TextButton({ directions = true }) { Text("Direction · ${shot.shakeDirection.name.lowercase()}") }
                DropdownMenu(directions, { directions = false }) {
                    ShakeDirection.entries.forEach { direction ->
                        DropdownMenuItem(text = { Text(direction.name.lowercase()) }, onClick = {
                            onChange(shot.copy(shakeDirection = direction))
                            directions =
                                false
                        })
                    }
                }
            } else {
                if (shot.action == CameraAction.FOLLOW) {
                    Text("Follow smoothing · ${"%.2f".format(shot.followSmoothingSeconds)} s")
                    Slider(shot.followSmoothingSeconds, { onChange(shot.copy(followSmoothingSeconds = it)) })
                }
                var easing by remember { mutableStateOf(false) }
                TextButton({ easing = true }) { Text("Curve · ${shot.easing.name.lowercase().replace('_', ' ')}") }
                DropdownMenu(easing, { easing = false }) {
                    CameraEasing.entries.forEach { curve ->
                        DropdownMenuItem(text = { Text(curve.name.lowercase().replace('_', ' ')) }, onClick = {
                            onChange(shot.copy(easing = curve))
                            easing =
                                false
                        })
                    }
                }
            }
            Text("All phases stay inside the selected frame range.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onDismiss) { Text("Done") } })
}

@Preview
@Composable
private fun MotionOptionsPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        CameraMotionOptions(CameraShot("preview", CameraAction.PAN, 0, 24), 12, {}, {})
    }
}
