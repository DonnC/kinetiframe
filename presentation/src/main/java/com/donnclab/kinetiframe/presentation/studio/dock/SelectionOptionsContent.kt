package com.donnclab.kinetiframe.presentation.studio.dock

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.StandardSlider
import com.donnclab.kinetiframe.studio.SelectionToolType
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

@Composable
fun SelectionOptionsContent(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.padding(Dimens.spaceL).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.selection_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EditorTokens.dark,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(EditorTokens.closeButton)) {
                Icon(
                    Lucide.X,
                    contentDescription = stringResource(R.string.action_close),
                    modifier = Modifier.size(Dimens.iconS),
                    tint = EditorTokens.dark,
                )
            }
        }

        Spacer(Modifier.height(Dimens.spaceL))

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            SelectionTypeRow(
                label = stringResource(R.string.tool_lasso),
                selected = state.selection.toolType == SelectionToolType.LASSO,
                onClick = { onIntent(StudioIntent.Selection.SetTool(SelectionToolType.LASSO)) },
            )
            SelectionTypeRow(
                label = stringResource(R.string.selection_rectangle),
                selected = state.selection.toolType == SelectionToolType.RECTANGLE,
                onClick = { onIntent(StudioIntent.Selection.SetTool(SelectionToolType.RECTANGLE)) },
            )
        }

        Spacer(Modifier.height(Dimens.spaceXl))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusS))
                .clickable { onIntent(StudioIntent.Selection.SetPartialInclude(!state.selection.partialInclude)) }
                .padding(vertical = Dimens.spaceXs),
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
            Spacer(Modifier.width(Dimens.spaceS))
            Text(
                stringResource(R.string.selection_partial),
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
            .clip(RoundedCornerShape(Dimens.radiusS))
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = EditorTokens.accent,
                unselectedColor = EditorTokens.inactive,
            ),
            modifier = Modifier.size(EditorTokens.control).scale(0.8f),
        )
        Spacer(Modifier.width(Dimens.spaceXs))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) EditorTokens.dark else EditorTokens.muted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
