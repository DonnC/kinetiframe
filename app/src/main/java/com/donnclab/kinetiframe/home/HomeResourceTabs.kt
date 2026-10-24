package com.donnclab.kinetiframe.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.actors.ui.ActorManagerIntent
import com.donnclab.kinetiframe.actors.ui.ActorManagerMessage
import com.donnclab.kinetiframe.actors.ui.ActorManagerState
import com.donnclab.kinetiframe.actors.ui.ActorNameDialog
import com.donnclab.kinetiframe.actors.ui.AddActorScreen
import com.donnclab.kinetiframe.common.ui.CompactTextField
import com.donnclab.kinetiframe.common.ui.FullScreenDialog
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens
import com.donnclab.kinetiframe.designsystem.theme.LibraryAccent
import com.donnclab.kinetiframe.library.data.AssetOrganization
import com.donnclab.kinetiframe.library.data.BrowserPosition
import com.donnclab.kinetiframe.library.data.PackItemEntry
import com.donnclab.kinetiframe.library.ui.CollectionAssignmentDialog
import com.donnclab.kinetiframe.library.ui.CollectionBrowserControls
import com.donnclab.kinetiframe.library.ui.HierarchyBrowserIndex
import com.donnclab.kinetiframe.library.ui.HierarchyBrowserIntent
import com.donnclab.kinetiframe.library.ui.HierarchyBrowserState
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserControls
import com.donnclab.kinetiframe.library.ui.packs.PacksTabCallbacks
import com.donnclab.kinetiframe.library.ui.packs.PacksTabIntent
import com.donnclab.kinetiframe.library.ui.packs.PacksTabState
import com.donnclab.kinetiframe.project.ui.AddProjectRoute
import com.donnclab.kinetiframe.project.ui.ProjectManagerIntent
import com.donnclab.kinetiframe.project.ui.ProjectManagerState
import java.io.File

@Composable
internal fun ProjectsTabContent(
    hierarchy: HierarchyBrowserState,
    onHierarchyIntent: (HierarchyBrowserIntent) -> Unit,
    state: ProjectManagerState,
    query: String,
    onIntent: (ProjectManagerIntent) -> Unit,
    onShare: (ProjectManifest) -> Unit,
    onSaveToLibrary: (ProjectManifest) -> Unit,
    onExportVideo: (ProjectManifest) -> Unit,
    onRequestCreate: () -> Unit,
) {
    var renameCandidate by remember { mutableStateOf<ProjectManifest?>(null) }
    var deleteCandidate by remember { mutableStateOf<ProjectManifest?>(null) }
    var organiseCandidate by remember { mutableStateOf<ProjectManifest?>(null) }
    val organization = hierarchy.organization
    val position = organization.browsers["projects"] ?: BrowserPosition()
    val filtered = remember(state.projects, query, organization, position) {
        HierarchyBrowserIndex.filter(
            state.projects,
            { "project:${it.projectId}" },
            { it.displayName },
            { it.updatedAtEpochMs },
            organization,
            position,
            query,
        )
    }
    Column(Modifier.fillMaxSize()) {
        CollectionBrowserControls(
            position,
            HierarchyBrowserIndex.collections(state.projects, {
                "project:${it.projectId}"
            }, organization),
            { onHierarchyIntent(HierarchyBrowserIntent.Browse("projects", it)) },
        )
        hierarchy.error?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        Box(Modifier.weight(1f)) {
            when {
                state.loading -> LoadingBox()

                state.error != null -> ActorManagerMessage(state.error, "Retry") {
                    onIntent(ProjectManagerIntent.Reload)
                }

                state.projects.isEmpty() -> ActorManagerMessage(
                    "Create a project to start animating.",
                    "New project",
                    onRequestCreate,
                )

                filtered.isEmpty() -> EmptySearchMessage()

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.projectId }) { project ->
                        HomeProjectCard(
                            project, {
                                onHierarchyIntent(HierarchyBrowserIntent.Used("project:${project.projectId}"))
                                onIntent(ProjectManagerIntent.Open(project.projectId))
                            },
                            { renameCandidate = project }, { deleteCandidate = project },
                            { onShare(project) }, { onSaveToLibrary(project) }, { onExportVideo(project) },
                            favourite = organization.items["project:${project.projectId}"]?.favourite == true,
                            onFavourite = {
                                onHierarchyIntent(HierarchyBrowserIntent.Favourite("project:${project.projectId}"))
                            },
                            onOrganise = { organiseCandidate = project },
                        )
                    }
                }
            }
        }
    }
    organiseCandidate?.let { project ->
        CollectionAssignmentDialog(
            organization.items["project:${project.projectId}"] ?: AssetOrganization(),
            { organiseCandidate = null },
            {
                onHierarchyIntent(HierarchyBrowserIntent.Organise("project:${project.projectId}", it))
                organiseCandidate =
                    null
            },
        )
    }
    renameCandidate?.let { project ->
        ActorNameDialog(
            initialName = project.displayName,
            title = "Rename project",
            onDismiss = { renameCandidate = null },
            onConfirm = { name ->
                onIntent(ProjectManagerIntent.Rename(project.projectId, name))
                renameCandidate = null
            },
        )
    }
    deleteCandidate?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete ${project.displayName}?") },
            text = {
                Text(
                    "This permanently deletes the project and everything in it -- frames, actor performances and the background image. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(ProjectManagerIntent.Delete(project.projectId))
                    deleteCandidate =
                        null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun ActorsTabContent(
    projectNames: Map<String, String>,
    hierarchy: HierarchyBrowserState,
    onHierarchyIntent: (HierarchyBrowserIntent) -> Unit,
    onOpenPerformanceProject: (String) -> Unit,
    state: ActorManagerState,
    query: String,
    onIntent: (ActorManagerIntent) -> Unit,
    onRequestCreate: () -> Unit,
    onRequestRename: (String) -> Unit,
) {
    var archiveCandidate by remember { mutableStateOf<ActorManifest?>(null) }
    var selectedActor by remember { mutableStateOf<ActorManifest?>(null) }
    var organiseCandidate by remember { mutableStateOf<ActorManifest?>(null) }
    val organization = hierarchy.organization
    val position = organization.browsers["actors"] ?: BrowserPosition()
    val filtered = remember(state.actors, state.performances, query, organization, position) {
        HierarchyBrowserIndex.filter(state.actors, { "actor:${it.actorId}" }, { actor ->
            actor.displayName + " " +
                state.performances.filter { it.actorId == actor.actorId }.joinToString { it.displayName }
        }, { it.updatedAtEpochMs }, organization, position, query)
    }
    Column(Modifier.fillMaxSize()) {
        CollectionBrowserControls(
            position,
            HierarchyBrowserIndex.collections(state.actors, {
                "actor:${it.actorId}"
            }, organization),
            { onHierarchyIntent(HierarchyBrowserIntent.Browse("actors", it)) },
        )
        hierarchy.error?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        Box(Modifier.weight(1f)) {
            when {
                state.loading -> LoadingBox()

                state.error != null -> ActorManagerMessage(state.error, "Retry") { onIntent(ActorManagerIntent.Reload) }

                state.actors.isEmpty() -> ActorManagerMessage(
                    "Create a Human actor, then draw and rig it in Actor Studio.",
                    "New actor",
                    onRequestCreate,
                )

                filtered.isEmpty() -> EmptySearchMessage()

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.actorId }) { actor ->
                        HomeActorCard(
                            actor, state.thumbnailPaths[actor.actorId], {
                                selectedActor = actor
                                onHierarchyIntent(HierarchyBrowserIntent.Used("actor:${actor.actorId}"))
                            },
                            {
                                onRequestRename(actor.actorId)
                            }, { onIntent(ActorManagerIntent.Duplicate(actor.actorId)) },
                            { onIntent(ActorManagerIntent.Export(actor.actorId)) }, { archiveCandidate = actor },
                            favourite = organization.items["actor:${actor.actorId}"]?.favourite == true,
                            onFavourite = {
                                onHierarchyIntent(HierarchyBrowserIntent.Favourite("actor:${actor.actorId}"))
                            },
                            onOrganise = { organiseCandidate = actor },
                        )
                    }
                }
            }
        }
    }
    selectedActor?.let { actor ->
        com.donnclab.kinetiframe.actors.ui.CharacterDetailsSheet(
            actor,
            state.thumbnailPaths[actor.actorId],
            state.performances.filter { it.actorId == actor.actorId },
            { selectedActor = null },
            { view ->
                selectedActor = null
                onIntent(ActorManagerIntent.OpenView(actor.actorId, view))
            },
            { projectId ->
                selectedActor = null
                onOpenPerformanceProject(projectId)
            },
            projectNames,
        )
    }
    organiseCandidate?.let { actor ->
        CollectionAssignmentDialog(
            organization.items["actor:${actor.actorId}"] ?: AssetOrganization(),
            { organiseCandidate = null },
            {
                onHierarchyIntent(HierarchyBrowserIntent.Organise("actor:${actor.actorId}", it))
                organiseCandidate =
                    null
            },
        )
    }
    archiveCandidate?.let { actor ->
        AlertDialog(
            onDismissRequest = { archiveCandidate = null },
            title = { Text("Archive ${actor.displayName}?") },
            text = {
                Text(
                    "Archived actors can be recovered. Actors used by a project or library item are protected and cannot be archived.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(ActorManagerIntent.Archive(actor.actorId))
                    archiveCandidate =
                        null
                }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { archiveCandidate = null }) { Text("Cancel") } },
        )
    }
}
