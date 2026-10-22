package com.donnclab.kinetiframe.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import java.io.File

/** Lazy bounded Coil decoding: the picker displays thumbnails, never full-resolution bitmaps. */
@Composable
internal fun LibraryImageThumbnail(file: File, label: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = file,
        contentDescription = "$label preview",
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun LibraryImageThumbnailPreview() {
    KinetiFrameTheme { LibraryImageThumbnail(File("living-room.png"), "Living room") }
}
