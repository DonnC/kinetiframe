package com.donnclab.kinetiframe.export.domain

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.StageDimensions
import java.io.File

/**
 * Renders a project to a video and saves it to the gallery. The export screen works through this, so its
 * ViewModel can be tested without Android's encoders; `ProjectExporter` is the implementation.
 */
interface VideoExporter {
    /** The name the video is saved and shared under, or null when the project can't be read. */
    fun projectName(projectId: String): String?

    /** Renders [projectId] to an app-private MP4, letterboxed into [targetDimensions] when set. */
    suspend fun export(
        projectId: String,
        targetDimensions: StageDimensions? = null,
        onProgress: (Float) -> Unit = {},
    ): StoreResult<File>

    /** Copies an exported video into the user's gallery. */
    fun saveToGallery(file: File, displayName: String): StoreResult<Unit>
}
