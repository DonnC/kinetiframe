package com.donnclab.kinetiframe.presentation.studio.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.BundledPalettes
import com.donnclab.kinetiframe.library.model.PaletteDocument

@Composable
fun PaletteSwatches(palette: PaletteDocument, selectedColor: Color, onPick: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        palette.swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                row.forEach { swatch ->
                    val color = Color(swatch.argb.toInt())
                    val isSelected = selectedColor.toArgb() == swatch.argb.toInt()
                    val hex = swatch.argb.toString(16).padStart(8, '0').uppercase()
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
                                contentDescription = "${swatch.name} #$hex"
                                selected = isSelected
                            }
                            .clickable { onPick(color) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaletteSwatchesPreview() {
    KinetiFrameTheme { PaletteSwatches(BundledPalettes.entries.last(), Color.Black, {}) }
}
