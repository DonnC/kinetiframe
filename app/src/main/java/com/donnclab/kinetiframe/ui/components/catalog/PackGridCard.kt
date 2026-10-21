package com.donnclab.kinetiframe.ui.components.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.common.model.AssetDetails
import com.donnclab.kinetiframe.ui.common.model.AssetKind
import com.donnclab.kinetiframe.ui.common.model.AssetSummary
import com.donnclab.kinetiframe.ui.common.model.CardThumbnail
import com.donnclab.kinetiframe.ui.common.model.PreviewKind

/**
 * "Info panel" style grid card, used only for Packs: a collage thumbnail on top, then a plain
 * surface panel below with the pack's name and item-count subtitle. Deliberately different from
 * [AssetGridCard] so packs read as a flat, scannable list rather than the overlay style used for
 * projects and actors.
 */
@Composable
fun PackGridCard(item: AssetSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(Dimens.radiusL)) {
        Column {
            AssetThumbnailBox(
                thumbnail = item.thumbnail,
                modifier = Modifier.fillMaxWidth().height(Dimens.packCollageThumbHeight),
            )
            Column(Modifier.padding(Dimens.spaceM)) {
                Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(
                    item.scrimSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun previewSample() = AssetSummary(
    id = "preview",
    title = "Mouth Expressions",
    scrimSubtitle = "12 Items",
    thumbnail = CardThumbnail.SolidGradient(Color(0xFFFFE1D6), Color(0xFFFFC9B3)),
    details = AssetDetails(
        id = "preview",
        kind = AssetKind.PACK,
        previewKind = PreviewKind.LIVE_COLLAGE,
        title = "Mouth Expressions",
        specsLine = "12 Items in this pack",
        thumbnail = CardThumbnail.SolidGradient(Color(0xFFFFE1D6), Color(0xFFFFC9B3)),
    ),
)

@Preview(showBackground = true)
@Composable
private fun PackGridCardPreview() {
    KinetiFrameTheme {
        Box(Modifier.padding(Dimens.spaceL).aspectRatio(0.78f)) { PackGridCard(previewSample(), onClick = {}) }
    }
}
