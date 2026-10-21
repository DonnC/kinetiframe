package com.donnclab.kinetiframe.data.actor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import java.io.File

/** Persists revision artwork by stable texture asset ID; only bound parts are stored. */
class ActorArtworkStore(private val files: ActorFileStore) {
    fun save(actorId: String, revisionId: String, graph: SceneGraph, runtime: RigAssetManager): Map<String, String> {
        ActorDocumentValidation.requireId(revisionId, "revision ID")
        val directory = File(files.actorDirectory(actorId), "revisions/$revisionId/artwork")
        return com.donnclab.kinetiframe.engine.rigging.model.ActorArtwork.bindings(graph).map { (partId, binding) ->
            val assetId = binding.assetId
            ActorDocumentValidation.requireId(assetId, "artwork asset ID")
            val bitmap =
                requireNotNull(runtime.getAsset(assetId)?.takeUnless(Bitmap::isRecycled)) {
                    "Actor artwork is missing for $partId"
                }
            val target = File(directory, "$assetId.png")
            com.donnclab.kinetiframe.data.storage.DurableFile.write(target) { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Could not save actor artwork" }
            }
            partId to target.relativeTo(files.actorDirectory(actorId)).invariantSeparatorsPath
        }.toMap()
    }

    fun restore(
        actorId: String,
        artwork: Map<String, String>,
        graph: SceneGraph,
        runtime: RigAssetManager,
    ): List<String> {
        val bindings = com.donnclab.kinetiframe.engine.rigging.model.ActorArtwork.bindings(graph)
        return artwork.mapNotNull { (partId, relativePath) ->
            val assetId = bindings[partId]?.assetId ?: return@mapNotNull partId
            val source = runCatching {
                ActorPathPolicy.resolveWithin(files.actorDirectory(actorId), relativePath, "Artwork")
            }.getOrNull() ?: return@mapNotNull partId
            val bitmap = BitmapFactory.decodeFile(source.path) ?: return@mapNotNull partId
            runtime.registerAsset(assetId, bitmap)
            null
        }
    }
}
