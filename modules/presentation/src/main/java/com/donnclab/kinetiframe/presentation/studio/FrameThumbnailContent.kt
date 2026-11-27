package com.donnclab.kinetiframe.presentation.studio

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens
import com.donnclab.kinetiframe.presentation.R

/**
 * A frame as it looks on the canvas: the whole drawing on paper, in the canvas's own shape and
 * centred in the tile. Never crops or stretches frame pixels.
 */
@Composable
fun FrameThumbnailContent(
    bitmap: Bitmap?,
    index: Int,
    current: Boolean,
    compact: Boolean = false,
    numberAlignment: Alignment = Alignment.TopEnd,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val dot = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.fillMaxSize()) {
        if (bitmap != null) {
            // Cropping to the tile cut a portrait frame down to a centre square, and clear pixels
            // on the dark tile hid dark lines (R3-19). Thumbnails keep the stage's proportions.
            val canvasAspect = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    bitmap.asImageBitmap(),
                    stringResource(R.string.frame_thumbnail_description, index + 1),
                    Modifier.aspectRatio(canvasAspect).background(EditorTokens.framePaper),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            Canvas(Modifier.fillMaxSize()) { drawCircle(dot, radius = TimelineTokens.thumbDot.toPx()) }
        }
        Surface(
            Modifier.align(numberAlignment).padding(if (compact) Dimens.spaceXxs else Dimens.spaceXsPlus),
            shape = RoundedCornerShape(Dimens.radiusXxs),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
        ) {
            Text(
                "${index + 1}",
                Modifier.padding(
                    horizontal = if (compact) Dimens.spaceXxs else Dimens.spaceXsPlus,
                    vertical = Dimens.spaceXxxs,
                ),
                color = if (current) EditorTokens.accent else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 9.sp else 11.sp),
            )
        }
        overlay()
    }
}

@Preview(showBackground = true)
@Composable
private fun FrameThumbnailPreview() {
    MaterialTheme { Box(Modifier.size(160.dp, 120.dp)) { FrameThumbnailContent(null, 0, false) } }
}
