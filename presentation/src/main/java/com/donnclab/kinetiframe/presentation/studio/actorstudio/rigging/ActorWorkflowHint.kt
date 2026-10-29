package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.presentation.R
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
        definition == null -> stringResource(R.string.actor_hint_needs_skeleton)

        state.scene.studioMode == StudioMode.ANIMATE_IK -> stringResource(R.string.actor_hint_record)

        rigSetup && state.actor.editTarget == ActorEditTarget.ARTWORK -> stringResource(
            R.string.actor_hint_drag_artwork,
        )

        rigSetup && state.actor.editTarget == ActorEditTarget.JOINT_START -> stringResource(
            R.string.actor_hint_drag_attachment,
        )

        definition.sceneGraph.drawing?.enabled == true ->
            stringResource(R.string.actor_hint_fit_sketch)

        state.tool.activeTool == DrawingTool.LASSO_CUT -> stringResource(R.string.actor_hint_trace_artwork)

        state.scene.selectedBoneId == null -> stringResource(R.string.actor_hint_pick_part)

        rigSetup -> stringResource(R.string.actor_hint_fit_joints)

        else -> stringResource(
            R.string.actor_hint_drawing_part,
            selectedPart ?: stringResource(R.string.actor_hint_selected_part),
        )
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
