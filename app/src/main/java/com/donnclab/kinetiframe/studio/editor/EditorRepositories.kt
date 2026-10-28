// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.donnclab.kinetiframe.KinetiFrameApplication
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore

/** The on-disk stores the editor reads and writes, all rooted in the same actor-studio directory. */
internal class EditorRepositories(
    val actorStore: DocumentFileStore,
    val actors: ActorRepository,
    val performances: PerformanceRepository,
    val projects: ProjectRepository,
)

/** The editor's view of the app's shared stores; see [com.donnclab.kinetiframe.AppContainer]. */
@Composable
internal fun rememberEditorRepositories(context: Context): EditorRepositories = remember(context) {
    val container = (context.applicationContext as KinetiFrameApplication).container
    EditorRepositories(
        actorStore = container.files,
        actors = container.actors,
        performances = container.performances,
        projects = container.projects,
    )
}
