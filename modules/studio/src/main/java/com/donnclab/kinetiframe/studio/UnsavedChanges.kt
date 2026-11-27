// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.core.document.ProjectSaveDocument

/**
 * Tells whether the open project differs from what was last saved, so leaving only asks to save
 * when there is something to lose.
 *
 * It compares two things with the last save: the saved document's structure (layers, frames, holds,
 * scene, tracks) and a count of pixel writes. Any doubt counts as a change: undoing back to the saved
 * drawing still counts, and a session that hasn't been marked saved always has changes.
 */
class UnsavedChanges(private val snapshot: () -> ProjectSaveDocument, private val pixelEdits: () -> Long) {
    private data class Mark(val document: ProjectSaveDocument, val pixelEdits: Long)

    @Volatile private var saved: Mark? = null

    @Volatile private var prepared: Mark? = null

    /** The open project matches its saved copy, for example just after opening it. */
    fun markSaved() {
        saved = Mark(snapshot().comparable(), pixelEdits())
    }

    /** Records the state a save is about to write. Call after pending edits have finished. */
    fun preparing(document: ProjectSaveDocument) {
        prepared = Mark(document.comparable(), pixelEdits())
    }

    /** The save of [document] succeeded. */
    fun saved(document: ProjectSaveDocument) {
        prepared?.takeIf { it.document == document.comparable() }?.let { saved = it }
    }

    fun hasChanges(): Boolean {
        val mark = saved ?: return true
        return mark.pixelEdits != pixelEdits() || mark.document != snapshot().comparable()
    }

    // The active layer and the drawing density are view settings, not work to lose.
    // Density is compared at a fixed valid value; a document rejects zero.
    private fun ProjectSaveDocument.comparable() = copy(activeLayerIndex = 0, project = project.copy(nodeDensity = 1f))
}
