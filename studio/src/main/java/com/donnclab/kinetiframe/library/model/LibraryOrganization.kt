package com.donnclab.kinetiframe.library.model

import kotlinx.serialization.Serializable

@Serializable enum class AssetCategory(val label: String) {
    CHARACTERS("Characters"),
    EXPRESSIONS("Expressions"),
    GESTURES("Gestures"),
    SCENES("Scenes"),
    PROPS("Props"),
    AUDIO("Audio"),
    COLOURS("Colours"),
}

@Serializable enum class BrowseMode(val label: String) {
    RECENT("Recent"),
    FAVOURITES("Favourites"),
    COLLECTIONS("Collections"),
    ALL("All"),
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
