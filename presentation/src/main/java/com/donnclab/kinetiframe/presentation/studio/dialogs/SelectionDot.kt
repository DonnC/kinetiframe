package com.donnclab.kinetiframe.presentation.studio.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens

/** A filled, checked circle when [selected]; an empty outline otherwise. The one "pick exactly
 *  one of these" indicator shared by every option row across the Home "Add" flows (canvas-size
 *  presets, pack sizes). */
@Composable
fun SelectionDot(selected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            Dimens.borderThick,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.size(EditorTokens.selectionDot),
    ) {
        if (selected) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(EditorTokens.selectionDotIcon),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectionDotPreview() = SelectionDot(selected = true)
