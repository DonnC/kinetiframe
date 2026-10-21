package com.donnclab.kinetiframe.data.actor

import java.io.File

/** One canonical policy for document-controlled relative paths. */
internal object ActorPathPolicy {
    fun resolveWithin(root: File, relativePath: String, label: String): File {
        require(relativePath.isNotBlank()) { "$label path is blank" }
        val canonicalRoot = root.canonicalFile
        val candidate = File(canonicalRoot, relativePath).canonicalFile
        require(candidate.toPath().startsWith(canonicalRoot.toPath()) && candidate != canonicalRoot) {
            "$label path escapes its root"
        }
        return candidate
    }
}
