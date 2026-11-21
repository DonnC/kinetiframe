package com.donnclab.kinetiframe.presentation.studio.dock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.StandardSlider
import com.donnclab.kinetiframe.studio.EraserType
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ToolConfig

@Composable
fun EraserOptionsContent(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.padding(Dimens.spaceL).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.eraser_title),
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

        val eraserConfig = state.tool.configs[DrawingTool.ERASER] ?: ToolConfig()
        // Radio options for "Stroke eraser" and "Area eraser"
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            EraserTypeRow(
                label = stringResource(R.string.eraser_stroke),
                selected = eraserConfig.eraserType == EraserType.STROKE,
                onClick = { onIntent(StudioIntent.Draw.SetEraserType(EraserType.STROKE)) },
            )
            EraserTypeRow(
                label = stringResource(R.string.eraser_area),
                selected = eraserConfig.eraserType == EraserType.AREA,
                onClick = { onIntent(StudioIntent.Draw.SetEraserType(EraserType.AREA)) },
            )
        }

        Spacer(Modifier.height(Dimens.spaceXl))

        StandardSlider(
            label = stringResource(R.string.tool_size),
            value = eraserConfig.size,
            onValueChange = { onIntent(StudioIntent.Draw.SetEraserSize(it)) },
            range = 1f..100f,
        )

        Spacer(Modifier.height(EditorTokens.sectionGap))

        Button(
            onClick = { onIntent(StudioIntent.Draw.ClearAllHandwriting) },
            modifier = Modifier.fillMaxWidth().height(EditorTokens.panelAction),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = EditorTokens.accent,
            ),
            border = BorderStroke(Dimens.borderThin, EditorTokens.inactive.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(Dimens.radiusS),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                stringResource(R.string.eraser_clear_all),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EraserTypeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusS))
            .combinedClickable(onClick = onClick)
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
