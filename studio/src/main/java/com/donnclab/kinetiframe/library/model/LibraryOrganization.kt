package com.donnclab.kinetiframe.library.model

import kotlinx.serialization.Serializable

// The names below are saved. Screens name each value from their own string resources.

@Serializable enum class AssetCategory {
    CHARACTERS,
    EXPRESSIONS,
    GESTURES,
    SCENES,
    PROPS,
    AUDIO,
    COLOURS,
}

@Serializable enum class BrowseMode {
    RECENT,
    FAVOURITES,
    COLLECTIONS,
    ALL,
}

@Serializable data class AssetOrganization(
    val collection: String = "General Essentials",
    val category: AssetCategory? = null,
    val favourite: Boolean = false,
    val lastUsedEpochMs: Long = 0,
    val displayName: String? = null,
)

@Serializable data class BrowserPosition(
    val mode: BrowseMode = BrowseMode.RECENT,
    val collection: String? = null,
    val category: AssetCategory? = null,
)

@Serializable data class LibraryOrganization(
    val items: Map<String, AssetOrganization> = emptyMap(),
    val browsers: Map<String, BrowserPosition> = emptyMap(),
)
