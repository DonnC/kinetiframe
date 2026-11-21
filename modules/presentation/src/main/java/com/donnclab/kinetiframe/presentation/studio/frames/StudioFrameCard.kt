package com.donnclab.kinetiframe.presentation.studio.frames

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.studio.FrameThumbnailContent

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
        shape = RoundedCornerShape(Dimens.radiusMPlus),
        color = if (selected) EditorTokens.accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            Dimens.borderThick,
            if (selected) EditorTokens.accent else Color.Transparent,
        ),
    ) {
        Box(Modifier.fillMaxWidth().height(EditorTokens.frameStudioCardThumb)) {
            FrameThumbnailContent(bitmap, index, selected, numberAlignment = Alignment.TopStart) {
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(Dimens.spaceXsPlus),
                    shape = RoundedCornerShape(Dimens.radiusPill),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                ) {
                    IconButton(onClick = onOpen, modifier = Modifier.size(Dimens.iconButton)) {
                        Icon(Lucide.Eye, stringResource(R.string.frames_open), Modifier.size(Dimens.iconM))
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
