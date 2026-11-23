package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
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
 * Each segment shows its name beside its icon, so the two modes read as words, not puzzles.
 */
@Composable
fun StudioModePill(current: StudioMode, onSelect: (StudioMode) -> Unit, modifier: Modifier = Modifier) {
    val inPerformance = current == StudioMode.ANIMATE_IK
    Surface(
        shape = RoundedCornerShape(Dimens.radiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Row(modifier = Modifier.padding(ActorStudioTokens.pillPadding)) {
            ModeSegment(Lucide.Bone, stringResource(R.string.studio_mode_rig), selected = !inPerformance, onClick = {
                if (inPerformance) onSelect(StudioMode.RIG_SETUP)
            })
            ModeSegment(
                Lucide.Drama,
                stringResource(
                    R.string.studio_mode_performance,
                ),
                selected = inPerformance,
                onClick = {
                    if (!inPerformance) onSelect(StudioMode.ANIMATE_IK)
                },
            )
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
        shape = RoundedCornerShape(ActorStudioTokens.pillCorner),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) ActorStudioTokens.pillSelectedElevation else 0.dp,
    ) {
        val tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        Row(
            modifier = Modifier.padding(
                horizontal = ActorStudioTokens.pillIconPadding,
                vertical = ActorStudioTokens.pillIconPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            Icon(icon, null, modifier = Modifier.size(EditorTokens.icon), tint = tint)
            Text(contentDescription, style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}

@Preview
@Composable
private fun StudioModePillPreview() {
    StudioModePill(current = StudioMode.RIG_SETUP, onSelect = {})
}
