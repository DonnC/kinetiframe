package com.donnclab.kinetiframe.actors.rig.texture

import android.graphics.Bitmap
import java.util.UUID

/** Owns extracted bitmap assets so SceneGraph remains lightweight and immutable. */
class RigAssetManager {
    private val assets = mutableMapOf<String, Bitmap>()

    @Synchronized
    fun registerAsset(bitmap: Bitmap): String {
        require(!bitmap.isRecycled) { "Cannot register a recycled bitmap." }
        val id = UUID.randomUUID().toString()
        assets[id] = bitmap
        return id
    }

    /** Restores a persisted texture under the ID already referenced by a saved scene graph. */
    @Synchronized
    fun registerAsset(assetId: String, bitmap: Bitmap) {
        require(assetId.isNotBlank()) { "Asset ID cannot be blank" }
        require(!bitmap.isRecycled) { "Cannot register a recycled bitmap." }
        assets.remove(assetId)?.let { previous -> if (previous !== bitmap && !previous.isRecycled) previous.recycle() }
        assets[assetId] = bitmap
    }

    @Synchronized
    fun getAsset(assetId: String): Bitmap? = assets[assetId]

    @Synchronized
    fun contains(assetId: String): Boolean = assets.containsKey(assetId)

    /**
     * Immediately recycles [assetId]. Callers must be certain nothing else — in particular no
     * undo/redo snapshot — can still reference it. That only holds for temporary, pre-checkpoint
     * registrations: an in-progress lasso extraction that gets cancelled or replaced before the
     * user ever confirms it. Anything that could already be reachable from authoring history
     * (a bound, confirmed texture) must go through [retainOnly] instead so a snapshot that still
     * needs the bitmap is never left pointing at a recycled one. See
     * `ActorStudioManager.reconcileRuntimeAssets`.
     */
    @Synchronized
    fun releaseAsset(assetId: String) {
        assets.remove(assetId)?.let { if (!it.isRecycled) it.recycle() }
    }

    /**
     * Mark-and-sweep bitmap collection: recycles every registered asset whose ID is not in
     * [reachableAssetIds] and leaves everything else untouched.
     *
     * This exists because a document mutation (detach, rebind) can drop the *live* project's
     * only reference to a texture while an undo snapshot still needs it. Recycling on the spot,
     * as a naive reference count would, leaves that snapshot pointing at a recycled bitmap the
     * moment the user hits Undo. Instead, the caller recomputes the full reachable set — the
     * live document plus every undo and redo snapshot — after each such mutation, and this sweep
     * only ever discards bitmaps nothing can reach any more.
     */
    @Synchronized
    fun retainOnly(reachableAssetIds: Set<String>) {
        val unreachable = assets.keys - reachableAssetIds
        unreachable.forEach { id -> assets.remove(id)?.let { if (!it.isRecycled) it.recycle() } }
    }

    @Synchronized
    fun clear() {
        assets.values.forEach { if (!it.isRecycled) it.recycle() }
        assets.clear()
    }

    @Synchronized
    fun size(): Int = assets.size

    /** Includes undo-reachable artwork; refuse new imports rather than evicting live textures. */
    @Synchronized
    fun allocatedBytes(): Long = assets.values.filterNot { it.isRecycled }.sumOf { it.allocationByteCount.toLong() }
}
