package com.donnclab.kinetiframe.ui.components.timeline

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.ui.theme.DenseTextField

@Composable
fun LayerItem(
    layer: Layer,
    isActive: Boolean,
    preview: Bitmap?,
    editing: Boolean,
    onEdit: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onVisibilityClick: () -> Unit = {},
    onOpacityChange: (Float) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(EditorTokens.row)
            .background(if (isActive) EditorTokens.accent.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // [Drag Handle]
        Icon(
            Lucide.GripVertical,
            contentDescription = null,
            tint = EditorTokens.inactive,
            modifier = dragHandleModifier
                .size(EditorTokens.icon)
                .padding(end = 4.dp),
        )

        // [Eye Icon (only shows when hidden)]
        Box(modifier = Modifier.width(EditorTokens.iconSmall)) {
            if (!layer.isVisible) {
                Icon(
                    Lucide.EyeOff,
                    contentDescription = "Hidden",
                    tint = EditorTokens.accent,
                    modifier = Modifier
                        .size(EditorTokens.iconSmall)
                        .clickable { onVisibilityClick() },
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // [Preview]
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            // Keep transparent artwork readable instead of rendering as a black
            // thumbnail on devices whose bitmap surface has no background.
            val previewLight = MaterialTheme.colorScheme.surfaceVariant
            val previewDark = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val cell = 8.dp.toPx()
                var y = 0f
                var row = 0
                while (y < size.height) {
                    var x = 0f
                    var column = 0
                    while (x < size.width) {
                        drawRect(
                            color = if ((row + column) % 2 == 0) previewLight else previewDark,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(cell, cell),
                        )
                        x += cell
                        column++
                    }
                    y += cell
                    row++
                }
            }
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // [Layer Name]
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (!editing) {
                        Modifier.clickable {
                            onClick()
                            onEdit()
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (editing) {
                var localName by remember(layer.name) { mutableStateOf(layer.name) }
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                var didFocus by remember { mutableStateOf(false) }

                LaunchedEffect(editing) {
                    if (editing) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }

                DenseTextField(
                    value = localName,
                    onValueChange = { localName = it },
                    onDone = {
                        onRename(localName)
                        keyboardController?.hide()
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                didFocus = true
                            } else if (didFocus && editing) {
                                onRename(localName)
                            }
                        },
                )
            } else {
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else EditorTokens.inactive,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // [Opacity Value]
        OpacityDrag(
            opacity = layer.opacity,
            onOpacityChange = onOpacityChange,
            onVisibilityToggle = onVisibilityClick,
            isVisible = layer.isVisible,
        )
    }
}
