package com.donnclab.kinetiframe

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.donnclab.kinetiframe.data.actor.ActorArtworkStore
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.LibraryScope
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.data.project.db.AppDatabase
import com.donnclab.kinetiframe.data.settings.AppPreferencesStore
import com.donnclab.kinetiframe.data.settings.AppTheme
import com.donnclab.kinetiframe.data.settings.StudioOrientation
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.EditorScreen
import com.donnclab.kinetiframe.ui.actors.ActorStudioRepositories
import com.donnclab.kinetiframe.ui.actors.ActorStudioScreen
import com.donnclab.kinetiframe.ui.actors.ActorStudioTarget
import com.donnclab.kinetiframe.ui.color.ScreenColorPickerHost
import com.donnclab.kinetiframe.ui.components.navigation.BottomNavDestination
import com.donnclab.kinetiframe.ui.components.navigation.ExitConfirmationDialog
import com.donnclab.kinetiframe.ui.export.ExportRoute
import com.donnclab.kinetiframe.ui.home.HomeRoute
import com.donnclab.kinetiframe.ui.library.LibraryOpenTarget
import com.donnclab.kinetiframe.ui.library.LibraryRoute
import com.donnclab.kinetiframe.ui.navigation.ActorStudioKey
import com.donnclab.kinetiframe.ui.navigation.EditorKey
import com.donnclab.kinetiframe.ui.navigation.ExportKey
import com.donnclab.kinetiframe.ui.navigation.LibraryKey
import com.donnclab.kinetiframe.ui.navigation.PackCreatorKey
import com.donnclab.kinetiframe.ui.navigation.ProjectGalleryKey
import com.donnclab.kinetiframe.ui.navigation.SettingsKey
import com.donnclab.kinetiframe.ui.packs.PackCreatorRoute
import com.donnclab.kinetiframe.ui.settings.LocalAppPreferences
import com.donnclab.kinetiframe.ui.settings.SettingsScreen
import com.donnclab.kinetiframe.ui.settings.SettingsViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        SettingsViewModel(AppPreferencesStore(File(filesDir, "preferences.json"))) as T
                },
            )
            val settingsState by settings.state.collectAsStateWithLifecycle()
            val preferences = settingsState.preferences
            val dark = when (preferences.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            CompositionLocalProvider(LocalAppPreferences provides preferences) {
                KinetiFrameTheme(darkTheme = dark, dynamicColor = false) {
                    ScreenColorPickerHost {
                        Surface(
                            modifier = Modifier.fillMaxSize().background(
                                MaterialTheme.colorScheme.background,
                            ).windowInsetsPadding(WindowInsets.safeDrawing),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            val backStack = rememberNavBackStack(ProjectGalleryKey)
                            var confirmExit by androidx.compose.runtime.saveable.rememberSaveable {
                                mutableStateOf(false)
                            }
                            BackHandler(enabled = backStack.size == 1) { confirmExit = true }
                            if (confirmExit) {
                                ExitConfirmationDialog(onDismiss = {
                                    confirmExit = false
                                }, onExit = { finish() })
                            }
                            var returnedPerformance by remember {
                                mutableStateOf<PerformanceDocument?>(null)
                            }

                            /**
                             * Bumped every time a destination is popped, so Home can refresh the lists it
                             * loaded once when its entry was created. Home sits in the back stack the whole
                             * time an editor is open, so its ViewModels' `init { reload() }` never runs
                             * again -- an actor, pack or project saved in a child screen simply never
                             * appeared until the app was restarted. Hoisted here, next to
                             * [returnedPerformance], because MainActivity owns the back stack and is the
                             * only place that knows a pop happened.
                             */
                            var homeRefreshToken by remember { mutableStateOf(0) }
                            val goBack: () -> Unit = {
                                if (backStack.size >
                                    1
                                ) {
                                    backStack.removeLastOrNull()
                                    homeRefreshToken++
                                }
                            }
                            val goHome: () -> Unit = {
                                while (backStack.size >
                                    1
                                ) {
                                    backStack.removeLastOrNull()
                                }
                                homeRefreshToken++
                            }

                            val studio = backStack.lastOrNull().let {
                                it is EditorKey || it is ActorStudioKey ||
                                    it is PackCreatorKey
                            }
                            SideEffect {
                                if (!settingsState.loading) {
                                    requestedOrientation =
                                        if (!studio) {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            preferences.orientation.activityOrientation()
                                        }
                                }
                                WindowCompat.getInsetsController(window, window.decorView).apply {
                                    isAppearanceLightStatusBars = !dark
                                    isAppearanceLightNavigationBars = !dark
                                    systemBarsBehavior =
                                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                    if (studio && preferences.hideSystemBars) {
                                        hide(WindowInsetsCompat.Type.systemBars())
                                    } else {
                                        show(WindowInsetsCompat.Type.systemBars())
                                    }
                                }
                            }

                            // Browsing stays portrait. Studio's preference is independent of export geometry.

                            NavDisplay(
                                backStack = backStack,
                                onBack = goBack,
                                // WHY this list is spelled out rather than left to default: NavDisplay's
                                // default is `listOf(rememberSaveableStateHolderNavEntryDecorator())`
                                // ONLY -- ViewModel scoping is deliberately opt-in and ships in a separate
                                // artifact (lifecycle-viewmodel-navigation3). Without
                                // rememberViewModelStoreNavEntryDecorator, LocalViewModelStoreOwner inside
                                // every entry resolves to this *Activity*, so a `viewModel()` call is
                                // stored process-wide: the StudioViewModel built for an actor sketch was
                                // handed straight back to the next project opened (same class, same
                                // default key, same owner -> the factory never even ran, so the project's
                                // own document and cache directories were silently discarded and the
                                // actor's canvas persisted until the Activity died -- i.e. until an app
                                // restart). It also meant StudioViewModel.onCleared() never fired on
                                // leaving a screen, so the animation engine kept running and pooled
                                // bitmaps/audio/video handles were never released. Scoping stores to the
                                // NavEntry gives every destination its own store, cleared on pop.
                                // Restating the saveable-state-holder default is required: passing this
                                // parameter replaces the default list rather than adding to it.
                                entryDecorators = listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator(),
                                ),
                                entryProvider = entryProvider {
                                    entry<ProjectGalleryKey> {
                                        // Home is one tabbed dashboard for projects, packs and actors.
                                        HomeRoute(
                                            File(filesDir, "actor-studio"),
                                            refreshToken = homeRefreshToken,
                                            onOpenProject = { projectId -> backStack.add(EditorKey(projectId)) },
                                            onOpenPackCreator = { itemId ->
                                                backStack.add(PackCreatorKey(editItemId = itemId))
                                            },
                                            onOpenExport = { projectId -> backStack.add(ExportKey(projectId)) },
                                            onOpenActor = { actorId -> backStack.add(ActorStudioKey(actorId)) },
                                            onOpenLibrary = { backStack.add(LibraryKey) },
                                            onOpenSettings = { backStack.add(SettingsKey) },
                                        )
                                    }
                                    entry<LibraryKey> {
                                        // Top-level destinations share one bottom nav (AppBottomNavBar).
                                        // Home pops the back stack instead of stacking another copy.
                                        LibraryRoute(onEditPack = { item ->
                                            backStack.add(
                                                PackCreatorKey(
                                                    editItemId = item.libraryItemId,
                                                    editScope = item.scope.name,
                                                    sourceProjectId = item.sourceProjectId,
                                                ),
                                            )
                                        }, onOpenDocument = { target ->
                                            when (target) {
                                                is LibraryOpenTarget.Project -> backStack.add(
                                                    EditorKey(target.id),
                                                )

                                                is LibraryOpenTarget.Actor -> backStack.add(
                                                    ActorStudioKey(target.id),
                                                )
                                            }
                                        }, onNavigate = { destination ->
                                            when (destination) {
                                                BottomNavDestination.HOME -> goBack()
                                                BottomNavDestination.SETTINGS -> backStack.add(SettingsKey)
                                                BottomNavDestination.LIBRARY -> Unit
                                            }
                                        })
                                    }
                                    entry<SettingsKey> {
                                        SettingsScreen(
                                            settingsState,
                                            settings::handleIntent,
                                            goBack,
                                            onNavigate = { destination ->
                                                goHome()
                                                when (destination) {
                                                    BottomNavDestination.HOME -> Unit
                                                    BottomNavDestination.LIBRARY -> backStack.add(LibraryKey)
                                                    BottomNavDestination.SETTINGS -> backStack.add(SettingsKey)
                                                }
                                            },
                                        )
                                    }
                                    entry<ExportKey> { key ->
                                        ExportRoute(
                                            File(filesDir, "actor-studio"),
                                            key.projectId,
                                            onExit = goBack,
                                        )
                                    }
                                    entry<PackCreatorKey> { key ->
                                        PackCreatorRoute(
                                            File(filesDir, "actor-studio"),
                                            onExit = goBack,
                                            editingItemId = key.editItemId,
                                            editingScope = LibraryScope.valueOf(
                                                key.editScope,
                                            ),
                                            sourceProjectId = key.sourceProjectId,
                                            onHome = goHome,
                                        )
                                    }
                                    entry<EditorKey> { key ->
                                        EditorScreen(
                                            projectId = key.projectId,
                                            returnedPerformance = returnedPerformance,
                                            onBackClick = goBack,
                                            onSettingsClick = { backStack.add(SettingsKey) },
                                            onHomeClick = goHome,
                                            onReturnedPerformanceConsumed = { returnedPerformance = null },
                                            onCreatePerformance = { actorId, projectId ->
                                                backStack.add(ActorStudioKey(actorId, projectId))
                                            },
                                            onEditPerformance = { placement, projectId ->
                                                backStack.add(
                                                    ActorStudioKey(
                                                        placement.actorId,
                                                        projectId,
                                                        placement.performanceId,
                                                        placement.placementId,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                    entry<ActorStudioKey> { key ->
                                        val db = AppDatabase.getInstance(
                                            this@MainActivity,
                                        )
                                        val actorStore = ActorFileStore(File(filesDir, "actor-studio"))
                                        ActorStudioScreen(
                                            target = ActorStudioTarget(
                                                actorId = key.actorId,
                                                projectId = key.projectId,
                                                performanceId = key.performanceId,
                                                returnPlacementId = key.returnPlacementId,
                                            ),
                                            repositories = ActorStudioRepositories(
                                                actors = ActorRepository(actorStore, dao = db.actorDao()),
                                                performances = PerformanceRepository(
                                                    actorStore,
                                                    dao = db.performanceDao(),
                                                ),
                                                artwork = ActorArtworkStore(
                                                    actorStore,
                                                ),
                                            ),
                                            onExit = goBack,
                                            onPerformanceSaved = { returnedPerformance = it },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun StudioOrientation.activityOrientation(): Int = when (this) {
    StudioOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    StudioOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    StudioOrientation.DEVICE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}
