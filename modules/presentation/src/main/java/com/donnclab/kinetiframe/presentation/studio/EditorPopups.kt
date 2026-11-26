package com.donnclab.kinetiframe.presentation.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.studio.dock.GridOptionsContent
import com.donnclab.kinetiframe.presentation.studio.timeline.LayerPanel
import com.donnclab.kinetiframe.presentation.studio.timeline.OnionOptionsContent
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.render.FrameCacheManager

@Composable
fun LayerPanelPopup(
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    cacheManager: FrameCacheManager?,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(-16, -120),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(EditorTokens.layerPopupWidth).heightIn(max = EditorTokens.popupHeight),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Dimens.radiusM),
            tonalElevation = Dimens.elevationRaised,
            border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.2f)),
        ) {
            LayerPanel(
                state = state,
                onIntent = onIntent,
                cacheManager = cacheManager,
                onLayerClick = { onDismiss() },
            )
        }
    }
}

@Composable
fun GridOptionsPopup(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(-16, 56),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(EditorTokens.popupNarrowWidth),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Dimens.radiusL),
            shadowElevation = Dimens.elevationM,
            border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.2f)),
        ) {
            GridOptionsContent(
                state = state,
                onIntent = onIntent,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
fun OnionOptionsPopup(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(-16, 56),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(EditorTokens.popupMediumWidth),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Dimens.radiusL),
            shadowElevation = Dimens.elevationM,
            border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.2f)),
        ) {
            OnionOptionsContent(
                state = state,
                onIntent = onIntent,
                onDismiss = onDismiss,
            )
        }
    }
}
