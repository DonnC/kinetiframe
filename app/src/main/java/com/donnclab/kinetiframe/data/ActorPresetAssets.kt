package com.donnclab.kinetiframe.data

/** Filename-based pairing deliberately ignores stable recipe IDs and editable display names. */
internal object ActorPresetAssets {
    fun previewPath(jsonPath: String, siblings: Collection<String>): String {
        val stem = jsonPath.substringAfterLast('/').substringBeforeLast('.')
        val name = listOf("svg", "png", "jpg", "jpeg").map { "$stem.$it" }
            .firstOrNull { it in siblings }
        require(name != null) { "Missing same-name preview for $jsonPath" }
        return "${jsonPath.substringBeforeLast('/')}/$name"
    }
}
