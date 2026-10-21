package com.donnclab.kinetiframe.ui.components.camera

import com.donnclab.kinetiframe.ui.TimelineIntent

/** Audience framing is a viewing mode, not a lock on navigation or an implicit edit mode. */
internal object CameraWorkspacePolicy {
    const val PREVIEW_ON_OPEN = false

    fun acceptsIntent(preview: Boolean, fittedDialog: Boolean, intent: TimelineIntent): Boolean = !fittedDialog && (
        !preview ||
            intent is TimelineIntent.View.UpdateTransform || intent is TimelineIntent.View.SetViewportSize
        )

    fun selectsArtwork(intent: TimelineIntent): Boolean =
        intent is TimelineIntent.Draw.SelectTool || intent is TimelineIntent.Draw.SelectBrush ||
            intent is TimelineIntent.Draw.SelectConstructionShape
}
