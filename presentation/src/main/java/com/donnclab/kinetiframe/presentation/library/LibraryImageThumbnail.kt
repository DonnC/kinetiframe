package com.donnclab.kinetiframe.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R
import java.io.File

/** Lazy bounded Coil decoding: the picker displays thumbnails, never full-resolution bitmaps. */
@Composable
internal fun LibraryImageThumbnail(file: File, label: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = file,
        contentDescription = stringResource(R.string.library_item_preview, label),
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(Dimens.radiusXs))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun LibraryImageThumbnailPreview() {
    KinetiFrameTheme { LibraryImageThumbnail(File("living-room.png"), "Living room") }
}
