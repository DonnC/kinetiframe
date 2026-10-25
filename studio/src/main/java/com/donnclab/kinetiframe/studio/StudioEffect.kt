package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.core.mvi.ViewEffect

/**
 * One-off events the studio shows once, such as an error or a text dialog.
 */
sealed class StudioEffect : ViewEffect {
    data class ShowError(val message: String) : StudioEffect()
    data class ShowTextEditDialog(val id: String, val initialText: String) : StudioEffect()
    data class ShowTextAddDialog(val x: Float, val y: Float) : StudioEffect()
}
