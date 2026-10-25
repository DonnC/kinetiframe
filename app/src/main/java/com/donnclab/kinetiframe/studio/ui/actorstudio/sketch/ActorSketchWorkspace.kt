package com.donnclab.kinetiframe.studio.ui.actorstudio.sketch

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Text
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchIntent
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchState
import com.donnclab.kinetiframe.presentation.actors.sketch.SketchLayer
import com.donnclab.kinetiframe.presentation.actors.sketch.SketchTool
import com.donnclab.kinetiframe.studio.ui.dock.FloatingDock
import com.donnclab.kinetiframe.studio.ui.dock.ToolButton

/** Same movable tool surface as Studio; only tools supported by this source draft appear. */
@Composable
internal fun ActorSketchToolDock(state: ActorSketchState, onIntent: (ActorSketchIntent) -> Unit) {
    var offset by remember { mutableStateOf(Offset(16f, 72f)) }
    FloatingDock(initialOffset = offset, onOffsetChange = { offset = it }) {
        Column(
            Modifier.width(48.dp).heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            listOf(
                Triple(SketchTool.JOINT, Lucide.Bone, "Fit drawing joints"),
                Triple(SketchTool.PEN, Lucide.Pen, "Draw"),
                Triple(SketchTool.ERASER, Lucide.Eraser, "Erase selected drawing layer"),
                Triple(SketchTool.FILL, Lucide.PaintBucket, "Fill colour beneath linework"),
            ).forEach { (tool, icon, label) ->
                ToolButton(
                    icon,
                    label,
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
        Modifier.fillMaxWidth().padding(8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Lucide.Layers, null)
            SketchLayer.entries.forEach { layer ->
                FilterChip(
                    state.layer == layer,
                    { onIntent(ActorSketchIntent.Layer(layer)) },
                    label = {
                        Text(
                            if (layer ==
                                SketchLayer.LINEWORK
                            ) {
                                "Linework"
                            } else {
                                "Colour"
                            },
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
