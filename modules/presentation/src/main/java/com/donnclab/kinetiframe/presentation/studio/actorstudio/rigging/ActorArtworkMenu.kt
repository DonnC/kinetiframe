package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.studio.StudioIntent

/**
 * Variant choices key only this part at the selected performance timestamp. [choices] are
 * (ID, name) pairs, the default first: cutout variants or a drawn part's poses and faces.
 */
@Composable
fun ActorArtworkMenu(partId: String, choices: List<Pair<String, String>>, onIntent: (StudioIntent) -> Unit) {
    var open by remember(partId) { mutableStateOf(false) }
    Box {
        // A labelled chip on its own surface, so it reads over any stage colour.
        Surface(
            onClick = { open = true },
            enabled = choices.isNotEmpty(),
            shape = RoundedCornerShape(Dimens.radiusPill),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = ActorStudioTokens.floatingElevation,
        ) {
            Row(
                Modifier.padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            ) {
                Icon(Icons.Default.Collections, null, modifier = Modifier.size(EditorTokens.icon))
                Text(stringResource(R.string.part_artwork), style = MaterialTheme.typography.labelLarge)
            }
        }
        DropdownMenu(open, { open = false }) {
            choices.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name, style = MaterialTheme.typography.labelSmall) }, onClick = {
                    open = false
                    onIntent(StudioIntent.Actor.SelectArtwork(partId, id))
                })
            }
        }
    }
}

@Preview
@Composable
private fun ArtworkMenuPreview() {
    MaterialTheme { ActorArtworkMenu("mouth", listOf("" to "Default", "open" to "Open"), {}) }
}
