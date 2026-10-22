package com.donnclab.kinetiframe.ui.components.dock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.ui.SelectionToolType
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.components.StandardSlider

@Composable
fun SelectionOptionsContent(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.padding(16.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Selection mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EditorTokens.dark,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Lucide.X, contentDescription = "Close", modifier = Modifier.size(16.dp), tint = EditorTokens.dark)
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SelectionTypeRow(
                label = "Lasso",
                selected = state.selection.toolType == SelectionToolType.LASSO,
                onClick = { onIntent(StudioIntent.Selection.SetTool(SelectionToolType.LASSO)) },
            )
            SelectionTypeRow(
                label = "Rectangle",
                selected = state.selection.toolType == SelectionToolType.RECTANGLE,
                onClick = { onIntent(StudioIntent.Selection.SetTool(SelectionToolType.RECTANGLE)) },
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onIntent(StudioIntent.Selection.SetPartialInclude(!state.selection.partialInclude)) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.selection.partialInclude,
                onCheckedChange = { onIntent(StudioIntent.Selection.SetPartialInclude(it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = EditorTokens.accent,
                    uncheckedColor = EditorTokens.inactive,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Include partially selected objects",
                style = MaterialTheme.typography.bodyMedium,
                color = EditorTokens.dark,
            )
        }
    }
}

@Composable
private fun SelectionTypeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = EditorTokens.accent,
                unselectedColor = EditorTokens.inactive,
            ),
            modifier = Modifier.size(32.dp).scale(0.8f),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) EditorTokens.dark else EditorTokens.muted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
