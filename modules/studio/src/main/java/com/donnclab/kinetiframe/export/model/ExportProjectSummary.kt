// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.export.model

import com.donnclab.kinetiframe.core.document.StageDimensions

/** What the export screen shows about the project before it renders: its picture, size and length. */
data class ExportProjectSummary(
    val name: String,
    val stage: StageDimensions,
    val fps: Int,
    val totalFrames: Int,
    val thumbnailPath: String?,
)
