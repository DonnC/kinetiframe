package com.donnclab.kinetiframe.presentation.studio

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens

/** Shared edge-to-edge policy. Crop preserves proportions; never stretches frame pixels. */
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
            Image(
                bitmap.asImageBitmap(),
                "Frame ${index + 1} preview",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) { drawCircle(dot, radius = 2.dp.toPx()) }
        }
        Surface(
            Modifier.align(numberAlignment).padding(if (compact) 2.dp else 6.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
        ) {
            Text(
                "${index + 1}",
                Modifier.padding(horizontal = if (compact) 2.dp else 6.dp, vertical = 1.dp),
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
