package com.donnclab.kinetiframe.ui.components.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.common.model.CardThumbnail

/**
 * Renders one [CardThumbnail] variant -- a single centered icon on a gradient, a 2x2 icon collage
 * (packs), or a plain gradient (Library project cards, which carry their info as chips instead).
 * The one place a card's background art gets drawn, so every grid card -- overlay-style or
 * info-panel-style -- looks like it came from the same illustration system.
 */
@Composable
fun AssetThumbnailBox(
    thumbnail: CardThumbnail,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(thumbnailBrush(thumbnail))) {
        when (thumbnail) {
            is CardThumbnail.LocalImage -> coil.compose.AsyncImage(
                model = java.io.File(thumbnail.path),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            is CardThumbnail.IconOnGradient -> Icon(
                thumbnail.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).padding(Dimens.spaceXxl),
            )

            is CardThumbnail.Collage -> CollageGrid(thumbnail)

            is CardThumbnail.SolidGradient -> Unit
        }
        overlay()
    }
}

@Composable
private fun BoxScope.CollageGrid(collage: CardThumbnail.Collage) {
    Column(Modifier.fillMaxSize()) {
        collage.icons.chunked(2).forEach { rowIcons ->
            Row(Modifier.weight(1f).fillMaxSize()) {
                rowIcons.forEach { icon ->
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(Dimens.spaceL),
                        )
                    }
                }
            }
        }
    }
}

private fun thumbnailBrush(thumbnail: CardThumbnail): Brush = when (thumbnail) {
    is CardThumbnail.LocalImage -> Brush.linearGradient(listOf(Color(0xFFF3F0F4), Color(0xFFE5E0E8)))
    is CardThumbnail.IconOnGradient -> Brush.linearGradient(listOf(thumbnail.start, thumbnail.end))
    is CardThumbnail.Collage -> Brush.linearGradient(listOf(thumbnail.start, thumbnail.end))
    is CardThumbnail.SolidGradient -> Brush.linearGradient(listOf(thumbnail.start, thumbnail.end))
}

@Preview(showBackground = true)
@Composable
private fun AssetThumbnailBoxPreview() {
    KinetiFrameTheme {
        AssetThumbnailBox(
            thumbnail = CardThumbnail.IconOnGradient(Color(0xFFFFD9CC), Color(0xFFFF6A45), Icons.Filled.Star),
            modifier = Modifier.aspectRatio(1.3f),
        )
    }
}
