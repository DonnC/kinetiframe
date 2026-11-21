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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
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
            .size(EditorTokens.colorDot)
            .clip(CircleShape)
            .background(color)
            .border(Dimens.borderMedium, EditorTokens.hairline, CircleShape)
            .clickable { showPopup = true },
    )

    if (showPopup) {
        Popup(
            onDismissRequest = { showPopup = false },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier = Modifier.padding(Dimens.spaceS),
                shape = RoundedCornerShape(Dimens.radiusM),
                shadowElevation = Dimens.elevationL,
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(Dimens.borderThin, EditorTokens.hairline),
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
    val pickerSize = if (isLandscape) EditorTokens.colorWheelLandscape else EditorTokens.colorWheel
    val screenPicker = LocalScreenColorRequest.current
    val windowHeightDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp().value }

    Column(
        modifier = Modifier.heightIn(max = (windowHeightDp - 32).coerceAtLeast(100f).dp)
            .verticalScroll(rememberScrollState()).padding(Dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceS),
    ) {
        Row {
            ContextIconButton(
                ContextIconAction(Icons.Filled.Colorize, stringResource(R.string.color_custom), {
                    palettes = false
                }, selected = !palettes),
            )
            ContextIconButton(
                ContextIconAction(Icons.Filled.Palette, stringResource(R.string.color_palettes), {
                    palettes = true
                }, selected = palettes),
            )
        }
        if (palettes) {
            com.donnclab.kinetiframe.presentation.studio.color.PaletteBrowser(color, onColorChanged)
        } else {
            HsvColorPicker(
                modifier = Modifier.size(pickerSize).clip(RoundedCornerShape(Dimens.radiusS)),
                controller = controller,
                initialColor = color,
                onColorChanged = { envelope: ColorEnvelope -> if (envelope.fromUser) onColorChanged(envelope.color) },
            )
            AlphaSlider(
                modifier = Modifier.width(pickerSize).height(EditorTokens.colorSliderHeight),
                controller = controller,
            )
            BrightnessSlider(
                modifier = Modifier.width(pickerSize).height(EditorTokens.colorSliderHeight),
                controller = controller,
            )
        }
        Row {
            Box(
                Modifier.size(EditorTokens.swatch)
                    .clip(RoundedCornerShape(EditorTokens.swatchCorner))
                    .background(color),
            )
            screenPicker?.let { picker ->
                ContextIconButton(
                    ContextIconAction(Icons.Filled.Colorize, stringResource(R.string.color_pick_from_screen), {
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
