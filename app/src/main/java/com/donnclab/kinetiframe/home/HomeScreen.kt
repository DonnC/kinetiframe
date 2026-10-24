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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.actors.ui.ActorManagerIntent
import com.donnclab.kinetiframe.actors.ui.ActorManagerMessage
import com.donnclab.kinetiframe.actors.ui.ActorManagerState
import com.donnclab.kinetiframe.actors.ui.ActorNameDialog
import com.donnclab.kinetiframe.actors.ui.AddActorScreen
import com.donnclab.kinetiframe.common.ui.BrandBadge
import com.donnclab.kinetiframe.common.ui.BrandWordmark
import com.donnclab.kinetiframe.common.ui.CompactTextField
import com.donnclab.kinetiframe.common.ui.FullScreenDialog
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens
import com.donnclab.kinetiframe.designsystem.theme.LibraryAccent
import com.donnclab.kinetiframe.library.data.PackItemEntry
import com.donnclab.kinetiframe.library.ui.HierarchyBrowserIntent
import com.donnclab.kinetiframe.library.ui.HierarchyBrowserState
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserControls
import com.donnclab.kinetiframe.library.ui.packs.PacksTabCallbacks
import com.donnclab.kinetiframe.library.ui.packs.PacksTabIntent
import com.donnclab.kinetiframe.library.ui.packs.PacksTabState
import com.donnclab.kinetiframe.navigation.AppBottomNavBar
import com.donnclab.kinetiframe.navigation.AppBrandBar
import com.donnclab.kinetiframe.navigation.BottomNavDestination
import com.donnclab.kinetiframe.project.ui.AddProjectRoute
import com.donnclab.kinetiframe.project.ui.ProjectManagerIntent
import com.donnclab.kinetiframe.project.ui.ProjectManagerState
import java.io.File

/**
 * The app's Home dashboard: one tabbed destination for projects, packs and actors. The top bar
 * (brand mark, search, import), the tab row and the bottom nav bar with its center "add" action
 * are shared by all three tabs; "add" creates whatever the open tab holds.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    hierarchyState: HierarchyBrowserState = HierarchyBrowserState(),
    onHierarchyIntent: (HierarchyBrowserIntent) -> Unit = {},
    projectState: ProjectManagerState,
    onProjectIntent: (ProjectManagerIntent) -> Unit,
    packState: PacksTabState,
    packPreviewFiles: Map<String, List<File>>,
    onPackIntent: (PacksTabIntent) -> Unit,
    actorState: ActorManagerState,
    onActorIntent: (ActorManagerIntent) -> Unit,
    onShareProject: (ProjectManifest) -> Unit = {},
    onSaveToLibraryProject: (ProjectManifest) -> Unit = {},
    onExportVideoProject: (ProjectManifest) -> Unit = {},
    onSharePack: (LibraryItemManifest) -> Unit = {},
    onImportFromFile: () -> Unit = {},
    onImportFromLibrary: () -> Unit = {},
    onOpenPackCreator: (String?) -> Unit = {},
    detailsPreviewFile: (String, PackItemEntry) -> File = { _, _ -> File("") },
    onBrowseLibrary: () -> Unit = {},
    onSettingsTapped: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showAddActorScreen by remember { mutableStateOf(false) }
    var renameActorId by remember { mutableStateOf<String?>(null) }
    var showImportSheet by remember { mutableStateOf(false) }

    val tabTitles = listOf("Projects", "Packs", "Actors")

    val fabDescription = when (selectedTab) {
        0 -> "New project"
        1 -> "New pack"
        else -> "New actor"
    }
    val onFabClick: () -> Unit = when (selectedTab) {
        0 -> {
            { showNewProjectDialog = true }
        }

        1 -> {
            { onOpenPackCreator(null) }
        }

        else -> {
            { showAddActorScreen = true }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HomeTopBar(
                searchActive = searchActive,
                searchQuery = searchQuery,
                searchLabel = tabTitles[selectedTab],
                onSearchQueryChange = { searchQuery = it },
                onSearchToggle = {
                    searchActive = !searchActive
                    if (!searchActive) searchQuery = ""
                },
                onImportClick = { showImportSheet = true },
            )
            if (!searchActive) {
                TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab ==
                                        index
                                    ) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> ProjectsTabContent(
                        hierarchy = hierarchyState, onHierarchyIntent = onHierarchyIntent,
                        state = projectState, query = searchQuery, onIntent = onProjectIntent,
                        onShare = onShareProject,
                        onSaveToLibrary = onSaveToLibraryProject,
                        onExportVideo = onExportVideoProject,
                        onRequestCreate = { showNewProjectDialog = true },
                    )

                    1 -> PacksTabContent(
                        state = packState,
                        previewFiles = packPreviewFiles,
                        query = searchQuery,
                        callbacks = PacksTabCallbacks(
                            onIntent = onPackIntent,
                            onShare = onSharePack,
                            onRequestCreate = { onOpenPackCreator(null) },
                            onEdit = { pack -> onOpenPackCreator(pack.libraryItemId) },
                            detailsPreviewFile = detailsPreviewFile,
                        ),
                    )

                    else -> ActorsTabContent(
                        projectNames = projectState.projects.associate { it.projectId to it.displayName },
                        hierarchy = hierarchyState, onHierarchyIntent = onHierarchyIntent,
                        onOpenPerformanceProject = { onProjectIntent(ProjectManagerIntent.Open(it)) },
                        state = actorState, query = searchQuery, onIntent = onActorIntent,
                        onRequestCreate = { showAddActorScreen = true }, onRequestRename = { renameActorId = it },
                    )
                }
            }
        }
        AppBottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            active = BottomNavDestination.HOME,
            onNavigate = { destination ->
                when (destination) {
                    BottomNavDestination.HOME -> {
                        selectedTab = 0
                        searchActive =
                            false
                        searchQuery = ""
                    }

                    BottomNavDestination.LIBRARY -> onBrowseLibrary()

                    BottomNavDestination.SETTINGS -> onSettingsTapped()
                }
            },
        )
        HomeFab(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-39).dp),
            contentDescription = fabDescription,
            onClick = onFabClick,
        )
    }

    if (showNewProjectDialog) {
        AddProjectRoute(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, canvas, fps ->
                onProjectIntent(ProjectManagerIntent.Create(name, canvas.dimensions, fps))
                showNewProjectDialog = false
            },
        )
    }
    if (showAddActorScreen) {
        FullScreenDialog(onDismiss = { showAddActorScreen = false }) {
            AddActorScreen(
                onBack = { showAddActorScreen = false },
                onCreate = { name, template ->
                    onActorIntent(ActorManagerIntent.Create(name, template))
                    showAddActorScreen = false
                },
            )
        }
    }
    renameActorId?.let { actorId ->
        ActorNameDialog(
            initialName = actorState.actors.firstOrNull { it.actorId == actorId }?.displayName.orEmpty(),
            title = "Rename actor",
            onDismiss = { renameActorId = null },
            onConfirm = { name ->
                onActorIntent(ActorManagerIntent.Rename(actorId, name))
                renameActorId = null
            },
        )
    }
    if (showImportSheet) {
        ModalBottomSheet(onDismissRequest = { showImportSheet = false }) {
            ImportSheetContent(
                onFile = {
                    showImportSheet = false
                    onImportFromFile()
                },
                onLibrary = {
                    showImportSheet = false
                    onImportFromLibrary()
                },
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    searchActive: Boolean,
    searchQuery: String,
    searchLabel: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onImportClick: () -> Unit,
) {
    if (!searchActive) {
        AppBrandBar {
            IconButton(onClick = onSearchToggle) { Icon(Icons.Filled.Search, "Search") }
            IconButton(onClick = onImportClick) { Icon(Icons.Filled.FileOpen, "Import") }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (searchActive) {
            CompactTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                placeholder = { Text("Search $searchLabel") },
                singleLine = true,
            )
            IconButton(onClick = onSearchToggle) { Icon(Icons.Filled.Close, "Close search") }
        }
    }
}

@Composable
internal fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
internal fun EmptySearchMessage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Nothing matches your search.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun HomeFab(modifier: Modifier = Modifier, contentDescription: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.size(HomeTokens.fabSize),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

/** The Import bottom sheet's content: From File or From Library. Both import a `.kframe` project
 *  archive; see [HomeRoute]. */
@Composable
private fun ImportSheetContent(onFile: () -> Unit, onLibrary: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.import_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            stringResource(R.string.import_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        ImportRow(
            Lucide.Folder,
            MaterialTheme.colorScheme.primary,
            stringResource(R.string.import_from_file_title),
            stringResource(R.string.import_from_file_subtitle),
            onFile,
        )
        ImportRow(
            Lucide.Layers,
            LibraryAccent,
            stringResource(R.string.import_from_library_title),
            stringResource(R.string.import_from_library_subtitle),
            onLibrary,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ImportRow(icon: ImageVector, tint: Color, label: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(14.dp), color = tint.copy(alpha = 0.14f), modifier = Modifier.size(46.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
