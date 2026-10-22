package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.common.strings.FeatureStrings
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.studio.ActorEditTarget
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition

@Composable
fun ActorWorkflowHint(state: StudioState, modifier: Modifier = Modifier) {
    // Each branch names the single next action for the studio's current state and tool, so the
    // hint always tells the animator what to do next.
    val definition = state.actorDefinition()
    val selectedPart = definition?.sceneGraph?.bones?.get(state.scene.selectedBoneId)?.name
    val rigSetup = state.scene.studioMode == StudioMode.RIG_SETUP
    val text = when {
        definition == null -> FeatureStrings.ActorStudio.NEEDS_SKELETON

        state.scene.studioMode == StudioMode.ANIMATE_IK -> "Record · move a part · stop and replay"

        rigSetup && state.actor.editTarget == ActorEditTarget.ARTWORK -> "Drag the artwork · joints stay fixed"

        rigSetup && state.actor.editTarget == ActorEditTarget.JOINT_START -> "Drag the highlighted attachment joint"

        definition.sceneGraph.drawing?.enabled == true ->
            "Fit joints to your sketch · drawing menu to edit lines and colour"

        state.tool.activeTool == DrawingTool.LASSO_CUT -> FeatureStrings.ActorStudio.TRACE_ARTWORK

        state.scene.selectedBoneId == null -> FeatureStrings.ActorStudio.PICK_A_PART

        rigSetup -> FeatureStrings.ActorStudio.FIT_JOINTS

        else -> "Drawing ${selectedPart ?: "selected part"} · strokes attach automatically"
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun ActorWorkflowHintPreview() {
    ActorWorkflowHint(StudioState())
}
