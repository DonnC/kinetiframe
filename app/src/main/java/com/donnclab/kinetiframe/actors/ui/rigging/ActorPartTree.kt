package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Link2
import com.composables.icons.lucide.Link2Off
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.core.actor.rigging.BoneDisplay
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens

/**
 * A stable-part hierarchy for Actor Studio, styled as a layer browser: expand/collapse groups,
 * per-part bound-artwork status, and a per-part show/hide toggle -- mirroring FxF's own layer
 * list ([com.donnclab.kinetiframe.studio.ui.timeline.LayerItem]) so both editors read the same way.
 * Expansion is presentation-only: it never changes structural IDs, which keeps a selected part
 * valid for performances and future revisions.
 */
@Composable
fun ActorPartTree(
    graph: SceneGraph,
    selectedPartId: String?,
    onPartSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onVisibilityToggle: (partId: String, visible: Boolean) -> Unit = { _, _ -> },
) {
    Column(modifier) {
        graph.rootBoneIds.mapNotNull(graph.bones::get).forEach { root ->
            ActorPartTreeNode(root, graph, selectedPartId, onPartSelected, onVisibilityToggle)
        }
    }
}

@Composable
private fun ActorPartTreeNode(
    part: BoneNode,
    graph: SceneGraph,
    selectedPartId: String?,
    onPartSelected: (String) -> Unit,
    onVisibilityToggle: (String, Boolean) -> Unit,
    depth: Int = 0,
) {
    val children = remember(graph, part.id) { graph.bones.values.filter { it.parentId == part.id } }
    var expanded by remember(part.id) { mutableStateOf(true) }
    val selected = part.id == selectedPartId
    val isGroup = children.isNotEmpty()
    Surface(
        color = if (selected) ActorStudioTokens.panelSurfaceVariant else ActorStudioTokens.panelSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp, top = 1.dp, bottom = 1.dp)
            .clickable { onPartSelected(part.id) },
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isGroup) {
                Icon(
                    if (expanded) Lucide.ChevronDown else Lucide.ChevronRight,
                    contentDescription = if (expanded) "Collapse ${part.name}" else "Expand ${part.name}",
                    tint = ActorStudioTokens.onPanelMuted,
                    modifier = Modifier.size(16.dp).clickable { expanded = !expanded },
                )
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Icon(
                if (isGroup) {
                    if (expanded) Lucide.FolderOpen else Lucide.Folder
                } else {
                    Lucide.Bone
                },
                contentDescription = null,
                tint = if (selected) ActorStudioTokens.accent else ActorStudioTokens.onPanelMuted,
                modifier = Modifier.size(15.dp),
            )
            Text(
                part.name,
                Modifier.weight(1f).padding(start = 6.dp),
                color = if (selected) ActorStudioTokens.onPanel else ActorStudioTokens.onPanelMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                if (part.textureBinding == null) Lucide.Link2Off else Lucide.Link2,
                contentDescription = if (part.textureBinding == null) "Artwork not bound" else "Artwork bound",
                modifier = Modifier.size(14.dp).padding(end = 6.dp),
                tint = if (part.textureBinding == null) ActorStudioTokens.onPanelMuted else ActorStudioTokens.accent,
            )
            Icon(
                if (part.visible) Lucide.Eye else Lucide.EyeOff,
                contentDescription = if (part.visible) "Hide ${part.name}" else "Show ${part.name}",
                tint = if (part.visible) ActorStudioTokens.onPanelMuted else ActorStudioTokens.accent,
                modifier = Modifier.size(15.dp).clickable { onVisibilityToggle(part.id, !part.visible) },
            )
        }
    }
    if (expanded) {
        children.forEach { child ->
            ActorPartTreeNode(child, graph, selectedPartId, onPartSelected, onVisibilityToggle, depth + 1)
        }
    }
}

@Preview
@Composable
private fun ActorPartTreePreview() {
    ActorPartTree(
        graph = SceneGraph(
            rootBoneIds = listOf("hips"),
            bones = mapOf(
                "hips" to BoneNode("hips", name = "Hips", length = 30f),
                "arm" to BoneNode(
                    hierarchy = BoneHierarchy("arm", "Arm", "hips"),
                    rest = BoneRest(22f),
                    display = BoneDisplay(visible = false),
                ),
            ),
        ),
        selectedPartId = "arm",
        onPartSelected = {},
    )
}
