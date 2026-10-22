package com.donnclab.kinetiframe.studio.ui.frames

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.ui.FrameThumbnailContent

@Composable
fun StudioFrameCard(
    index: Int,
    bitmap: Bitmap?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) EditorTokens.accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (selected) EditorTokens.accent else Color.Transparent,
        ),
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            FrameThumbnailContent(bitmap, index, selected, numberAlignment = Alignment.TopStart) {
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                ) {
                    IconButton(onClick = onOpen, modifier = Modifier.size(40.dp)) {
                        Icon(Lucide.Eye, "Open frame", Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudioFrameCardPreview() {
    MaterialTheme { StudioFrameCard(0, null, true, Modifier.size(190.dp, 150.dp), {}, {}) }
}
