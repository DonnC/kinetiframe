package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.common.StampAsset
import com.donnclab.kinetiframe.preview.PreviewCatalog
import java.io.File

@Composable
fun StampPreviewDialog(stamp: StampAsset, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stamp.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close preview") }
                }
                if (stamp.imagePath != null) {
                    AsyncImage(
                        File(stamp.imagePath),
                        stamp.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(
                            max = 360.dp,
                        ).aspectRatio(1f).background(Color(0xFFF1F1F1)),
                    )
                } else {
                    Icon(stamp.icon, stamp.name, Modifier.fillMaxWidth().height(200.dp), tint = stamp.tint)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StampPreview() {
    KinetiFrameTheme { StampPreviewDialog(PreviewCatalog.packContents().first(), {}) }
}
