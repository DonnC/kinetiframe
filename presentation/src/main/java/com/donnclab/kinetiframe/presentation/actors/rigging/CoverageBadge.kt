package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens

/** How many rig parts already have artwork bound, so the user can tell at a glance when an
 *  actor is fully covered instead of discovering a bare bone only at pose time. */
@Composable
fun CoverageBadge(definition: PuppetDefinition) {
    val bones = definition.sceneGraph.bones.values
    val bound = bones.count { it.textureBinding != null }
    val total = bones.size
    if (total == 0) return
    Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "$bound of $total parts have artwork",
                style = MaterialTheme.typography.labelSmall,
                color = if (bound == total) ActorStudioTokens.accent else ActorStudioTokens.onPanelMuted,
            )
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else bound.toFloat() / total },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp),
            color = ActorStudioTokens.accent,
            trackColor = ActorStudioTokens.panelSurfaceVariant,
        )
    }
}
