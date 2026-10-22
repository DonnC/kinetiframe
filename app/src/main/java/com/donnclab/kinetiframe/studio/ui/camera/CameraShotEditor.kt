package com.donnclab.kinetiframe.studio.ui.camera

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.BookmarkPlus
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Text
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.camera.CameraAction
import com.donnclab.kinetiframe.studio.camera.CameraFraming
import com.donnclab.kinetiframe.studio.camera.CameraPose
import com.donnclab.kinetiframe.studio.camera.CameraRanges
import com.donnclab.kinetiframe.studio.camera.CameraShot
import com.donnclab.kinetiframe.studio.camera.SceneCamera
import kotlinx.coroutines.delay

@Composable
internal fun CameraShotEditor(
    initial: CameraShot,
    state: StudioState,
    onDismiss: () -> Unit,
    onSave: (CameraShot) -> Unit,
    onRemove: () -> Unit,
    onSaveRecipe: (String, CameraShot) -> Unit = { _, _ -> },
    content: @Composable (CameraPose, CameraShot?, Int?) -> Unit,
) {
    var shot by remember(initial.id) { mutableStateOf(initial) }
    var ending by remember { mutableStateOf(true) }
    var motionOptions by remember { mutableStateOf(false) }
    var saveRecipe by remember { mutableStateOf(false) }
    var recipeName by remember { mutableStateOf("") }
    val conflicts =
        remember(shot, state.project.cameraShots) { CameraRanges.conflicts(shot, state.project.cameraShots) }
    val canSave = conflicts.isEmpty() && state.project.cameraShots.count { it.id != shot.id } < 256 &&
        (shot.action != CameraAction.FOLLOW || shot.targetPlacementId in state.project.actorPlacements)
    var playing by remember { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(initial.startFrame) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(playing, shot) {
        if (playing) {
            frame = shot.startFrame
            while (true) {
                delay(1000L / state.project.fps)
                frame = if (frame + 1 >= shot.endFrame) shot.startFrame else frame + 1
            }
        }
    }
    val procedural = shot.action == CameraAction.SHAKE || shot.action == CameraAction.HANDHELD
    val fade = shot.action == CameraAction.FADE_IN || shot.action == CameraAction.FADE_OUT
    val follow = shot.action == CameraAction.FOLLOW
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.94f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(12.dp)) {
                Row {
                    Text(shot.action.label(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (!procedural && !fade) {
                        TextButton({
                            ending = false
                            playing = false
                        }) { Text(if (!ending) "• Start" else "Start") }
                        TextButton({
                            ending = true
                            playing = false
                        }) { Text(if (ending) "• End" else "End") }
                    }
                    IconButton({
                        playing = !playing
                    }) { Icon(if (playing) Lucide.Pause else Lucide.Play, "Preview camera movement") }
                    IconButton({ motionOptions = true }) { Icon(Lucide.Settings2, "Movement timing and customization") }
                    IconButton({ saveRecipe = true }) { Icon(Lucide.BookmarkPlus, "Save camera preset") }
                    IconButton(onRemove) { Icon(Lucide.Trash2, "Remove camera effect") }
                    IconButton(onDismiss) { Icon(Lucide.X, "Cancel") }
                    IconButton({ onSave(shot) }, enabled = canSave) { Icon(Lucide.Check, "Save camera effect") }
                }
                Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { previewSize = it }) {
                    content(
                        if (playing) {
                            SceneCamera.evaluate(
                                state.project.cameraShots.filter {
                                    it.id != shot.id && it.endFrame <= shot.startFrame
                                } +
                                    shot,
                                frame,
                                state.project.fps,
                            )
                        } else if (ending) {
                            shot.end
                        } else {
                            shot.start
                        },
                        if (playing) shot else null,
                        if (playing) frame else null,
                    )
                    if (!procedural && !fade &&
                        !playing
                    ) {
                        Box(
                            Modifier.fillMaxSize().pointerInput(initial.id, ending, previewSize) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    val scale = minOf(
                                        previewSize.width.toFloat() / state.project.stage.width,
                                        previewSize.height.toFloat() / state.project.stage.height,
                                    ) * .92f
                                    if (scale <= 0) return@detectTransformGestures
                                    val next = CameraFraming.drag(
                                        if (ending) shot.end else shot.start,
                                        pan.x / scale,
                                        pan.y / scale,
                                        zoom,
                                        rotation,
                                        state.project.stage.width,
                                        state.project.stage.height,
                                    )
                                    shot = if (ending) shot.copy(end = next) else shot.copy(start = next)
                                }
                            },
                        )
                    }
                }
                Text(
                    if (procedural) {
                        "Adjust movement strength"
                    } else if (fade) {
                        "Fade to or from black"
                    } else {
                        "Drag to frame · pinch to zoom · twist to tilt. Choose Start and End."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (procedural) Slider(shot.strength, { shot = shot.copy(strength = it) })
                if (follow) {
                    var targets by remember { mutableStateOf(false) }
                    TextButton({
                        targets = true
                    }) {
                        Text(
                            "Follow · ${state.project.actorPlacements[shot.targetPlacementId]?.label ?: "Choose actor"}",
                        )
                    }
                    DropdownMenu(targets, { targets = false }) {
                        state.project.actorPlacements.values.forEach { actor ->
                            DropdownMenuItem(text = { Text(actor.label) }, onClick = {
                                shot =
                                    shot.copy(targetPlacementId = actor.placementId)
                                targets = false
                            })
                        }
                    }
                }
                if (!procedural && !follow) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            "Keep final state after this range",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Switch(shot.holdEnd, { shot = shot.copy(holdEnd = it) })
                    }
                }
                Text(
                    "Frames ${shot.startFrame + 1}–${shot.endFrame} · ${state.animation.totalFrames} total",
                    style = MaterialTheme.typography.labelMedium,
                )
                CameraRangeOverview(shot, state.project.cameraShots, state.animation.totalFrames)
                if (state.animation.totalFrames > 1) {
                    RangeSlider(
                        value = (shot.startFrame + 1).toFloat()..shot.endFrame.toFloat(),
                        onValueChange = { range ->
                            val from = range.start.toInt().coerceIn(1, state.animation.totalFrames)
                            val end = range.endInclusive.toInt().coerceIn(from, state.animation.totalFrames)
                            shot = shot.copy(startFrame = from - 1, endFrame = end)
                        },
                        valueRange = 1f..state.animation.totalFrames.toFloat(),
                    )
                }
            }
        }
    }
    if (motionOptions) CameraMotionOptions(shot, state.project.fps, { shot = it }, { motionOptions = false })
    if (saveRecipe) {
        AlertDialog(
            onDismissRequest = { saveRecipe = false },
            title = { Text("Save camera preset") },
            text = {
                OutlinedTextField(recipeName, { recipeName = it.take(80) }, label = { Text("Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton({
                    onSaveRecipe(recipeName.trim(), shot)
                    saveRecipe = false
                }, enabled = recipeName.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton({ saveRecipe = false }) { Text("Cancel") } },
        )
    }
}

@Preview(widthDp = 800, heightDp = 420)
@Composable
private fun CameraShotEditorPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        CameraShotEditor(
            SceneCamera.preset("preview", CameraAction.PUSH_IN, 0, 24, CameraPose()),
            StudioState(animation = com.donnclab.kinetiframe.studio.AnimationState(totalFrames = 48)),
            {
            },
            {},
            {},
        ) { _, _, _ ->
            Box(Modifier.fillMaxSize())
        }
    }
}
