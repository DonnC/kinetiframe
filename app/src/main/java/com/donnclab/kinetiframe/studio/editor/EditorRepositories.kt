// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.donnclab.kinetiframe.actors.data.ActorBundleRepository
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.render.ActorFrameRenderer
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.project.data.ProjectRepository
import com.donnclab.kinetiframe.project.data.db.AppDatabase
import java.io.File

/** The on-disk stores the editor reads and writes, all rooted in the same actor-studio directory. */
internal class EditorRepositories(
    val actorStore: DocumentFileStore,
    val actors: ActorRepository,
    val performances: PerformanceRepository,
    val bundles: ActorBundleRepository,
    val library: LibraryRepository,
    val projects: ProjectRepository,
) {
    fun actorFrameRenderer(): ActorFrameRenderer = ActorFrameRenderer(actors, performances, actorStore)
}

@Composable
internal fun rememberEditorRepositories(context: Context): EditorRepositories = remember(context) {
    val actorStore = DocumentFileStore(File(context.filesDir, "actor-studio"))
    val database = AppDatabase.getInstance(context)
    val actors = ActorRepository(actorStore, dao = database.actorDao())
    val performances = PerformanceRepository(actorStore, dao = database.performanceDao())
    EditorRepositories(
        actorStore = actorStore,
        actors = actors,
        performances = performances,
        bundles = ActorBundleRepository(actorStore, actors, performances, libraryDao = database.libraryItemDao()),
        library = LibraryRepository(actorStore, dao = database.libraryItemDao()),
        projects = ProjectRepository(actorStore),
    )
}
