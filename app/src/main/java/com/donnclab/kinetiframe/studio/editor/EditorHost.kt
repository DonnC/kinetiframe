// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.runtime.Composable
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.actors.ui.ActorReferenceSession
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.library.ui.packs.PackCreatorConfig
import com.donnclab.kinetiframe.studio.StudioState

/**
 * What the route hosting the editor supplies: which document it edits and what happens when the
 * animator saves, leaves or opens another studio. The parameters of
 * [com.donnclab.kinetiframe.studio.EditorScreen] describe each field.
 */
internal data class EditorHost(
    val projectId: String?,
    val initialEditorState: StudioState?,
    val packCreatorConfig: PackCreatorConfig?,
    val actorReference: ActorReferenceSession?,
    val actorViews: (@Composable () -> Unit)?,
    val onSaveActorRevision: ((PuppetDefinition, RigAssetManager) -> Unit)?,
    val onSavePerformance: ((PuppetInstance, Long) -> Unit)?,
    val onPerformanceActions: (() -> Unit)?,
    val onExitActorStudio: (() -> Unit)?,
    val onCreatePerformance: ((String, String) -> Unit)?,
    val onEditPerformance: ((ActorPlacementReference, String) -> Unit)?,
    val returnedPerformance: PerformanceDocument?,
    val onReturnedPerformanceConsumed: () -> Unit,
    val onActorStudioDirtyChanged: ((Boolean) -> Unit)?,
    val onActorSaveActionAvailable: (((() -> Unit)?) -> Unit)?,
    val onActorSavePreparationFailed: (() -> Unit)?,
    val onHomeClick: (() -> Unit)?,
    val onBackClick: (() -> Unit)?,
) {
    /** Actor Studio is the only caller that seeds [initialEditorState]. */
    val isActorStudioSession: Boolean get() = initialEditorState != null
    val isPackCreator: Boolean get() = packCreatorConfig != null
}
