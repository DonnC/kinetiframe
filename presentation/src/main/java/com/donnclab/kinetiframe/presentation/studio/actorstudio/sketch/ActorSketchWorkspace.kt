package com.donnclab.kinetiframe.presentation.studio.actorstudio.sketch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Text
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchIntent
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchState
import com.donnclab.kinetiframe.presentation.actors.sketch.SketchLayer
import com.donnclab.kinetiframe.presentation.actors.sketch.SketchTool
import com.donnclab.kinetiframe.presentation.studio.dock.FloatingDock
import com.donnclab.kinetiframe.presentation.studio.dock.ToolButton

/** Same movable tool surface as Studio; only tools supported by this source draft appear. */
@Composable
internal fun ActorSketchToolDock(state: ActorSketchState, onIntent: (ActorSketchIntent) -> Unit) {
    var offset by remember { mutableStateOf(Offset(16f, 72f)) }
    FloatingDock(initialOffset = offset, onOffsetChange = { offset = it }) {
        Column(
            Modifier.width(
                Dimens.touchTarget,
            ).heightIn(max = ActorStudioTokens.sketchToolRailMaxHeight).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            listOf(
                Triple(SketchTool.JOINT, Lucide.Bone, R.string.sketch_tool_joints),
                Triple(SketchTool.PEN, Lucide.Pen, R.string.sketch_tool_draw),
                Triple(SketchTool.ERASER, Lucide.Eraser, R.string.sketch_tool_erase),
                Triple(SketchTool.FILL, Lucide.PaintBucket, R.string.sketch_tool_fill),
            ).forEach { (tool, icon, label) ->
                ToolButton(
                    icon,
                    stringResource(label),
                    state.tool == tool,
                    onClick = { if (!state.busy) onIntent(ActorSketchIntent.Tool(tool)) },
                    onOpenMenu = {},
                )
            }
        }
    }
}

/** Source layers occupy the familiar bottom rail, not a second animation timeline. */
@Composable
internal fun ActorSketchLayerRail(state: ActorSketchState, onIntent: (ActorSketchIntent) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(Dimens.spaceS),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = Dimens.elevationFlat,
    ) {
        Row(
            Modifier.padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            Icon(Lucide.Layers, null)
            SketchLayer.entries.forEach { layer ->
                FilterChip(
                    state.layer == layer,
                    { onIntent(ActorSketchIntent.Layer(layer)) },
                    label = {
                        Text(
                            stringResource(
                                if (layer ==
                                    SketchLayer.LINEWORK
                                ) {
                                    R.string.sketch_layer_linework
                                } else {
                                    R.string.sketch_layer_colour
                                },
                            ),
                        )
                    },
                    enabled = !state.busy,
                )
            }
        }
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Composable
private fun SketchWorkspacePreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { ActorSketchToolDock(ActorSketchState(busy = false), {}) }
            ActorSketchLayerRail(ActorSketchState(busy = false), {})
        }
    }
}
