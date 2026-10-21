package com.donnclab.kinetiframe.ui.packs

import com.donnclab.kinetiframe.data.actor.LibraryItemManifest
import com.donnclab.kinetiframe.data.library.AssetCategory
import com.donnclab.kinetiframe.data.library.AssetOrganization
import com.donnclab.kinetiframe.data.library.BrowseMode
import com.donnclab.kinetiframe.data.library.BrowserPosition

/** Pure browsing/search policy shared by Home and the insertion picker. No raster decoding. */
object PackBrowserIndex {
    fun key(pack: LibraryItemManifest): String = if (pack.scope ==
        com.donnclab.kinetiframe.data.actor.LibraryScope.PERSONAL
    ) {
        pack.libraryItemId
    } else {
        "PROJECT:${pack.sourceProjectId}:${pack.libraryItemId}"
    }
    fun defaults(pack: LibraryItemManifest): AssetOrganization {
        val tags = pack.tags.map(String::lowercase)
        val words = (pack.displayName + " " + pack.libraryItemId + " " + tags.joinToString(" ")).lowercase()
        val category = when {
            pack.libraryItemId == "ls-studio-items-v3" -> AssetCategory.PROPS
            listOf("mouth", "eye", "brow", "expression", "face").any(words::contains) -> AssetCategory.EXPRESSIONS
            listOf("hand", "gesture", "pose").any(words::contains) -> AssetCategory.GESTURES
            listOf("room", "scene", "background", "studio").any(words::contains) -> AssetCategory.SCENES
            listOf("furniture", "prop", "desk", "material").any(words::contains) -> AssetCategory.PROPS
            listOf("body", "character", "actor", "torso").any(words::contains) -> AssetCategory.CHARACTERS
            else -> null
        }
        return AssetOrganization(
            collection = if (pack.libraryItemId.startsWith("ls-") || "loose-signal" in tags) {
                "Loose Signal"
            } else {
                "General Essentials"
            },
            category = category,
        )
    }

    fun filter(
        packs: List<LibraryItemManifest>,
        itemNames: Map<String, List<String>>,
        organization: Map<String, AssetOrganization>,
        position: BrowserPosition,
        query: String,
    ): List<LibraryItemManifest> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotEmpty)
        return packs.filter { pack ->
            val meta = organization[key(pack)] ?: defaults(pack)
            val search = (
                listOf(pack.displayName, meta.displayName.orEmpty()) + pack.tags +
                    itemNames[key(pack)].orEmpty()
                )
                .joinToString(" ").lowercase()
            (position.mode != BrowseMode.FAVOURITES || meta.favourite) &&
                (position.collection == null || meta.collection == position.collection) &&
                (position.category == null || meta.category == position.category) &&
                terms.all(search::contains)
        }.sortedWith(
            compareByDescending<LibraryItemManifest> {
                if (position.mode == BrowseMode.RECENT) organization[key(it)]?.lastUsedEpochMs ?: 0L else 0L
            }.thenByDescending { if (position.mode == BrowseMode.RECENT) it.createdAtEpochMs else 0L }
                .thenBy { (organization[key(it)]?.displayName ?: it.displayName).lowercase() },
        )
    }
}
