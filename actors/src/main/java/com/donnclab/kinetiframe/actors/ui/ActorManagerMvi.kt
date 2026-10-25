package com.donnclab.kinetiframe.actors.ui

import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState

data class ActorManagerState(
    val loading: Boolean = true,
    val actors: List<ActorManifest> = emptyList(),
    val performances: List<com.donnclab.kinetiframe.core.actor.document.PerformanceDocument> = emptyList(),
    /** Local file paths for card previews; the structural document remains the source of truth. */
    val thumbnailPaths: Map<String, String> = emptyMap(),
    val error: String? = null,
) : ViewState

sealed interface ActorManagerIntent : ViewIntent {
    data object Reload : ActorManagerIntent
    data class Create(val name: String, val template: ActorTemplate) : ActorManagerIntent
    data class Rename(val actorId: String, val name: String) : ActorManagerIntent
    data class Duplicate(val actorId: String) : ActorManagerIntent
    data class Export(val actorId: String) : ActorManagerIntent
    data class Archive(val actorId: String) : ActorManagerIntent
    data class Open(val actorId: String) : ActorManagerIntent
    data class OpenView(val actorId: String, val view: com.donnclab.kinetiframe.core.actor.rigging.ActorView) :
        ActorManagerIntent
}

sealed interface ActorManagerEffect : ViewEffect {
    data class OpenActorStudio(val actorId: String) : ActorManagerEffect
    data class ShowError(val message: String) : ActorManagerEffect
}
