package com.donnclab.kinetiframe.presentation.studio.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.ContextIconButton
import com.donnclab.kinetiframe.presentation.studio.color.LocalScreenColorRequest
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

/**
 * A compact color preview that opens a full HSV picker in a popup.
 */
@Composable
fun ColorPicker(color: Color, onColorChanged: (Color) -> Unit) {
    var showPopup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, Color(0xFFE0E0E0), CircleShape)
            .clickable { showPopup = true },
    )

    if (showPopup) {
        Popup(
            onDismissRequest = { showPopup = false },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier = Modifier.padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 12.dp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
            ) {
                ColorPickerPanel(
                    color = color,
                    onColorChanged = onColorChanged,
                    onDismiss = { showPopup = false },
                )
            }
        }
    }
}

/** Reusable HSV picker panel for tool popups and color settings. */
@Composable
fun ColorPickerPanel(color: Color, onColorChanged: (Color) -> Unit, onDismiss: () -> Unit = {}) {
    var palettes by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val controller = androidx.compose.runtime.key(palettes) { rememberColorPickerController() }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val pickerSize = if (isLandscape) 120.dp else 160.dp
    val screenPicker = LocalScreenColorRequest.current
    val windowHeightDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp().value }

    Column(
        modifier = Modifier.heightIn(max = (windowHeightDp - 32).coerceAtLeast(100f).dp)
            .verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row {
            ContextIconButton(
                ContextIconAction(Icons.Filled.Colorize, "Custom colour", {
                    palettes = false
                }, selected = !palettes),
            )
            ContextIconButton(
                ContextIconAction(Icons.Filled.Palette, "Palettes", {
                    palettes = true
                }, selected = palettes),
            )
        }
        if (palettes) {
            com.donnclab.kinetiframe.presentation.studio.color.PaletteBrowser(color, onColorChanged)
        } else {
            HsvColorPicker(
                modifier = Modifier.size(pickerSize).clip(RoundedCornerShape(8.dp)),
                controller = controller,
                initialColor = color,
                onColorChanged = { envelope: ColorEnvelope -> if (envelope.fromUser) onColorChanged(envelope.color) },
            )
            AlphaSlider(
                modifier = Modifier.width(pickerSize).height(16.dp),
                controller = controller,
            )
            BrightnessSlider(
                modifier = Modifier.width(pickerSize).height(16.dp),
                controller = controller,
            )
        }
        Row {
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(color))
            screenPicker?.let { picker ->
                ContextIconButton(
                    ContextIconAction(Icons.Filled.Colorize, "Pick color from screen", {
                        onDismiss()
                        picker.begin(onColorChanged)
                    }),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 180, heightDp = 240)
@Composable
private fun ColorPickerPreview() {
    ColorPicker(color = Color(0xFFE3B94F), onColorChanged = {})
}
