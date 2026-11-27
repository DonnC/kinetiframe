package com.donnclab.kinetiframe.presentation.studio.color

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.BundledPalettes
import com.donnclab.kinetiframe.library.model.ColorSwatch
import com.donnclab.kinetiframe.library.model.PaletteDocument
import com.donnclab.kinetiframe.library.model.PaletteEdits
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.library.tabs.ColourDetailDialog

/**
 * A palette's colours in rows of four. Long-pressing a colour opens it, with its hex and, given
 * [onRemove], Remove: the same step as tapping a colour in Library (R3-6).
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PaletteSwatches(
    palette: PaletteDocument,
    selectedColor: Color,
    onPick: (Color) -> Unit,
    onRemove: ((ColorSwatch) -> Unit)? = null,
) {
    val openLabel = stringResource(R.string.palette_open_colour)
    var opened by remember { mutableStateOf<ColorSwatch?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        palette.swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                row.forEach { swatch ->
                    val color = Color(swatch.argb.toInt())
                    val isSelected = selectedColor.toArgb() == swatch.argb.toInt()
                    val spoken = PaletteEdits.spokenName(swatch.name, swatch.argb)
                    Box(
                        Modifier.size(EditorTokens.paletteSwatch).background(color, RoundedCornerShape(Dimens.radiusXs))
                            .border(
                                if (isSelected) EditorTokens.swatchBorderSelected else Dimens.borderThin,
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                RoundedCornerShape(Dimens.radiusXs),
                            )
                            .semantics {
                                contentDescription = spoken
                                selected = isSelected
                            }
                            .combinedClickable(
                                onClick = { onPick(color) },
                                onLongClickLabel = openLabel,
                                onLongClick = { opened = swatch },
                            ),
                    )
                }
            }
        }
    }
    opened?.let { swatch ->
        ColourDetailDialog(swatch, onRemove?.let { remove -> { remove(swatch) } }, onDismiss = { opened = null })
    }
}

@Preview(showBackground = true)
@Composable
private fun PaletteSwatchesPreview() {
    KinetiFrameTheme { PaletteSwatches(BundledPalettes.entries.last(), Color.Black, {}) }
}
