package com.donnclab.kinetiframe.presentation.library

import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.library.model.BrowserPosition
import com.donnclab.kinetiframe.library.model.LibraryOrganization
import com.donnclab.kinetiframe.presentation.common.AssetDetails
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.common.AudioTrackSummary
import com.donnclab.kinetiframe.presentation.common.StampAsset
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState

/**
 * Disk-backed Library projection. Standalone browsing has no active editing destination;
 * insertion remains in the scoped From Library picker inside the editor.
 */
data class LibraryState(
    val loading: Boolean = true,
    val selectedTab: Int = 0,
    val selectedFilter: Int = 0,
    val projects: List<AssetSummary> = emptyList(),
    val actors: List<AssetSummary> = emptyList(),
    val packs: List<AssetSummary> = emptyList(),
    val packManifests: Map<String, LibraryItemManifest> = emptyMap(),
    val packItemNames: Map<String, List<String>> = emptyMap(),
    val organization: LibraryOrganization = LibraryOrganization(),
    val packBrowser: BrowserPosition = BrowserPosition(),
    val audio: List<AudioTrackSummary> = emptyList(),
    val hasOpenProject: Boolean = false,
    val query: String = "",
    val packContents: List<StampAsset> = emptyList(),
    val openDetails: AssetDetails? = null,
    val openPackContents: AssetSummary? = null,
    val packLoading: Boolean = false,
    val packError: String? = null,
) : ViewState

sealed interface LibraryIntent : ViewIntent {
    data object Reload : LibraryIntent
    data class Search(val query: String) : LibraryIntent
    data class SelectTab(val index: Int) : LibraryIntent
    data class SelectFilter(val index: Int) : LibraryIntent
    data class BrowsePacks(val position: BrowserPosition) : LibraryIntent
    data class OpenDetails(val details: AssetDetails) : LibraryIntent
    data object DismissDetails : LibraryIntent
    data class OpenPackContents(val pack: AssetSummary) : LibraryIntent
    data object ClosePackContents : LibraryIntent
    data object EditOpenPack : LibraryIntent
    data class AddToCurrentProject(val assetId: String) : LibraryIntent
}

sealed interface LibraryEffect : ViewEffect {
    data class ShowMessage(val message: String) : LibraryEffect
    data class OpenDocument(val target: LibraryOpenTarget) : LibraryEffect
    data class EditPack(val item: LibraryItemManifest) : LibraryEffect
}
