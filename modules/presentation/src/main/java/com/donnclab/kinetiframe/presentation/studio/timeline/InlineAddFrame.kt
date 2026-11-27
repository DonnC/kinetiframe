package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens
import com.donnclab.kinetiframe.presentation.R

/** A pinned blank frame cell, not another standalone toolbar action. */
@Composable
fun InlineAddFrame(onClick: () -> Unit) {
    val scale = LocalTimelineScale.current
    Box(Modifier.size(scale.cell, scale.stripHeight), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(scale.thumbWidth, scale.thumbHeight),
            shape = RoundedCornerShape(TimelineTokens.frameThumbCorner),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, stringResource(R.string.timeline_add_frame), Modifier.size(EditorTokens.icon))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InlineAddFramePreview() {
    InlineAddFrame({})
}
