package com.donnclab.kinetiframe.studio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.ui.dock.GridOptionsContent
import com.donnclab.kinetiframe.studio.ui.timeline.LayerPanel
import com.donnclab.kinetiframe.studio.ui.timeline.OnionOptionsContent

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
            modifier = Modifier.width(340.dp).height(330.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, EditorTokens.border.copy(alpha = 0.2f)),
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
            modifier = Modifier.width(260.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, EditorTokens.border.copy(alpha = 0.2f)),
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
            modifier = Modifier.width(280.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, EditorTokens.border.copy(alpha = 0.2f)),
        ) {
            OnionOptionsContent(
                state = state,
                onIntent = onIntent,
                onDismiss = onDismiss,
            )
        }
    }
}
