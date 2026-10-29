package com.donnclab.kinetiframe

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.donnclab.kinetiframe.actors.data.ActorReferenceIndex
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.home.HomeActionsViewModel
import com.donnclab.kinetiframe.library.domain.LibraryCatalog
import com.donnclab.kinetiframe.presentation.actors.ActorManagerViewModel
import com.donnclab.kinetiframe.presentation.actors.ActorStudioDocumentViewModel
import com.donnclab.kinetiframe.presentation.export.ExportViewModel
import com.donnclab.kinetiframe.presentation.library.HierarchyBrowserViewModel
import com.donnclab.kinetiframe.presentation.library.LibraryPickerViewModel
import com.donnclab.kinetiframe.presentation.library.LibraryViewModel
import com.donnclab.kinetiframe.presentation.library.LocalLibraryCatalog
import com.donnclab.kinetiframe.presentation.library.packs.PackCreatorViewModel
import com.donnclab.kinetiframe.presentation.library.packs.PacksTabViewModel
import com.donnclab.kinetiframe.presentation.project.ProjectManagerViewModel
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.actorstudio.ActorArtworkPickerViewModel
import com.donnclab.kinetiframe.presentation.studio.color.PaletteViewModel
import com.donnclab.kinetiframe.presentation.studio.editor.EditorStorageViewModel
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineServices
import com.donnclab.kinetiframe.settings.SettingsViewModel

/**
 * Builds every ViewModel that needs the app's services, from [container]. MainActivity provides
 * it to the UI as `LocalViewModelFactory`, and screens ask for a ViewModel with `appViewModel()`.
 *
 * Values that differ per screen, such as the document being edited, come from the creation
 * extras; each ViewModel declares the keys it reads. ViewModels that need no services, such as
 * the sketch editor's, keep their own small factories.
 */
fun appViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { SettingsViewModel(container.preferences) }
    initializer { ProjectManagerViewModel(container.projectCatalog) }
    initializer {
        ActorManagerViewModel(container.actors, ActorReferenceIndex(container.files), container.actorBundles)
    }
    initializer {
        ActorStudioDocumentViewModel(
            checkNotNull(this[ActorStudioDocumentViewModel.TargetKey]) { "Actor Studio needs a target" },
            container.actors,
            container.performances,
            artwork = container.actorArtwork,
        )
    }
    initializer {
        ExportViewModel(
            container.newProjectExporter(),
            checkNotNull(this[ExportViewModel.ProjectIdKey]) { "Export needs a project" },
        )
    }
    initializer { HierarchyBrowserViewModel(container.library.organizationStore()) }
    initializer { HomeActionsViewModel(container.bundledContent, container.projectTransfers) }
    initializer {
        PacksTabViewModel(
            container.library,
            this[PacksTabViewModel.ScopeKey] ?: LibraryScope.PERSONAL,
            this[PacksTabViewModel.ProjectIdKey],
        )
    }
    initializer {
        LibraryViewModel(
            LocalLibraryCatalog(
                LibraryCatalog(
                    container.files,
                    container.library,
                    container.projects,
                    container.scannedActors,
                    container.scannedPerformances,
                ),
            ),
            container.bundledContent,
        )
    }
    initializer { LibraryPickerViewModel(container.library, this[LibraryPickerViewModel.ProjectIdKey]) }
    initializer { PackCreatorViewModel(container.packDrafts, this[PackCreatorViewModel.EditKey]) }
    initializer { PaletteViewModel(container.palettes) }
    initializer { ActorArtworkPickerViewModel(container.libraryArtwork) }
    initializer {
        EditorStorageViewModel(
            container.projectSaver,
            container.libraryTransfers,
            container.actorRendering,
            TimelineServices(container.performanceChoices, container.voiceStyles),
            container.projectOpener,
        )
    }
    initializer {
        val arguments = checkNotNull(this[StudioViewModel.ArgumentsKey]) { "The editor needs its arguments" }
        StudioViewModel(
            arguments.cacheDir,
            loadActorPresets = container.actorPresets::load,
            initialEditorState = arguments.initialEditorState,
            runtimeRigAssetManager = arguments.runtimeRigAssetManager,
            filesDir = arguments.filesDir,
            diagnosticsDir = container.filesDir,
        )
    }
}
