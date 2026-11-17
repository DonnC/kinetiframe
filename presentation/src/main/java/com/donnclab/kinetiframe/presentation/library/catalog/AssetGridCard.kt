package com.donnclab.kinetiframe.presentation.library.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.designsystem.theme.PreviewSwatches
import com.donnclab.kinetiframe.presentation.common.AssetDetails
import com.donnclab.kinetiframe.presentation.common.AssetKind
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.common.CardChips
import com.donnclab.kinetiframe.presentation.common.CardThumbnail
import com.donnclab.kinetiframe.presentation.common.PreviewKind
import com.donnclab.kinetiframe.presentation.common.UiText
import com.donnclab.kinetiframe.presentation.common.asString

/**
 * "Overlay" style grid card -- the thumbnail fills the whole card, spec chips float in its top
 * corners, and a bottom scrim carries the title and subtitle. Used by the Projects and Actors grids.
 */
@Composable
fun AssetGridCard(item: AssetSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(Dimens.radiusL),
        colors = CardDefaults.cardColors(),
    ) {
        AssetThumbnailBox(thumbnail = item.thumbnail, modifier = Modifier.aspectRatio(1f)) {
            item.chips.topStart?.let { SpecChip(it, Modifier.align(Alignment.TopStart).padding(Dimens.spaceS)) }
            item.chips.topEnd?.let { SpecChip(it, Modifier.align(Alignment.TopEnd).padding(Dimens.spaceS)) }
            CardBottomScrim(title = item.title, subtitle = item.scrimSubtitle.asString())
        }
    }
}

private fun previewSample() = AssetSummary(
    id = "preview",
    title = "Walk Cycle Take 3",
    scrimSubtitle = UiText.Verbatim("Performance  ·  0:04"),
    thumbnail = CardThumbnail.SolidGradient(PreviewSwatches.peach, PreviewSwatches.coral),
    chips = CardChips(topEnd = "Performance"),
    details = AssetDetails(
        id = "preview",
        kind = AssetKind.ACTOR_PERFORMANCE,
        previewKind = PreviewKind.LIVE_MOTION,
        title = "Walk Cycle Take 3",
        specsLine = UiText.Verbatim("Humanoid  ·  12 FPS"),
        thumbnail = CardThumbnail.SolidGradient(PreviewSwatches.peach, PreviewSwatches.coral),
    ),
)

@Preview(showBackground = true)
@Composable
private fun AssetGridCardPreview() {
    KinetiFrameTheme { Box(Modifier.padding(Dimens.spaceL)) { AssetGridCard(previewSample(), onClick = {}) } }
}
