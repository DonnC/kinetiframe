package com.donnclab.kinetiframe.presentation.studio.actorstudio.sketch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchCanvas
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchControls
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchEffect
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchIntent
import com.donnclab.kinetiframe.presentation.actors.sketch.SketchTool
import com.donnclab.kinetiframe.studio.StudioIntent

/** A draft closes only after the actor document accepts its source IDs; import/budget failures
 * leave the original editable draft open. Discard never modifies the saved actor. */
@Composable
fun ActorSketchEditor(
    definition: PuppetDefinition,
    stageWidth: Int,
    stageHeight: Int,
    assets: RigAssetManager,
    acceptedInkId: String?,
    onIntent: (StudioIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val drawing = remember { definition.sceneGraph.drawing }
    val factor = minOf(1f, 1024f / maxOf(stageWidth, stageHeight))
    val width = drawing?.width ?: (stageWidth * factor).toInt().coerceAtLeast(1)
    val height = drawing?.height ?: (stageHeight * factor).toInt().coerceAtLeast(1)
    val originalInk = drawing?.let { assets.getAsset(it.inkAssetId) }
    val originalColour = drawing?.let { assets.getAsset(it.colourAssetId) }
    if (drawing != null &&
        (
            originalInk == null || originalColour == null || originalInk.isRecycled || originalColour.isRecycled ||
                originalInk.width != width || originalInk.height != height || originalColour.width != width ||
                originalColour.height != height
            )
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Text(
                    "The original drawing layers could not be restored. Reopen the actor before editing; its saved artwork has not been replaced.",
                )
            },
            confirmButton = { TextButton(onDismiss) { Text("Close") } },
        )
        return
    }
    if (width.toLong() * height > 1_048_576) {
        AlertDialog(onDismissRequest = onDismiss, text = {
            Text(
                "This source exceeds the mobile sketch editor's pixel budget. It remains available for rendering and cutout editing.",
            )
        }, confirmButton = { TextButton(onDismiss) { Text("Close") } })
        return
    }
    val bounds = remember {
        floatArrayOf(
            drawing?.originX ?: 0f,
            drawing?.originY ?: 0f,
            drawing?.actorWidth ?: stageWidth.toFloat(),
            drawing?.actorHeight ?: stageHeight.toFloat(),
        )
    }
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val model: ActorSketchViewModel = viewModel(
        viewModelStoreOwner = owner,
        factory = remember {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ActorSketchViewModel(
                    definition.sceneGraph,
                    width,
                    height,
                    bounds,
                    originalInk,
                    originalColour,
                ) as T
            }
        },
    )
    val state by model.state.collectAsState()
    var showBones by remember { mutableStateOf(true) }
    var exit by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingInk by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(model) {
        model.effect.collect { effect ->
            when (effect) {
                is ActorSketchEffect.Error -> error = effect.message

                is ActorSketchEffect.Bound -> {
                    pendingInk = effect.drawing.inkAssetId
                    onIntent(
                        StudioIntent.Actor.CommitDrawing(
                            definition.id,
                            effect.ink,
                            effect.colour,
                            effect.drawing,
                            effect.restGraph,
                        ),
                    )
                }
            }
        }
    }
    LaunchedEffect(acceptedInkId, pendingInk) { if (pendingInk != null && acceptedInkId == pendingInk) onDismiss() }
    fun close() {
        if (state.dirty) exit = true else onDismiss()
    }
    BackHandler(onBack = ::close)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            ActorSketchControls(state, model::handleIntent, showBones, {
                showBones = !showBones
            }, ::close, definition.sceneGraph)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Box(Modifier.weight(1f)) {
                ActorSketchCanvas(
                    state,
                    state.graph ?: definition.sceneGraph,
                    bounds,
                    showBones || state.tool == SketchTool.JOINT,
                    model::handleIntent,
                )
                ActorSketchToolDock(state, model::handleIntent)
                Surface(
                    Modifier.align(Alignment.TopCenter).padding(start = 76.dp, top = 8.dp, end = 8.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        when (state.tool) {
                            SketchTool.JOINT -> "Drag joints to fit your drawing · two fingers to pan/zoom"

                            SketchTool.BIND_REGION -> {
                                val part = definition.sceneGraph.bones[state.bindingPartId]?.name ?: "selected part"
                                "Lasso artwork for $part"
                            }

                            else -> "Draw your character · colour beneath the lines"
                        },
                        Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            ActorSketchLayerRail(state, model::handleIntent)
        }
    }
    if (exit) {
        AlertDialog(
            onDismissRequest = { exit = false },
            title = { Text("Keep your sketch?") },
            text = { Text("Make it movable to apply it to the actor, or discard this draft.") },
            confirmButton = {
                TextButton({
                    exit = false
                    model.handleIntent(ActorSketchIntent.Bind)
                }, enabled = !state.busy) { Text("Apply") }
            },
            dismissButton = {
                Row {
                    TextButton({ exit = false }) { Text("Cancel") }
                    TextButton(onDismiss) { Text("Discard") }
                }
            },
        )
    }
    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null }, text = { Text(message) }, confirmButton = {
            TextButton({
                error =
                    null
            }) { Text("OK") }
        })
    }
}
