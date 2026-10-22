package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens

/**
 * One icon action inside a [ContextActionBar] -- tap a frame, select a clip, whatever the item.
 * [badge] is a small corner glyph for a variant of the same action (see [FrameContextToolbar]'s
 * "paste before" vs "paste after", which share an icon and differ only by which side arrow they
 * carry).
 */
data class ContextAction(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
    val tint: Color? = null,
    val badge: ImageVector? = null,
    val badgeAlignment: Alignment = Alignment.TopEnd,
    val onClick: () -> Unit,
)

/**
 * The one floating action row for anything selected or tapped on the timeline -- a frame, an
 * audio/video clip, an actor placement. Sharing one shell keeps spacing, tint and shape
 * consistent; a new clip type should use this rather than its own `Surface { Row { ... } }`.
 *
 * [label] is the optional clip name shown before the actions (clips have one; a bare frame
 * doesn't). [trailing] is a slot for the rare non-icon control a caller still needs inline (the
 * audio clip's volume [androidx.compose.material3.Slider] and "Move to playhead" button, the
 * actor placement's switch-performance picker and overflow menu) -- everything that _is_ just an
 * icon action belongs in [actions] instead of a bespoke `IconButton`.
 */
@Composable
fun ContextActionBar(
    actions: List<ContextAction>,
    modifier: Modifier = Modifier,
    label: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, EditorTokens.border.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp).widthIn(max = 96.dp),
                )
            }
            actions.forEach { action ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .let { if (action.enabled) it.clickable(onClick = action.onClick) else it }
                        .padding(6.dp)
                        .alpha(if (action.enabled) 1f else 0.4f),
                ) {
                    Icon(
                        action.icon,
                        action.label,
                        tint = action.tint ?: EditorTokens.dark,
                        modifier = Modifier.size(20.dp),
                    )
                    action.badge?.let { badge ->
                        Icon(
                            badge,
                            null,
                            tint = EditorTokens.accent,
                            modifier = Modifier.size(10.dp).align(action.badgeAlignment),
                        )
                    }
                }
            }
            trailing?.invoke()
        }
    }
}
