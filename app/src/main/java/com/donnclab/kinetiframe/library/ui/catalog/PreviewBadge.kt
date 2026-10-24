package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** The small pill in an Asset Details Card's preview area saying what kind of preview this is --
 * "Live preview -- loops", "Static preview", or "Preview" for audio. Text is passed in rather than
 * derived here, since the exact copy differs slightly by [com.donnclab.kinetiframe.presentation.common.PreviewKind]. */
@Composable
fun PreviewBadge(label: String, icon: ImageVector = Icons.Filled.PlayArrow, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(Dimens.iconXs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBadgePreview() {
    KinetiFrameTheme { PreviewBadge(stringResource(R.string.asset_preview_live_loops)) }
}
