package com.donnclab.kinetiframe.library.bundled

/** Starter background plates; images are streamed to disk by the library installer, not decoded. */
object BundledSceneBackgrounds {
    data class Scene(val slug: String, val name: String, val category: String) {
        val libraryId get() = "kf-background-$slug-v1"

        // WebP: the plates are painted scenes, and lossy WebP keeps them looking the same at a
        // twentieth of the PNG size.
        val assetPath get() = "backgrounds/$category/$slug.webp"
    }

    val entries = listOf(
        Scene("living-room", "Living room", "indoor"),
        Scene("kitchen", "Kitchen", "indoor"),
        Scene("bedroom", "Bedroom", "indoor"),
        Scene("classroom", "Classroom", "education"),
        Scene("library", "Library", "education"),
        Scene("quiet-street", "Quiet street", "outdoor"),
        Scene("park", "Open park", "outdoor"),
        Scene("countryside", "Countryside", "outdoor"),
        Scene("park-bench", "Bus stop bench", "outdoor"),
        Scene("old-village", "Old village", "outdoor"),
        Scene("curtain-stage", "Curtain stage", "performance"),
        Scene("space", "Space", "education"),
        Scene("diagram", "Diagram canvas", "generic"),
        Scene("colour-studio", "Colour studio", "generic"),
    )
}
