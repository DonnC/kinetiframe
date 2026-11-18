package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.donnclab.kinetiframe.designsystem.theme.Dimens

/** Captions add no toolbar chrome; accessibility and long-press tooltips remain available. */
@Composable
fun ContextMenuActionTile(action: ContextIconAction, showLabel: Boolean) {
    Column(
        Modifier.width(if (showLabel) Dimens.menuTileWidth else Dimens.touchTarget),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContextIconButton(action)
        if (showLabel) {
            Text(
                action.label,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Dimens.spaceXxs, vertical = Dimens.spaceXxs),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelledActionPreview() = ContextMenuActionTile(
    ContextIconAction(Icons.Filled.ContentCopy, "Copy", {
    }),
    true,
)
