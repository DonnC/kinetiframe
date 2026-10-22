package com.donnclab.kinetiframe.ui.components.camera

import com.donnclab.kinetiframe.ui.StudioIntent

/** Audience framing is a viewing mode, not a lock on navigation or an implicit edit mode. */
internal object CameraWorkspacePolicy {
    const val PREVIEW_ON_OPEN = false

    fun acceptsIntent(preview: Boolean, fittedDialog: Boolean, intent: StudioIntent): Boolean = !fittedDialog && (
        !preview ||
            intent is StudioIntent.View.UpdateTransform || intent is StudioIntent.View.SetViewportSize
        )

    fun selectsArtwork(intent: StudioIntent): Boolean =
        intent is StudioIntent.Draw.SelectTool || intent is StudioIntent.Draw.SelectBrush ||
            intent is StudioIntent.Draw.SelectConstructionShape
}
