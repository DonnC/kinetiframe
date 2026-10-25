package com.donnclab.kinetiframe.library.ui.packs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.PackItemEntry
import java.io.File

/**
 * One real drawing inside a saved pack: the pack's own PNG read in place off disk (never a
 * stand-in icon or a copy), squared off and captioned with the name the animator gave it while
 * building the pack.
 *
 * A separate component so every screen that shows pack drawings renders the same tile.
 */
@Composable
fun PackItemThumbnail(entry: PackItemEntry, file: File, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = file,
            contentDescription = entry.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Dimens.radiusM))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            entry.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Preview(name = "Pack item -- light", showBackground = true)
@Preview(
    name = "Pack item -- dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PackItemThumbnailPreview() {
    KinetiFrameTheme {
        PackItemThumbnail(
            entry = PackItemEntry(file = "frame_001.png", index = 0, name = "Smile"),
            file = File(""),
        )
    }
}
