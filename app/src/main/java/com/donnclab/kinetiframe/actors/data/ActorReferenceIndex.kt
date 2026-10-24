package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import kotlinx.serialization.json.Json
import java.io.File

/** Read-only reference scan used to make actor archive/delete decisions safe. */
class ActorReferenceIndex(
    private val files: DocumentFileStore,
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    fun isReferenced(actorId: String): Boolean = runCatching {
        File(files.projectsRoot(), "").walkTopDown().filter {
            it.extension == "json" &&
                it.parentFile?.name == "performances"
        }
            .any { json.decodeFromString<PerformanceDocument>(it.readText()).validate().actorId == actorId } ||
            files.libraryRoot().walkTopDown().filter { it.name == "manifest.json" }.any {
                json.decodeFromString<LibraryItemManifest>(it.readText()).validate().sourceActorId == actorId
            }
    }.getOrDefault(true)
}
