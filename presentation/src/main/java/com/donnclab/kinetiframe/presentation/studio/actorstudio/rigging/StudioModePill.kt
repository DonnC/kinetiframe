package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.studio.StudioMode

/**
 * The top-header tab switch between Actor Studio's two phases -- Rig Fabricator (sketching and
 * pivot setup, [StudioMode.RASTER_DRAWING] and [StudioMode.RIG_SETUP]) and Performance Stage
 * ([StudioMode.ANIMATE_IK]) -- shared by [EditorTopBar][com.donnclab.kinetiframe.presentation.studio.EditorTopBar]
 * (shown while sketching) and [PuppetTopBar] (shown while rigging or posing) so the same control
 * appears in the same place regardless of which sub-tool put you there. Selecting "Rig Fabricator"
 * while already in it is a no-op -- the pill only ever needs to move the *phase*, not the
 * finer-grained tool within it (that stays [com.donnclab.kinetiframe.presentation.studio.dock.ToolDock]'s job).
 *
 * Icon-only, matching the rest of Actor Studio's controls: Bone is already this
 * app's established rigging symbol (the skeleton-overlay toggle and ToolDock's Rig button both use
 * it), and Drama reads clearly as "performance" without competing with the literal Play button on
 * the preview timeline below it.
 */
@Composable
fun StudioModePill(current: StudioMode, onSelect: (StudioMode) -> Unit, modifier: Modifier = Modifier) {
    val inPerformance = current == StudioMode.ANIMATE_IK
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Row(modifier = Modifier.padding(3.dp)) {
            ModeSegment(Lucide.Bone, "Rig Fabricator", selected = !inPerformance, onClick = {
                if (inPerformance) onSelect(StudioMode.RIG_SETUP)
            })
            ModeSegment(Lucide.Drama, "Performance Stage", selected = inPerformance, onClick = {
                if (!inPerformance) onSelect(StudioMode.ANIMATE_IK)
            })
        }
    }
}

@Composable
private fun ModeSegment(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Icon(
            icon,
            contentDescription,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 11.dp).size(18.dp),
            tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun StudioModePillPreview() {
    StudioModePill(current = StudioMode.RIG_SETUP, onSelect = {})
}
