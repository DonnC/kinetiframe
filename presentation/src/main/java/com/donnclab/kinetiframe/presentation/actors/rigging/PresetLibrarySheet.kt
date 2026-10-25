package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens

/**
 * Performance Stage's Preset Library, reached on demand from the top bar rather than sitting
 * permanently in the canvas -- the design's whole point is that Rig Fabricator's sketch/rig
 * screen stays uncluttered, and this browsing UI only ever mattered once there was a posed rig to
 * retarget it onto anyway (see [PresetLibrarySection]'s own "Switch to Animate mode" hint, from
 * back when it lived inline).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetLibrarySheet(
    presets: List<ActorMotionPreset>,
    previewingId: String?,
    onPreview: (String) -> Unit,
    onApply: () -> Unit,
    canApply: Boolean,
    preview: com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset?,
    playing: Boolean,
    replacesKeys: Boolean,
    onPlay: () -> Unit,
    onIntensity: (Float) -> Unit,
    onSpeed: (Float) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
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
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            PresetAuditionControls(
                preview,
                playing,
                replacesKeys,
                onPlay,
                onIntensity,
                onSpeed,
                onRemove,
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}
