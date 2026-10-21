package com.donnclab.kinetiframe.ui.components.rigging

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph

/**
 * Rig Fabricator's default, always-on part picker: a slim horizontal chip strip along the bottom
 * of the canvas for the everyday "which part am I sketching or rigging" choice. The full hierarchy -- with
 * visibility toggles and the raster layer list -- is still one tap away via [PartPickerSheet];
 * this rail is just the fast path most taps never need to leave.
 */
@Composable
fun PartRail(
    graph: SceneGraph,
    selectedPartId: String?,
    onPartSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parts = remember(graph) { graph.orderedBones() }
    Surface(color = Color.White, shadowElevation = 6.dp, modifier = modifier) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            parts.forEach { part -> PartChip(part, part.id == selectedPartId, onClick = { onPartSelected(part.id) }) }
        }
    }
}

@Composable
private fun PartChip(part: BoneNode, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) ActorStudioTokens.accent.copy(alpha = 0.14f) else Color(0xFFF1EBF3),
        border = if (selected) BorderStroke(1.5.dp, ActorStudioTokens.accent) else null,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Filled = artwork already bound to this part; hollow warning color = still needs a
            // sketch -- the same signal ActorPartTree's Link2/Link2Off icon and RigSkeletonOverlay's
            // unbound-joint color give elsewhere, just small enough for a chip.
            Box(
                Modifier.size(6.dp).background(
                    if (part.textureBinding != null) ActorStudioTokens.accent else MaterialTheme.colorScheme.error,
                    CircleShape,
                ),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                part.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) ActorStudioTokens.accent else EditorTokens.dark,
            )
        }
    }
}

@Preview
@Composable
private fun PartRailPreview() {
    PartRail(
        graph = SceneGraph(
            rootBoneIds = listOf("torso"),
            bones = mapOf(
                "torso" to BoneNode("torso", name = "Torso", length = 30f),
                "head" to BoneNode("head", parentId = "torso", name = "Head", length = 12f),
            ),
        ),
        selectedPartId = "torso",
        onPartSelected = {},
    )
}
