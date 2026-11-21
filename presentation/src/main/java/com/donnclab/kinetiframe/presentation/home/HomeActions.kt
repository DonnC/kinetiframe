// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.home

import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.presentation.actors.ActorManagerIntent
import com.donnclab.kinetiframe.presentation.actors.ActorManagerState
import com.donnclab.kinetiframe.presentation.library.HierarchyBrowserIntent
import com.donnclab.kinetiframe.presentation.library.HierarchyBrowserState
import com.donnclab.kinetiframe.presentation.library.packs.PacksTabIntent
import com.donnclab.kinetiframe.presentation.library.packs.PacksTabState
import com.donnclab.kinetiframe.presentation.project.ProjectManagerIntent
import com.donnclab.kinetiframe.presentation.project.ProjectManagerState
import java.io.File

/** What Home shows: each tab's ViewModel state. */
data class HomeStates(
    val projects: ProjectManagerState,
    val packs: PacksTabState,
    val packPreviewFiles: Map<String, List<File>>,
    val actors: ActorManagerState,
    val hierarchy: HierarchyBrowserState = HierarchyBrowserState(),
)

/** Where Home's tabs send their intents. */
data class HomeIntents(
    val onProject: (ProjectManagerIntent) -> Unit,
    val onPack: (PacksTabIntent) -> Unit,
    val onActor: (ActorManagerIntent) -> Unit,
    val onHierarchy: (HierarchyBrowserIntent) -> Unit = {},
)

/** Where the Projects tab sends its intents and actions. */
internal data class ProjectsTabCallbacks(
    val onIntent: (ProjectManagerIntent) -> Unit,
    val onHierarchyIntent: (HierarchyBrowserIntent) -> Unit,
    val onShare: (ProjectManifest) -> Unit,
    val onSaveToLibrary: (ProjectManifest) -> Unit,
    val onExportVideo: (ProjectManifest) -> Unit,
    val onRequestCreate: () -> Unit,
)

/** Where the Actors tab sends its intents and actions. */
internal data class ActorsTabCallbacks(
    val onIntent: (ActorManagerIntent) -> Unit,
    val onHierarchyIntent: (HierarchyBrowserIntent) -> Unit,
    val onOpenPerformanceProject: (String) -> Unit,
    val onRequestCreate: () -> Unit,
    val onRequestRename: (String) -> Unit,
)

/** An actor card's menu actions. */
data class ActorCardActions(
    val onRename: () -> Unit,
    val onDuplicate: () -> Unit,
    val onExport: () -> Unit,
    val onArchive: () -> Unit,
    val onFavourite: () -> Unit = {},
    val onOrganise: () -> Unit = {},
)
