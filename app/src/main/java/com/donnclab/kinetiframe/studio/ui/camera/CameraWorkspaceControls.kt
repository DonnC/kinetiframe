package com.donnclab.kinetiframe.studio.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Redo
import com.composables.icons.lucide.Text
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Undo
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.camera.CameraAction
import com.donnclab.kinetiframe.studio.camera.CameraPose
import com.donnclab.kinetiframe.studio.camera.CameraRanges
import com.donnclab.kinetiframe.studio.camera.CameraRecipe
import com.donnclab.kinetiframe.studio.camera.CameraRecipes
import com.donnclab.kinetiframe.studio.camera.CameraShot
import com.donnclab.kinetiframe.studio.camera.SceneCamera
import kotlinx.coroutines.launch

internal fun CameraAction.label() = when (this) {
    CameraAction.PUSH_IN -> "Push in"
    CameraAction.PULL_OUT -> "Pull out"
    CameraAction.PAN -> "Pan"
    CameraAction.TILT -> "Tilt"
    CameraAction.SHAKE -> "Impact shake"
    CameraAction.HANDHELD -> "Gentle handheld"
    CameraAction.FADE_IN -> "Fade in"
    CameraAction.FADE_OUT -> "Fade out"
    CameraAction.WHIP_PAN -> "Whip pan"
    CameraAction.FOLLOW -> "Follow actor"
}

/** Camera edits have their own bounded undo history, separate from drawing strokes. */
@Composable
fun CameraWorkspaceControls(
    state: StudioState,
    preview: Boolean,
    onPreview: (Boolean) -> Unit,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (CameraPose, CameraShot?, Int?) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var draft by remember(state.project.id) { mutableStateOf<CameraShot?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { com.donnclab.kinetiframe.studio.camera.CameraRecipeStore(context) }
    val scope = rememberCoroutineScope()
    var recipes by remember { mutableStateOf<List<CameraRecipe>>(emptyList()) }
    var library by remember { mutableStateOf(false) }
    var presetError by remember { mutableStateOf(false) }
    LaunchedEffect(store) { recipes = store.load() }
    Box(modifier) {
        Row {
            IconButton(onClick = { menu = true }) { Icon(Lucide.Camera, "Camera effects") }
            if (state.project.cameraShots.isNotEmpty()) {
                IconButton(onClick = { onPreview(!preview) }) {
                    Icon(if (preview) Lucide.Pen else Lucide.Play, if (preview) "Edit artwork" else "Preview camera")
                }
            }
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text("Cinematic and saved presets") }, onClick = {
                menu = false
                library = true
            })
            val available = CameraRanges.available(
                state.project.cameraShots,
                state.animation.totalFrames,
                state.animation.currentFrame,
                state.project.fps * 2,
            )
            CameraAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label()) },
                    onClick = {
                        menu = false
                        onIntent(StudioIntent.Playback.Pause)
                        val range = available ?: return@DropdownMenuItem
                        val from = range.first
                        val to = range.last + 1
                        draft = SceneCamera.preset(
                            java.util.UUID.randomUUID().toString(),
                            action,
                            from,
                            to,
                            SceneCamera.stablePose(state.project.cameraShots, from, state.project.fps),
                        ).let {
                            if (action ==
                                CameraAction.FOLLOW
                            ) {
                                it.copy(
                                    targetPlacementId = state.project.actorPlacements.keys.firstOrNull(),
                                    end = CameraPose(zoom = it.end.zoom),
                                )
                            } else {
                                it
                            }
                        }
                    },
                    enabled =
                    available != null && (action != CameraAction.FOLLOW || state.project.actorPlacements.isNotEmpty()),
                )
            }
            if (available ==
                null
            ) {
                DropdownMenuItem(text = { Text("No free camera range") }, onClick = {}, enabled = false)
            }
            if (state.project.cameraShots.isNotEmpty()) {
                HorizontalDivider()
                state.project.cameraShots.forEach { shot ->
                    DropdownMenuItem(
                        text = { Text("${shot.action.label()} · ${shot.startFrame + 1}–${shot.endFrame}") },
                        onClick = {
                            menu = false
                            onIntent(StudioIntent.Playback.Pause)
                            draft = shot
                        },
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Undo effect edit") }, onClick = {
                menu = false
                onIntent(StudioIntent.Camera.Undo)
            })
            DropdownMenuItem(text = { Text("Redo effect edit") }, onClick = {
                menu = false
                onIntent(StudioIntent.Camera.Redo)
            })
        }
    }
    draft?.let { shot ->
        CameraShotEditor(shot, state, { draft = null }, { next ->
            onIntent(StudioIntent.Camera.Put(next))
            onPreview(true)
            draft = null
        }, {
            onIntent(StudioIntent.Camera.Remove(shot.id))
            draft = null
        }, onSaveRecipe = { name, saved ->
            scope.launch {
                val reference = SceneCamera.stablePose(
                    state.project.cameraShots.filterNot {
                        it.id == saved.id
                    },
                    saved.startFrame,
                    state.project.fps,
                )
                if (store.save(CameraRecipe(name, saved, state.project.fps, reference))) {
                    recipes = store.load()
                } else {
                    presetError =
                        true
                }
            }
        }, content = content)
    }
    if (library) {
        AlertDialog(onDismissRequest = { library = false }, title = { Text("Camera presets") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                (CameraRecipes.builtIn(state.project.fps) + recipes).forEach { recipe ->
                    val range = CameraRanges.available(
                        state.project.cameraShots,
                        state.animation.totalFrames,
                        state.animation.currentFrame,
                        recipe.durationFrames(state.project.fps),
                    )
                    Row {
                        TextButton(
                            {
                                val selected = range ?: return@TextButton
                                onIntent(StudioIntent.Playback.Pause)
                                draft =
                                    recipe.retime(state.project.fps).place(
                                        java.util.UUID.randomUUID().toString(),
                                        selected.first,
                                        selected.last + 1,
                                        SceneCamera.stablePose(
                                            state.project.cameraShots,
                                            selected.first,
                                            state.project.fps,
                                        ),
                                    ).let {
                                        if (it.action ==
                                            CameraAction.FOLLOW
                                        ) {
                                            it.copy(
                                                targetPlacementId = state.project.actorPlacements.keys.firstOrNull(),
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                library = false
                            },
                            modifier = Modifier.weight(1f),
                            enabled = range != null &&
                                (
                                    recipe.shot.action != CameraAction.FOLLOW ||
                                        state.project.actorPlacements.isNotEmpty()
                                    ),
                        ) {
                            Text(recipe.name)
                        }
                        if (recipe in recipes) {
                            IconButton({
                                scope.launch {
                                    if (store.remove(recipe.name)) recipes = store.load() else presetError = true
                                }
                            }) { Icon(Lucide.Trash2, "Remove saved preset ${recipe.name}") }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton({ library = false }) { Text("Close") } })
    }
    if (presetError) {
        AlertDialog(
            onDismissRequest = {
                presetError = false
            },
            text = {
                Text(
                    "Could not update presets. If you have 64 saved presets, remove one first; otherwise please try again.",
                )
            },
            confirmButton = { TextButton({ presetError = false }) { Text("OK") } },
        )
    }
}

@Preview
@Composable
private fun CameraWorkspacePreview() {
    KinetiFrameTheme { CameraWorkspaceControls(StudioState(), false, {}, {}, content = { _, _, _ -> }) }
}
