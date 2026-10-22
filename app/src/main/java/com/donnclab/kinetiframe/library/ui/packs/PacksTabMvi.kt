package com.donnclab.kinetiframe.library.ui.packs

import com.donnclab.kinetiframe.actors.data.LibraryItemManifest
import com.donnclab.kinetiframe.library.data.AssetOrganization
import com.donnclab.kinetiframe.library.data.BrowserPosition
import com.donnclab.kinetiframe.library.data.LibraryOrganization
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackItemEntry
import com.donnclab.kinetiframe.library.data.PackManifest
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import java.io.File

/** Home's Packs tab: the user's PERSONAL PACK library items. Each pack additionally carries
 *  its item count and up to four preview entries (for the card's 2x2 collage thumbnail), both read
 *  from the pack's own [PackManifest] via
 *  [LibraryRepository.loadPack].
 *
 *  [openDetails]/[detailsItems] back the pack drill-down screen (tap a card to see every drawing
 *  inside it, full size, with its real name) -- a separate, on-demand load from [previewItems]
 *  because a pack's full item list (as opposed to just its first four, cached for every card's
 *  collage) is only ever needed for the one pack currently open. */
data class PacksTabState(
    val loading: Boolean = true,
    val packs: List<LibraryItemManifest> = emptyList(),
    val itemCounts: Map<String, Int> = emptyMap(),
    val previewItems: Map<String, List<PackItemEntry>> = emptyMap(),
    val itemNames: Map<String, List<String>> = emptyMap(),
    val organization: LibraryOrganization = LibraryOrganization(),
    val browser: BrowserPosition = BrowserPosition(),
    val error: String? = null,
    val openDetails: LibraryItemManifest? = null,
    val detailsItems: List<PackItemEntry> = emptyList(),
    val detailsLoading: Boolean = false,
) : ViewState

sealed interface PacksTabIntent : ViewIntent {
    data object Reload : PacksTabIntent
    data class Delete(val libraryItemId: String) : PacksTabIntent
    data class OpenDetails(val pack: LibraryItemManifest) : PacksTabIntent
    data object CloseDetails : PacksTabIntent
    data class Browse(val position: BrowserPosition) : PacksTabIntent
    data class ToggleFavourite(val pack: LibraryItemManifest) : PacksTabIntent
    data class Organise(val pack: LibraryItemManifest, val value: AssetOrganization) : PacksTabIntent
    data class Used(val pack: LibraryItemManifest) : PacksTabIntent
}

sealed interface PacksTabEffect : ViewEffect {
    data class ShowError(val message: String) : PacksTabEffect
}

/**
 * Everything Home's Packs tab needs to call outward, grouped so the tab composable keeps a
 * 4-parameter signature. [detailsPreviewFile] resolves one pack drawing's on-disk path on demand
 * (pack id + entry) rather than eagerly materialising a file list, so opening the drill-down never
 * copies or preloads a pack's PNGs.
 */
data class PacksTabCallbacks(
    val onIntent: (PacksTabIntent) -> Unit,
    val onShare: (LibraryItemManifest) -> Unit,
    val onRequestCreate: () -> Unit,
    val onEdit: (LibraryItemManifest) -> Unit,
    val detailsPreviewFile: (String, PackItemEntry) -> File,
)
