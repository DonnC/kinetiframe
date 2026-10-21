package com.donnclab.kinetiframe.ui.components.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.common.model.StampAsset

/** One tile in a pack's contents drill-down grid -- a tinted icon over a light surface, with the
 * stamp's name underneath. */
@Composable
fun StampThumbnail(stamp: StampAsset, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            // Neutral artboard is content, not chrome: black transparent ink must remain visible in dark mode.
            Modifier.aspectRatio(1f).clip(RoundedCornerShape(Dimens.radiusM)).background(Color(0xFFF1F1F1)),
            contentAlignment = Alignment.Center,
        ) {
            if (stamp.imagePath !=
                null
            ) {
                coil.compose.AsyncImage(
                    model = java.io.File(stamp.imagePath),
                    contentDescription = stamp.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.aspectRatio(1f),
                )
            } else {
                Icon(
                    stamp.icon,
                    contentDescription = stamp.name,
                    tint = stamp.tint,
                    modifier = Modifier.size(Dimens.iconL),
                )
            }
        }
        Text(
            stamp.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StampThumbnailPreview() {
    KinetiFrameTheme { StampThumbnail(StampAsset("preview", "Smile", Lucide.Circle, Color(0xFFFF6A45))) }
}
