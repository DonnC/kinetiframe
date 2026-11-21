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

/**
 * The collection items belong to until someone moves them. The name is saved in organization files
 * and compared as a key, so it stays in English; screens show a translated label for it.
 */
const val DEFAULT_COLLECTION = "General Essentials"

@Serializable data class AssetOrganization(
    val collection: String = DEFAULT_COLLECTION,
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
