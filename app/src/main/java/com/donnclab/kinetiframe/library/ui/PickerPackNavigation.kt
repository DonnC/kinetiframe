package com.donnclab.kinetiframe.library.ui

/** An attachment picker owns its nested item picker; a stamp tray instead closes the browser. */
internal fun openPackFromPicker(dismissOnOpen: Boolean, onDismiss: () -> Unit, onOpen: () -> Unit) {
    if (dismissOnOpen) onDismiss()
    onOpen()
}
