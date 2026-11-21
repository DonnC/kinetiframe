package com.donnclab.kinetiframe.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pen
import com.donnclab.kinetiframe.designsystem.R
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockToolButton(action: DockToolAction, selected: Boolean, onOpenMenu: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(36.dp).combinedClickable(
            onClick = { if (selected) onOpenMenu() else action.onSelect() },
            onLongClick = onOpenMenu,
            onDoubleClick = onOpenMenu,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(28.dp).background(EditorTokens.accent.copy(alpha = 0.15f), CircleShape))
        }
        Icon(
            action.icon,
            action.label,
            Modifier.size(EditorTokens.icon),
            tint = if (selected) EditorTokens.accent else EditorTokens.dark,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DockToolButtonPreview() {
    KinetiFrameTheme {
        DockToolButton(DockToolAction(Lucide.Pen, stringResource(R.string.designsystem_preview_pen), {}), true, {})
    }
}
