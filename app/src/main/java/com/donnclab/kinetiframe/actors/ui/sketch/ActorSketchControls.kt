package com.donnclab.kinetiframe.actors.ui.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Redo
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Undo
import com.composables.icons.lucide.Undo2

@Composable
internal fun ActorSketchControls(
    state: ActorSketchState,
    onIntent: (ActorSketchIntent) -> Unit,
    showBones: Boolean,
    onBones: () -> Unit,
    onClose: () -> Unit,
    graph: com.donnclab.kinetiframe.actors.rig.model.SceneGraph? = null,
) {
    var palette by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        IconButton(onClose) { Icon(Lucide.ArrowLeft, "Close sketch") }
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            if (state.tool != SketchTool.JOINT && state.tool != SketchTool.BIND_REGION) {
                Box {
                    IconButton({ palette = true }, enabled = !state.busy) {
                        Icon(
                            Lucide.Palette,
                            if (state.tool ==
                                SketchTool.ERASER
                            ) {
                                "Eraser size"
                            } else {
                                "Choose colour"
                            },
                            tint = Color(state.paintColour),
                        )
                    }
                    DropdownMenu(palette, { palette = false }) {
                        if (state.tool !=
                            SketchTool.ERASER
                        ) {
                            SketchPalette.forEach { colour ->
                                Box(
                                    Modifier.padding(
                                        6.dp,
                                    ).size(32.dp).background(Color(colour), CircleShape).clickable {
                                        onIntent(ActorSketchIntent.Colour(colour.toInt()))
                                        palette = false
                                    },
                                )
                            }
                        }
                        if (state.tool !=
                            SketchTool.FILL
                        ) {
                            Slider(state.size, {
                                onIntent(ActorSketchIntent.Size(it))
                            }, valueRange = 1f..48f, modifier = Modifier.width(160.dp))
                        }
                    }
                }
            }
            if (state.tool !=
                SketchTool.JOINT
            ) {
                IconButton(onBones) { Icon(Lucide.Eye, if (showBones) "Hide sketch joints" else "Show sketch joints") }
            }
            graph?.let { ActorSketchBindingMenu(state, it, onIntent) }
        }
        IconButton({
            onIntent(ActorSketchIntent.Undo)
        }, enabled = state.canUndo && !state.busy) { Icon(Lucide.Undo2, "Undo sketch") }
        IconButton({
            onIntent(ActorSketchIntent.Redo)
        }, enabled = state.canRedo && !state.busy) { Icon(Lucide.Redo2, "Redo sketch") }
        IconButton({
            onIntent(ActorSketchIntent.Bind)
        }, enabled = !state.busy) { Icon(Lucide.Check, "Make drawing movable") }
    }
}

@Preview(widthDp = 900)
@Composable
private fun SketchControlsPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        ActorSketchControls(ActorSketchState(busy = false), {}, true, {}, {})
    }
}

/** Quick colours offered in the sketch palette menu, as ARGB values. */
private val SketchPalette = listOf(
    0xff202020,
    0xffffffff,
    0xffe86c5d,
    0xffffb86b,
    0xffffdd80,
    0xff32b6ac,
    0xff4e87d8,
    0xff9865bd,
    0xff9c6546,
)
