// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.library.data

import com.donnclab.kinetiframe.core.actor.document.LibraryScope

/**
 * Where a new library item is filed: the personal library or one project's, and the tags it
 * carries. [projectId] is required for [LibraryScope.PROJECT].
 */
data class LibraryPlacement(
    val scope: LibraryScope = LibraryScope.PERSONAL,
    val projectId: String? = null,
    val tags: Set<String> = emptySet(),
)
