package com.donnclab.kinetiframe.engine.cache

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

    fun delete(exposures: Map<Int, String>, index: Int): Map<Int, String> =
        exposures.filterKeys { it != index }.mapKeys { (frame, _) -> if (frame > index) frame - 1 else frame }

    fun link(exposures: Map<Int, String>, first: Int, last: Int, cel: String): Map<Int, String> {
        require(first >= 0 && last >= first)
        return exposures + (first..last).associateWith { cel }
    }
}
