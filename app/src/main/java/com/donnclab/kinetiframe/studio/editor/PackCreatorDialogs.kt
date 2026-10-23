// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.runtime.Composable
import com.donnclab.kinetiframe.library.ui.packs.PackCreatorConfig
import com.donnclab.kinetiframe.library.ui.packs.PackFrameTag
import com.donnclab.kinetiframe.library.ui.packs.PackItemDialog
import com.donnclab.kinetiframe.library.ui.packs.PackSaveTagsDialog
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

private const val MAX_PACK_ITEMS = 1000

/** Pack Creator's dialogs for naming and tagging an item, and for confirming tags before a save. */
@Composable
internal fun PackCreatorDialogs(
    config: PackCreatorConfig,
    state: StudioState,
    ui: EditorUiState,
    onIntent: (StudioIntent) -> Unit,
) {
    val panels = ui.panels
    panels.packItemDetails?.let { (index, creating) ->
        PackItemDialog(
            if (creating) PackFrameTag() else state.packTags[index] ?: PackFrameTag(),
            creating,
            onDismiss = { panels.packItemDetails = null },
            onSave = { tag ->
                if (creating && state.animation.totalFrames >= MAX_PACK_ITEMS) {
                    ui.messages.timelineError = "Maximum item limit reached"
                } else if (creating) {
                    onIntent(StudioIntent.PackItem.Add(index, tag))
                } else {
                    onIntent(StudioIntent.PackItem.SetTag(index, tag))
                }
                panels.packItemDetails = null
            },
        )
    }
    ui.exit.pendingPackSaveFrames?.let { count ->
        PackSaveTagsDialog(
            count,
            state.packTags,
            state.animation.currentFrame,
            onDismiss = { ui.exit.pendingPackSaveFrames = null },
            onSave = { tags ->
                config.onReplaceTags(tags)
                ui.exit.pendingPackSaveFrames = null
                config.onSavePack(count)
            },
        )
    }
}
