package com.donnclab.kinetiframe.actors.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ActorPlacementRepository(
    private val files: ActorFileStore,
    private val json: Json = Json {
        ignoreUnknownKeys =
            false
        prettyPrint = true
    },
) {
    fun save(placement: ActorPlacement): ActorStoreResult<ActorPlacement> = runCatching {
        placement.validate()
        files.writeAtomically(
            File(files.projectDirectory(placement.projectId), "placements/${placement.placementId}.json"),
            json.encodeToString(placement),
        )
        placement
    }.fold(::value, ::failure)

    fun list(projectId: String): ActorStoreResult<List<ActorPlacement>> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID")
        File(files.projectDirectory(projectId), "placements").listFiles().orEmpty().filter { it.extension == "json" }
            .map {
                json.decodeFromString<ActorPlacement>(it.readText()).validate()
            }.sortedWith(compareBy(ActorPlacement::startFrame, ActorPlacement::zIndex))
    }.fold(::value, ::failure)

    /** Recoverable removal; never permanently deletes a project placement document. */
    fun archive(projectId: String, placementId: String): ActorStoreResult<Unit> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID")
        ActorDocumentValidation.requireId(placementId, "placement ID")
        val source = File(files.projectDirectory(projectId), "placements/$placementId.json")
        require(source.isFile) { "Placement not found" }
        files.move(source, File(files.projectDirectory(projectId), "archive/placements/$placementId.json"))
    }.fold(::value, ::failure)

    private fun <T> value(value: T): ActorStoreResult<T> = ActorStoreResult.Value(value)
    private fun failure(error: Throwable): ActorStoreResult.Failure =
        ActorStoreResult.Failure(error.message ?: "Placement storage failed", error)
}
