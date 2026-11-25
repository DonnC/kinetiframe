package com.donnclab.kinetiframe.presentation.library.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.designsystem.theme.PreviewSwatches
import com.donnclab.kinetiframe.presentation.common.AssetDetails
import com.donnclab.kinetiframe.presentation.common.AssetKind
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.common.CardChips
import com.donnclab.kinetiframe.presentation.common.CardThumbnail
import com.donnclab.kinetiframe.presentation.common.MediaBadge
import com.donnclab.kinetiframe.presentation.common.MediaCard
import com.donnclab.kinetiframe.presentation.common.PreviewKind
import com.donnclab.kinetiframe.presentation.common.UiText
import com.donnclab.kinetiframe.presentation.common.asString

/**
 * A Library grid card for any kind of item (projects, actors, packs, images), in the same
 * picture-on-top, caption-below style as Home's cards. Spec chips sit on the picture's top corners.
 */
@Composable
fun AssetGridCard(item: AssetSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chips = item.chips
    MediaCard(
        title = item.title,
        subtitle = item.scrimSubtitle.asString().takeIf { it.isNotBlank() },
        onClick = onClick,
        modifier = modifier,
        media = { AssetThumbnailBox(thumbnail = item.thumbnail) },
        badges = if (chips.topStart == null && chips.topEnd == null) {
            null
        } else {
            {
                chips.topStart?.let { MediaBadge(it, Modifier.weight(1f, fill = false)) } ?: Spacer(Modifier)
                chips.topEnd?.let { MediaBadge(it, Modifier.weight(1f, fill = false)) }
            }
        },
    )
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
