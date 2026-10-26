package com.donnclab.kinetiframe

import android.content.Context
import com.donnclab.kinetiframe.actors.data.ActorArtworkStore
import com.donnclab.kinetiframe.actors.data.ActorBundleRepository
import com.donnclab.kinetiframe.actors.data.ActorPresetRepository
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.data.db.AppDatabase
import com.donnclab.kinetiframe.data.diagnostics.CrashReportStore
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.export.data.ProjectExporter
import com.donnclab.kinetiframe.library.bundled.BundledContent
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PaletteRepository
import com.donnclab.kinetiframe.library.domain.PackDrafts
import com.donnclab.kinetiframe.project.domain.ProjectTransfers
import com.donnclab.kinetiframe.settings.AppPreferencesStore
import java.io.File

/**
 * Every object the app shares across screens, created once and lazily. This is the only place
 * that opens the database or builds a repository or store; screens get them through their
 * ViewModels, which [AppViewModelFactory] builds from this container.
 *
 * Repositories are shared because they hold no screen state. The one cache among them,
 * [LibraryRepository]'s pack commits, stays consistent because every screen writes through the
 * same instance.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** App-private storage root, outside the cache directory the system may clear. */
    val filesDir: File get() = appContext.filesDir

    /** Projects, actors, performances and the library, all under `actor-studio/`. */
    val files by lazy { DocumentFileStore(File(filesDir, "actor-studio")) }
    val database by lazy { AppDatabase.getInstance(appContext) }

    val projects by lazy { ProjectRepository(files) }
    val actors by lazy { ActorRepository(files, dao = database.actorDao()) }
    val performances by lazy { PerformanceRepository(files, dao = database.performanceDao()) }
    val actorArtwork by lazy { ActorArtworkStore(files) }
    val actorBundles by lazy {
        ActorBundleRepository(files, actors, performances, libraryDao = database.libraryItemDao())
    }
    val actorPresets by lazy { ActorPresetRepository(appContext.assets) }
    val library by lazy { LibraryRepository(files, dao = database.libraryItemDao()) }
    val palettes by lazy { PaletteRepository(files) }

    /** Actors and performances read from their files, without the Room index, for the library's
     *  catalog of what is on disk. */
    val scannedActors by lazy { ActorRepository(files) }
    val scannedPerformances by lazy { PerformanceRepository(files) }

    /** The demo projects, library items and sample actors that ship with the app. */
    val bundledContent by lazy { BundledContent(files, library, appContext.assets::open) }
    val projectTransfers by lazy { ProjectTransfers(files, library, filesDir, appContext.cacheDir) }

    val preferences by lazy { AppPreferencesStore(File(filesDir, "preferences.json")) }
    val crashReports by lazy { CrashReportStore(filesDir) }

    val packDrafts by lazy { PackDrafts(files, projects, library, appContext.cacheDir) }

    /** A new exporter for one export screen; it tracks that screen's single export. */
    fun newProjectExporter() = ProjectExporter(
        context = appContext,
        actorStore = files,
        actorRepository = actors,
        performanceRepository = performances,
        projectRepository = projects,
        exportDao = database.exportDao(),
    )
}
