package com.donnclab.kinetiframe.ui.components.catalog

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
import com.donnclab.kinetiframe.ui.common.model.AssetDetails
import com.donnclab.kinetiframe.ui.common.model.AssetKind
import com.donnclab.kinetiframe.ui.common.model.AssetSummary
import com.donnclab.kinetiframe.ui.common.model.CardChips
import com.donnclab.kinetiframe.ui.common.model.CardThumbnail
import com.donnclab.kinetiframe.ui.common.model.PreviewKind

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
            CardBottomScrim(title = item.title, subtitle = item.scrimSubtitle)
        }
    }
}

private fun previewSample() = AssetSummary(
    id = "preview",
    title = "Walk Cycle Take 3",
    scrimSubtitle = "Performance  ·  0:04",
    thumbnail = CardThumbnail.SolidGradient(Color(0xFFFFD9CC), Color(0xFFFF6A45)),
    chips = CardChips(topEnd = "Performance"),
    details = AssetDetails(
        id = "preview",
        kind = AssetKind.ACTOR_PERFORMANCE,
        previewKind = PreviewKind.LIVE_MOTION,
        title = "Walk Cycle Take 3",
        specsLine = "Humanoid  ·  12 FPS",
        thumbnail = CardThumbnail.SolidGradient(Color(0xFFFFD9CC), Color(0xFFFF6A45)),
    ),
)

@Preview(showBackground = true)
@Composable
private fun AssetGridCardPreview() {
    KinetiFrameTheme { Box(Modifier.padding(Dimens.spaceL)) { AssetGridCard(previewSample(), onClick = {}) } }
}
