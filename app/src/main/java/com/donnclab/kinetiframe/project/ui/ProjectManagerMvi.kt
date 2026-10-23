package com.donnclab.kinetiframe.project.ui

import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.project.data.ProjectManifest
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState

data class ProjectManagerState(
    val loading: Boolean = true,
    val projects: List<ProjectManifest> = emptyList(),
    val error: String? = null,
) : ViewState

sealed interface ProjectManagerIntent : ViewIntent {
    data object Reload : ProjectManagerIntent
    data class Create(val name: String, val stage: StageDimensions, val fps: Int) : ProjectManagerIntent
    data class Rename(val projectId: String, val name: String) : ProjectManagerIntent
    data class Delete(val projectId: String) : ProjectManagerIntent
    data class Open(val projectId: String) : ProjectManagerIntent
}

sealed interface ProjectManagerEffect : ViewEffect {
    data class OpenProject(val projectId: String) : ProjectManagerEffect
    data class ShowError(val message: String) : ProjectManagerEffect
}
