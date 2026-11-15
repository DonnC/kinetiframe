// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.library

import androidx.annotation.StringRes
import com.donnclab.kinetiframe.library.model.AssetCategory
import com.donnclab.kinetiframe.library.model.BrowseMode
import com.donnclab.kinetiframe.presentation.R

@StringRes
internal fun AssetCategory.labelRes(): Int = when (this) {
    AssetCategory.CHARACTERS -> R.string.library_category_characters
    AssetCategory.EXPRESSIONS -> R.string.library_category_expressions
    AssetCategory.GESTURES -> R.string.library_category_gestures
    AssetCategory.SCENES -> R.string.library_category_scenes
    AssetCategory.PROPS -> R.string.library_category_props
    AssetCategory.AUDIO -> R.string.library_category_audio
    AssetCategory.COLOURS -> R.string.library_category_colours
}

@StringRes
internal fun BrowseMode.labelRes(): Int = when (this) {
    BrowseMode.RECENT -> R.string.library_browse_recent
    BrowseMode.FAVOURITES -> R.string.library_browse_favourites
    BrowseMode.COLLECTIONS -> R.string.library_browse_collections
    BrowseMode.ALL -> R.string.filter_all
}
