package com.donnclab.kinetiframe.presentation.library

/** An attachment picker owns its nested item picker; a stamp tray instead closes the browser. */
internal fun openPackFromPicker(dismissOnOpen: Boolean, onDismiss: () -> Unit, onOpen: () -> Unit) {
    if (dismissOnOpen) onDismiss()
    onOpen()
}
