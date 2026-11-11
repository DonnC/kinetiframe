package com.donnclab.kinetiframe.presentation.studio.camera

import androidx.annotation.StringRes
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.camera.SceneCamera
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.camera.CameraRanges
import com.donnclab.kinetiframe.studio.camera.CameraRecipe
import com.donnclab.kinetiframe.studio.camera.CameraRecipeStore
import com.donnclab.kinetiframe.studio.camera.CameraRecipes
import kotlinx.coroutines.launch

@StringRes
internal fun CameraAction.labelRes(): Int = when (this) {
    CameraAction.PUSH_IN -> R.string.camera_action_push_in
    CameraAction.PULL_OUT -> R.string.camera_action_pull_out
    CameraAction.PAN -> R.string.camera_action_pan
    CameraAction.TILT -> R.string.camera_action_tilt
    CameraAction.SHAKE -> R.string.camera_action_shake
    CameraAction.HANDHELD -> R.string.camera_action_handheld
    CameraAction.FADE_IN -> R.string.camera_action_fade_in
    CameraAction.FADE_OUT -> R.string.camera_action_fade_out
    CameraAction.WHIP_PAN -> R.string.camera_action_whip_pan
    CameraAction.FOLLOW -> R.string.camera_action_follow
}

@Composable
@ReadOnlyComposable
internal fun CameraAction.label(): String = stringResource(labelRes())

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
            IconButton(onClick = { menu = true }) { Icon(Lucide.Camera, stringResource(R.string.camera_effects)) }
            if (state.project.cameraShots.isNotEmpty()) {
                IconButton(onClick = { onPreview(!preview) }) {
                    Icon(
                        if (preview) Lucide.Pen else Lucide.Play,
                        stringResource(if (preview) R.string.camera_edit_artwork else R.string.camera_preview),
                    )
                }
            }
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.camera_presets_menu)) }, onClick = {
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
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.camera_no_free_range)) },
                    onClick = {},
                    enabled = false,
                )
            }
            if (state.project.cameraShots.isNotEmpty()) {
                HorizontalDivider()
                state.project.cameraShots.forEach { shot ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    R.string.camera_shot_range,
                                    shot.action.label(),
                                    shot.startFrame + 1,
                                    shot.endFrame,
                                ),
                            )
                        },
                        onClick = {
                            menu = false
                            onIntent(StudioIntent.Playback.Pause)
                            draft = shot
                        },
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text(stringResource(R.string.camera_undo_edit)) }, onClick = {
                menu = false
                onIntent(StudioIntent.Camera.Undo)
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.camera_redo_edit)) }, onClick = {
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
        AlertDialog(onDismissRequest = {
            library = false
        }, title = { Text(stringResource(R.string.camera_presets_title)) }, text = {
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
                            }) { Icon(Lucide.Trash2, stringResource(R.string.camera_remove_preset, recipe.name)) }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton({ library = false }) { Text(stringResource(R.string.action_close)) } })
    }
    if (presetError) {
        AlertDialog(
            onDismissRequest = {
                presetError = false
            },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.camera_presets_update_failed,
                        CameraRecipeStore.MAX_RECIPES,
                        CameraRecipeStore.MAX_RECIPES,
                    ),
                )
            },
            confirmButton = { TextButton({ presetError = false }) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@Preview
@Composable
private fun CameraWorkspacePreview() {
    KinetiFrameTheme { CameraWorkspaceControls(StudioState(), false, {}, {}, content = { _, _, _ -> }) }
}
