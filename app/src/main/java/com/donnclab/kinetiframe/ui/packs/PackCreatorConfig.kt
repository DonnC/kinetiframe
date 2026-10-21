package com.donnclab.kinetiframe.ui.packs

/** One frame's own tag inside a pack draft -- a required name and optional short description,
 *  reviewed in the metadata dialog when saving. */
data class PackFrameTag(val name: String = "", val description: String = "") {
    val isValid: Boolean get() = name.isNotBlank() &&
        name.length <= com.donnclab.kinetiframe.data.library.PackItemEntry.MAX_NAME_LENGTH &&
        description.length <= com.donnclab.kinetiframe.data.library.PackItemEntry.MAX_DESCRIPTION_LENGTH
}

/**
 * Turns an ordinary [com.donnclab.kinetiframe.ui.EditorScreen] into the Pack Creator micro-editor: passing
 * a non-null instance hides Play/Loop/FPS (see
 * [com.donnclab.kinetiframe.ui.components.timeline.TimelineControls]'s `packMode`), overlays the crosshair
 * anchor, reviews per-frame tags at save time, and swaps "Save Project" out of scope for a
 * dedicated "Save Pack" action. This is intentionally the *only* thing EditorScreen needs to know
 * about packs -- persistence (writing the draft, reading it back, turning tags + frame bitmaps
 * into a real [com.donnclab.kinetiframe.data.library.PackManifest]) all lives in `PackCreatorRoute`, which
 * is the thing that actually builds this config.
 *
 * Saving deliberately stops at writing the PACK library item -- it does not also bundle a
 * `.kpack` file or open the share sheet. Sharing only ever happens from the Saved
 * Packs gallery (Home's Packs tab), so a half-finished or renamed-later
 * pack is never accidentally sent out mid-edit; "save" and "share" stay two separate actions the
 * way "save project" and "share project" already are on the main gallery.
 */
data class PackCreatorConfig(
    val canvasDimension: Int,
    val tags: Map<Int, PackFrameTag>,
    val onSetTag: (frameIndex: Int, tag: PackFrameTag) -> Unit,
    /**
     * Receives the editor's live timeline length after all of its frame bitmaps have been flushed
     * to the draft cache. A pack draft's JSON document is only its opening/reload snapshot, so it
     * must not be used as the authority for this value when exporting a newly expanded pack.
     */
    val onSavePack: (totalFrames: Int) -> Unit,
    val onReplaceTags: (
        Map<Int, PackFrameTag>,
    ) -> Unit = { tags -> tags.forEach { (index, tag) -> onSetTag(index, tag) } },
)
