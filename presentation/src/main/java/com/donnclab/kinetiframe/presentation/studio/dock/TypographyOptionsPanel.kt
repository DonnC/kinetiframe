package com.donnclab.kinetiframe.presentation.studio.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.StandardSlider
import com.donnclab.kinetiframe.presentation.common.valueText
import com.donnclab.kinetiframe.presentation.studio.dialogs.TextInsertionDialog
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.drawing.FontItem
import com.donnclab.kinetiframe.studio.drawing.FontRegistry

/**
 * The main panel for text tool options, showing Add, Font, Size, and Color.
 */
@Composable
fun TypographyOptionsPanel(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("main") }
    val context = LocalContext.current
    val fontRegistry = remember { FontRegistry(context) }
    // A selected node lives in SelectedContent until its transform is committed.
    // Resolve both locations so properties remain available throughout a gesture.
    val selectedNode = state.scene.activeNodeId?.let { id ->
        (state.scene.nodes[id] ?: state.selection.selectedContent?.nodes?.get(id)) as? TextNode
    }

    if (showAddDialog) {
        TextInsertionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text ->
                onIntent(StudioIntent.Text.AddText(text))
                showAddDialog = false
            },
        )
    }
    if (showEditDialog && selectedNode != null) {
        TextInsertionDialog(
            initialText = selectedNode.text,
            onDismiss = { showEditDialog = false },
            onConfirm = { text ->
                onIntent(StudioIntent.Text.UpdateText(selectedNode.id, text))
                showEditDialog = false
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            "main" -> {
                Column(
                    modifier = Modifier
                        .padding(Dimens.spaceL)
                        .fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.text_tool_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(Dimens.spaceL))

                    if (selectedNode == null) {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.radiusS),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(EditorTokens.icon))
                            Spacer(Modifier.width(Dimens.spaceS))
                            Text(stringResource(R.string.text_add))
                        }
                    } else {
                        Text(
                            stringResource(R.string.text_edit_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = EditorTokens.muted,
                        )
                        TextButton(onClick = { showEditDialog = true }) { Text(stringResource(R.string.pack_edit)) }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceL))

                    // Font Selection
                    Text(
                        stringResource(R.string.text_font),
                        style = MaterialTheme.typography.labelMedium,
                        color = EditorTokens.muted,
                    )
                    Spacer(Modifier.height(Dimens.spaceXs))
                    OutlinedButton(
                        onClick = { currentScreen = "fonts" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.radiusS),
                    ) {
                        val currentFontName = selectedNode?.fontFileName ?: state.tool.textToolOptions.fontFileName
                        val currentFont = fontRegistry.availableFonts.find { it.fileName == currentFontName }
                        Text(
                            text = currentFont?.displayName ?: stringResource(R.string.text_select_font),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceL))

                    // Size
                    StandardSlider(
                        label = stringResource(R.string.tool_size),
                        value = selectedNode?.fontSizeSp ?: state.tool.textToolOptions.fontSizeSp,
                        onValueChange = { value ->
                            selectedNode?.let { node ->
                                onIntent(StudioIntent.Text.UpdateTextNode(node.copy(fontSizeSp = value)))
                            }
                                ?: onIntent(
                                    StudioIntent.Text.SetTextToolOptions(
                                        state.tool.textToolOptions.copy(fontSizeSp = value),
                                    ),
                                )
                        },
                        range = 8f..200f,
                        valueDisplay = valueText(R.string.value_sp) { it.toInt() },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceL))

                    // Color
                    Text(
                        stringResource(R.string.text_color),
                        style = MaterialTheme.typography.labelMedium,
                        color = EditorTokens.muted,
                    )
                    Spacer(Modifier.height(Dimens.spaceS))
                    ColorPicker(
                        selectedNode?.let {
                            Color(it.style.mainColorHex)
                        } ?: Color(state.tool.textToolOptions.textColor),
                    ) {
                        selectedNode?.let { node ->
                            onIntent(
                                StudioIntent.Text.UpdateTextNode(
                                    node.copy(style = node.style.copy(mainColorHex = it.toArgb())),
                                ),
                            )
                        }
                            ?: onIntent(
                                StudioIntent.Text.SetTextToolOptions(
                                    state.tool.textToolOptions.copy(textColor = it.toArgb()),
                                ),
                            )
                    }
                }
            }

            "fonts" -> {
                FontSelectionPanel(
                    selectedFontFileName = selectedNode?.fontFileName ?: state.tool.textToolOptions.fontFileName,
                    onFontSelected = { font ->
                        selectedNode?.let { node ->
                            onIntent(StudioIntent.Text.UpdateTextNode(node.copy(fontFileName = font.fileName)))
                        }
                            ?: onIntent(
                                StudioIntent.Text.SetTextToolOptions(
                                    state.tool.textToolOptions.copy(fontFileName = font.fileName),
                                ),
                            )
                        currentScreen = "main"
                    },
                    onBack = { currentScreen = "main" },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * A panel for selecting fonts, displaying previews of each font.
 */
@Composable
fun FontSelectionPanel(
    selectedFontFileName: String?,
    onFontSelected: (FontItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fontRegistry = remember { FontRegistry(context) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(EditorTokens.textPanelWidth)
            .background(
                androidx.compose.material3.MaterialTheme.colorScheme.surface,
                RoundedCornerShape(Dimens.radiusM),
            )
            .padding(Dimens.spaceS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                stringResource(R.string.text_select_font),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Dimens.spaceS),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceS))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            items(fontRegistry.availableFonts) { font ->
                FontItemRow(
                    font = font,
                    selected = font.fileName == selectedFontFileName,
                    onClick = { onFontSelected(font) },
                )
            }
        }
    }
}

@Composable
private fun FontItemRow(font: FontItem, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = font.displayName,
                style = TextStyle(
                    fontFamily = FontFamily(font.typeface),
                    fontSize = 16.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.state_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconM),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 220, heightDp = 500)
@Composable
private fun TypographyOptionsPanelPreview() {
    TypographyOptionsPanel(state = StudioState(), onIntent = {}, onDismiss = {})
}
