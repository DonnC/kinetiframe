package com.donnclab.kinetiframe.library.ui

/** Captured assets are inserted via a project; working documents have editor destinations. */
sealed interface LibraryOpenTarget {
    data class Project(val id: String) : LibraryOpenTarget
    data class Actor(val id: String) : LibraryOpenTarget
}
fun libraryOpenTarget(assetId: String): LibraryOpenTarget? {
    val parts = assetId.split(':')
    return when {
        parts.size == 2 && parts[1].isNotBlank() && parts[0] == "project" -> LibraryOpenTarget.Project(parts[1])

        parts.size == 2 && parts[1].isNotBlank() && parts[0] == "actor" -> LibraryOpenTarget.Actor(parts[1])

        parts.size == 3 && parts[1].isNotBlank() && parts[2].isNotBlank() && parts[0] == "performance" ->
            LibraryOpenTarget.Project(parts[1])

        else -> null
    }
}
