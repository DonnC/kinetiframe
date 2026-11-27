// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.storage

import java.io.File

/** Resolves relative paths that come from documents, so none can point outside its root. */
object ContainedPaths {
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
