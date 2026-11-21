package com.donnclab.kinetiframe.library.bundled

/** Versioned starter plates; images are streamed to disk by the library installer, not decoded. */
object BundledSceneBackgrounds {
    data class Scene(val slug: String, val name: String, val category: String) {
        val libraryId get() = "kf-background-$slug-v1"
        val assetPath get() = "backgrounds/v1/$category/$slug.png"
    }

    val entries = listOf(
        Scene("living-room", "Living room", "indoor"),
        Scene("kitchen", "Kitchen", "indoor"),
        Scene("classroom", "Classroom", "education"),
        Scene("quiet-street", "Quiet street", "outdoor"),
        Scene("park", "Open park", "outdoor"),
        Scene("countryside", "Countryside", "outdoor"),
        Scene("curtain-stage", "Curtain stage", "performance"),
        Scene("space", "Space", "education"),
        Scene("diagram", "Diagram canvas", "generic"),
        Scene("colour-studio", "Colour studio", "generic"),
    )
}
