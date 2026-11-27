package com.donnclab.kinetiframe.presentation.project

import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import java.util.UUID

data class ProjectDraftSessionState(val id: String = UUID.randomUUID().toString(), val ready: Boolean = false) :
    ViewState
sealed interface ProjectDraftSessionIntent : ViewIntent {
    data object Prepared : ProjectDraftSessionIntent
}
sealed interface ProjectDraftSessionEffect : ViewEffect

/** Navigation-entry scoped identity survives rotation with the engine, but not process death.
 * A recreated process must open saved pixels, never orphaned pixels from an unsaved session. */
class ProjectDraftSession :
    BaseViewModel<ProjectDraftSessionState, ProjectDraftSessionIntent, ProjectDraftSessionEffect>(
        ProjectDraftSessionState(),
    ) {
    override fun handleIntent(intent: ProjectDraftSessionIntent) = when (intent) {
        ProjectDraftSessionIntent.Prepared -> updateState { it.copy(ready = true) }
    }
}
