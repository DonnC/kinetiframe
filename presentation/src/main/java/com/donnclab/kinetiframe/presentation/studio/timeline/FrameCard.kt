package com.donnclab.kinetiframe.presentation.studio.timeline

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.studio.FrameThumbnailContent

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FrameCard(index: Int, isCurrent: Boolean, thumbnail: Bitmap? = null, linked: Boolean = false, onClick: () -> Unit) {
    val borderColor = when {
        isCurrent -> EditorTokens.accent.copy(alpha = 0.6f)
        else -> EditorTokens.border
    }
    val borderWidth = if (isCurrent) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .width(36.dp)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.size(36.dp, 40.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isCurrent -> EditorTokens.accent.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isCurrent) EditorTokens.accent else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(
                width = borderWidth,
                color = borderColor,
            ),
        ) {
            FrameThumbnailContent(thumbnail, index, isCurrent, compact = true) {
                if (linked) {
                    Icon(
                        Icons.Default.Link,
                        "Linked drawing",
                        modifier = Modifier.align(Alignment.BottomStart).size(12.dp),
                        tint = EditorTokens.accent,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FrameCardPreview() {
    KinetiFrameTheme {
        Box(Modifier.padding(16.dp)) {
            FrameCard(
                index = 0,
                isCurrent = true,
                onClick = {},
            )
        }
    }
}
