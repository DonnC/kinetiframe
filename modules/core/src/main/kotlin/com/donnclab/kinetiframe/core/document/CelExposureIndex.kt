// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.document

/** A stable cel ID owns pixels; frame positions only expose it. Never alias mutable Bitmaps by
 * numeric frame index: deleting the first exposure must not delete a held drawing's owner. */
object CelExposureIndex {
    fun storageKey(layerId: String, celId: String): String =
        "${layerId.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "main" }}_linked_$celId"
    fun validate(links: Map<String, Map<Int, String>>, totalFrames: Int) {
        require(totalFrames in 1..1000)
        require(links.size <= 256)
        links.forEach { (layer, exposures) ->
            require(layer.isNotBlank() && layer.length <= 240)
            require(exposures.size <= totalFrames)
            exposures.forEach { (frame, cel) ->
                require(frame in 0 until totalFrames)
                require(cel.matches(Regex("[a-zA-Z0-9_-]{1,80}"))) { "Invalid linked drawing ID" }
            }
        }
    }

    fun insert(exposures: Map<Int, String>, index: Int): Map<Int, String> =
        exposures.mapKeys { (frame, _) -> if (frame >= index) frame + 1 else frame }

    /**
     * Like [insert], but a held drawing continues onto the new frame when the frame before it is
     * held and either the frame after it holds the same drawing or the new frame ends the scene. So
     * a background held to the end stays held as the scene grows. [totalFrames] is the count before
     * inserting. Content written to the new frame later replaces the hold there, as any edit does.
     */
    fun insertContinuingHold(exposures: Map<Int, String>, index: Int, totalFrames: Int): Map<Int, String> {
        val shifted = insert(exposures, index)
        val held = exposures[index - 1] ?: return shifted
        val continues = index >= totalFrames || exposures[index] == held
        return if (continues) shifted + (index to held) else shifted
    }

    fun delete(exposures: Map<Int, String>, index: Int): Map<Int, String> =
        exposures.filterKeys { it != index }.mapKeys { (frame, _) -> if (frame > index) frame - 1 else frame }

    fun link(exposures: Map<Int, String>, first: Int, last: Int, cel: String): Map<Int, String> {
        require(first >= 0 && last >= first)
        return exposures + (first..last).associateWith { cel }
    }
}
