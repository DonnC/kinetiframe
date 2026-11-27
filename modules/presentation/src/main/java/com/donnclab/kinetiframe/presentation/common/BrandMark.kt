package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.BrandArt
import com.donnclab.kinetiframe.designsystem.theme.BrandCarmine
import com.donnclab.kinetiframe.designsystem.theme.BrandGraphite
import com.donnclab.kinetiframe.designsystem.theme.BrandPaper
import com.donnclab.kinetiframe.designsystem.theme.Carmine70
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R

private fun parse(pathData: String): Path =
    PathParser().parsePathString(pathData).toPath().apply { fillType = PathFillType.EvenOdd }

/**
 * The bouncing-ball mark: the ball round near the top of its fall, stretched as it speeds up,
 * and squashed on the ground. Earlier frames are carmine, like onion skins on paper.
 * Vector art, crisp at any size; it fills the box it is given.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, contentDescription: String? = null) {
    val pieces = remember { BrandArt.mark.map { parse(it.pathData) to it.colour.toColor() } }
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier.then(semantics)) {
        // The art leaves room for a launcher's crop; on a whole tile it can grow.
        val zoomed = BrandArt.MARK_SIZE / BrandArt.TILE_SCALE
        fitted(zoomed, zoomed) {
            translate((zoomed - BrandArt.MARK_SIZE) / 2f, (zoomed - BrandArt.MARK_SIZE) / 2f) {
                pieces.forEach { (path, colour) -> drawPath(path, colour) }
            }
        }
    }
}

/** The hand-lettered wordmark in the surface's ink, with carmine bouncing balls for the i dots. */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    val letters = remember { parse(BrandArt.WORDMARK_LETTERS) }
    val dots = remember { parse(BrandArt.WORDMARK_DOTS) }
    val ink = MaterialTheme.colorScheme.onSurface
    val dot = if (MaterialTheme.colorScheme.surface.luminanceIsDark()) Carmine70 else BrandCarmine
    Canvas(modifier) {
        fitted(BrandArt.WORDMARK_WIDTH, BrandArt.WORDMARK_HEIGHT) {
            drawPath(letters, ink)
            drawPath(dots, dot)
        }
    }
}

/** The launcher look: the mark on a paper tile, with a hairline so it holds on light surfaces. */
@Composable
fun BrandBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(Dimens.radiusSPlus),
        color = BrandPaper,
        border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            BrandMark(Modifier.fillMaxSize(), contentDescription = stringResource(R.string.brand_name))
        }
    }
}

/** Scales art drawn on a [width] by [height] grid to fit and centre in the canvas. */
private fun DrawScope.fitted(width: Float, height: Float, draw: DrawScope.() -> Unit) {
    val scale = minOf(size.width / width, size.height / height)
    withTransform({
        translate((size.width - width * scale) / 2f, (size.height - height * scale) / 2f)
        scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
    }) { draw() }
}

private fun BrandArt.BrandArtColour.toColor(): Color = when (this) {
    BrandArt.BrandArtColour.GRAPHITE -> BrandGraphite
    BrandArt.BrandArtColour.CARMINE -> BrandCarmine
}

private fun Color.luminanceIsDark(): Boolean = (red * 0.2126f + green * 0.7152f + blue * 0.0722f) < 0.5f

@Preview(showBackground = true)
@Composable
private fun BrandMarkPreview() {
    KinetiFrameTheme {
        Surface {
            Row(Modifier.padding(Dimens.spaceL)) {
                BrandMark(Modifier.size(96.dp))
                BrandBadge(Modifier.size(48.dp))
                BrandWordmark(Modifier.size(160.dp, 28.dp))
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BrandMarkDarkPreview() {
    KinetiFrameTheme(darkTheme = true) { Surface { BrandWordmark(Modifier.size(160.dp, 28.dp)) } }
}
