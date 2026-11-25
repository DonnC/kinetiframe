// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens

/** Every card's picture has the same proportions, so grids line up whatever the item. */
private const val MEDIA_ASPECT = 4f / 3f

/**
 * The one card every grid uses: projects, actors, packs, images and collections.
 *
 * The picture sits on top with optional [badges] (short facts such as fps or size) in its corners;
 * the title and a [subtitle] sit below it on a solid caption, so they read on any artwork, light or
 * dark. A [menu] sits at the caption's end.
 *
 * [contentModifier] goes inside the card's tap handling, so a gesture there, such as long-press to
 * drag, sees the touch before the tap does.
 */
@Composable
fun MediaCard(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    media: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    badges: (@Composable RowScope.() -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null,
    contentModifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(HomeTokens.cardCorner)
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(contentModifier) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(MEDIA_ASPECT)
                    .clip(RoundedCornerShape(topStart = HomeTokens.cardCorner, topEnd = HomeTokens.cardCorner)),
            ) {
                media()
                badges?.let {
                    Row(
                        Modifier.fillMaxWidth().padding(Dimens.spaceS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        content = it,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.spaceSPlus, top = Dimens.spaceS, bottom = Dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (menu != null) menu() else Spacer(Modifier.size(Dimens.spaceSPlus))
            }
        }
    }
}

/** The ⋮ at the end of a card's caption. Picking an action closes the menu before running it. */
@Composable
fun MediaCardMenu(description: String, actions: List<ContextIconAction>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconContextMenu(
            open,
            { open = false },
            actions.map { action ->
                action.copy(onClick = {
                    open = false
                    action.onClick()
                })
            },
        )
    }
}

/** A short fact on a card's picture, such as "12 fps": light text on a dark chip, readable on any art. */
@Composable
fun MediaBadge(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(modifier = modifier, shape = RoundedCornerShape(HomeTokens.chipCorner), color = HomeTokens.chipBg) {
        Row(
            Modifier.padding(horizontal = HomeTokens.badgePaddingHorizontal, vertical = Dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(it, null, tint = Color.White, modifier = Modifier.size(Dimens.iconXs))
                Spacer(Modifier.size(Dimens.spaceXxsPlus))
            }
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
