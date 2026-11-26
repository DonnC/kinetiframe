// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.color

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R

/** One palette in [PaletteDropdown]: its ID and the name shown. */
data class PaletteChoice(val id: String, val name: String)

/**
 * Chooses the palette shown in the editor's colour picker. A dropdown, not a row of chips: chips
 * took over the narrow phone panel as palettes were added. New palette is the last item.
 */
@Composable
fun PaletteDropdown(
    choices: List<PaletteChoice>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onNewPalette: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val current = choices.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.color_palettes)
    Box(modifier) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = current },
        ) {
            Text(
                current,
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Lucide.ChevronDown, stringResource(R.string.palette_choose), Modifier.size(Dimens.iconS))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.name) },
                    onClick = {
                        open = false
                        onSelect(choice.id)
                    },
                    trailingIcon = if (choice.id == selectedId) {
                        { Icon(Lucide.Check, null, Modifier.size(Dimens.iconS)) }
                    } else {
                        null
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.palette_new)) },
                onClick = {
                    open = false
                    onNewPalette()
                },
                leadingIcon = { Icon(Lucide.Plus, null, Modifier.size(Dimens.iconS)) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaletteDropdownPreview() {
    KinetiFrameTheme {
        PaletteDropdown(
            listOf(PaletteChoice("mine", "My colours"), PaletteChoice("signal", "Loose Signal")),
            selectedId = "signal",
            onSelect = {},
            onNewPalette = {},
        )
    }
}
