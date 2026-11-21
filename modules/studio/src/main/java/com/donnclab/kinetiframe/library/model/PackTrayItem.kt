package com.donnclab.kinetiframe.library.model

import android.graphics.Bitmap

/**
 * One drawing in a pack opened as the editor's tray, already copied out of the library and
 * decoded, so it can be dragged onto the canvas repeatedly without reading disk again.
 */
data class PackTrayItem(val fileName: String, val name: String, val thumbnail: Bitmap)
