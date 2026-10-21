package com.donnclab.kinetiframe.ui.components.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Small translucent label floated over a card thumbnail -- fps, canvas size, rig type, or
 * anything else a grid card wants to surface at a glance. Positioning (top-start vs top-end) is
 * the caller's job via [modifier], so this stays a single reusable shape. */
@Composable
fun SpecChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.radiusS))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceXs),
    )
}

@Preview(showBackground = true)
@Composable
private fun SpecChipPreview() {
    KinetiFrameTheme { SpecChip("1080×1920") }
}
