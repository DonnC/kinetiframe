package com.donnclab.kinetiframe.studio.ui.dock

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.common.ui.StandardSlider
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.drawing.FontItem
import com.donnclab.kinetiframe.studio.drawing.FontRegistry
import com.donnclab.kinetiframe.studio.ui.dialogs.TextInsertionDialog

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
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        "Text Tool",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(16.dp))

                    if (selectedNode == null) {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Text")
                        }
                    } else {
                        Text("Edit text", style = MaterialTheme.typography.labelMedium, color = EditorTokens.muted)
                        TextButton(onClick = { showEditDialog = true }) { Text("Edit") }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Font Selection
                    Text("Font", style = MaterialTheme.typography.labelMedium, color = EditorTokens.muted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { currentScreen = "fonts" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        val currentFontName = selectedNode?.fontFileName ?: state.tool.textToolOptions.fontFileName
                        val currentFont = fontRegistry.availableFonts.find { it.fileName == currentFontName }
                        Text(
                            text = currentFont?.displayName ?: "Select Font",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Size
                    StandardSlider(
                        label = "Size",
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
                        valueDisplay = { "${it.toInt()}sp" },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Color
                    Text("Color", style = MaterialTheme.typography.labelMedium, color = EditorTokens.muted)
                    Spacer(Modifier.height(8.dp))
                    ColorPicker(
                        selectedNode?.let {
                            Color(it.style.mainColorHex)
                        } ?: state.tool.textToolOptions.textColor,
                    ) {
                        selectedNode?.let { node ->
                            onIntent(
                                StudioIntent.Text.UpdateTextNode(
                                    node.copy(style = node.style.copy(mainColorHex = it.toArgb())),
                                ),
                            )
                        }
                            ?: onIntent(
                                StudioIntent.Text.SetTextToolOptions(state.tool.textToolOptions.copy(textColor = it)),
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
            .width(220.dp)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                "Select Font",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
            modifier = Modifier.padding(12.dp),
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
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
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
