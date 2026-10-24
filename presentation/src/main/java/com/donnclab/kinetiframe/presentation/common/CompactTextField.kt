package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** One compact field for forms, names, search and timeline values; font scaling can grow its height. */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (label != null) ProvideTextStyle(MaterialTheme.typography.labelSmall) { label() }
        BasicTextField(
            value, onValueChange, enabled = enabled, singleLine = singleLine,
            minLines = minLines, maxLines = maxLines, interactionSource = interaction,
            textStyle = textStyle.copy(color = colors.onSurface.copy(alpha = if (enabled) 1f else 0.38f)),
            cursorBrush = SolidColor(
                colors.primary,
            ),
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(), decorationBox = { inner ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = colors.surface,
                    border = BorderStroke(
                        1.dp,
                        when {
                            isError -> colors.error
                            focused -> colors.primary
                            else -> colors.outlineVariant
                        },
                    ),
                ) {
                    Row(
                        Modifier.heightIn(min = 36.dp).padding(contentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (leadingIcon !=
                            null
                        ) {
                            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { leadingIcon() }
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() &&
                                placeholder != null
                            ) {
                                CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                                    ProvideTextStyle(textStyle) { placeholder() }
                                }
                            }
                            inner()
                        }
                        if (suffix != null) ProvideTextStyle(MaterialTheme.typography.labelSmall) { suffix() }
                        if (trailingIcon != null) trailingIcon()
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompactFieldPreview() {
    KinetiFrameTheme { CompactTextField("Tavi", {}, label = { Text("Name") }, singleLine = true) }
}
