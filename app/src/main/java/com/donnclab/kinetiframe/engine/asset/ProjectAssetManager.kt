package com.donnclab.kinetiframe.engine.asset

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns decoded bitmaps for project-level imported assets. Today that's exactly one slot -- the
 * persistent background image set via [com.donnclab.kinetiframe.domain.manager.BackgroundManager] -- but
 * this is where a general imported-asset library (background images, reference photos, other
 * gallery imports) belongs once that module exists, rather than bolted onto rig-specific code.
 *
 * Deliberately separate from [com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager], which owns
 * actor rig textures: different lifecycle (a rig texture is bound to a scene graph and swept by
 * reachability across undo/redo snapshots; a project asset today is a single named slot with no
 * such history to reconcile), different owner, and mixing the two would make the eventual asset
 * library's home ambiguous. Kept intentionally simpler than RigAssetManager for the same reason --
 * no mark-and-sweep here yet, because there is only ever the one slot to reconcile. Extend this
 * (not RigAssetManager) when a real multi-asset library needs it.
 */
class ProjectAssetManager {
    private val assets = mutableMapOf<String, Bitmap>()
    private val changes = MutableStateFlow(0L)
    val revision = changes.asStateFlow()

    /** Registers [bitmap] under a caller-chosen [assetId] (mirrors the "restore a persisted asset
     *  under its already-known ID" overload on RigAssetManager -- a project asset's ID is always
     *  assigned by [com.donnclab.kinetiframe.domain.manager.BackgroundManager], never generated here). */
    @Synchronized
    fun registerAsset(assetId: String, bitmap: Bitmap) {
        require(assetId.isNotBlank()) { "Asset ID cannot be blank" }
        require(!bitmap.isRecycled) { "Cannot register a recycled bitmap." }
        assets[assetId] = bitmap
        changes.value++
    }

    /** Null-safe on a null ID so call sites can pass `project.backgroundAssetId` (an optional
     *  reference) straight through without an extra null check at every render site. */
    @Synchronized
    fun getAsset(assetId: String?): Bitmap? = assetId?.let(assets::get)

    /** Drop cache ownership, not pixels still referenced by Compose or Android's render thread.
     * Garbage collection can free the bitmap once those consumers also let it go. */
    @Synchronized
    fun releaseAsset(assetId: String) {
        if (assets.remove(assetId) != null) changes.value++
    }

    @Synchronized
    fun clear() {
        assets.clear()
        changes.value++
    }
}
