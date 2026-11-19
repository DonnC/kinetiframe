package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.rig.presets.ActorPreset
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens

/** The presets on offer, the one being previewed, and what picking or applying one does. */
data class PresetPicker(
    val presets: List<ActorPreset>,
    val previewingId: String?,
    val canApply: Boolean,
    val onPreview: (String) -> Unit,
    val onApply: () -> Unit,
)

/**
 * Performance Stage's Preset Library, reached on demand from the top bar rather than sitting
 * permanently in the canvas -- the design's whole point is that Rig Fabricator's sketch/rig
 * screen stays uncluttered, and this browsing UI only ever mattered once there was a posed rig to
 * retarget it onto anyway (see [PresetLibrarySection]'s own "Switch to Animate mode" hint, from
 * back when it lived inline).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetLibrarySheet(picker: PresetPicker, audition: PresetAudition, onDismiss: () -> Unit) {
    val (presets, previewingId, canApply, onPreview, onApply) = picker
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ActorStudioTokens.panelSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        androidx.compose.foundation.layout.Column {
            PresetLibrarySection(
                presets = presets,
                previewingId = previewingId,
                inPerformanceMode = true,
                onPreview = onPreview,
                onApply = onApply,
                canApply = canApply,
                modifier = Modifier.padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceXs),
            )
            PresetAuditionControls(
                audition,
                Modifier.padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceXs),
            )
        }
    }
}
