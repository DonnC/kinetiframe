package com.donnclab.kinetiframe.data.project

import com.donnclab.kinetiframe.data.actor.ActorRevision
import kotlinx.serialization.json.Json
import java.io.File

/** Revision identity is validated data, not whitespace/default-field formatting. Native starter
 * upgrades and imported originals can serialize the same revision differently. Artwork still
 * requires exact bytes; neither matching revision JSON nor assets are overwritten on import. */
internal object ActorDependencyMatch {
    private val json = Json { ignoreUnknownKeys = false }
    fun equivalentRevision(relative: String, first: File, second: File): Boolean {
        if (!relative.startsWith("revisions/") || relative.count { it == '/' } != 1 ||
            !relative.endsWith(".json")
        ) {
            return false
        }
        return runCatching {
            json.decodeFromString<ActorRevision>(first.readText()).validate() ==
                json.decodeFromString<ActorRevision>(second.readText()).validate()
        }.getOrDefault(false)
    }
}
