package com.donnclab.kinetiframe.library.model

/** One frame's own tag inside a pack draft -- a required name and optional short description,
 *  reviewed in the metadata dialog when saving. */
data class PackFrameTag(val name: String = "", val description: String = "") {
    val isValid: Boolean get() = name.isNotBlank() &&
        name.length <= com.donnclab.kinetiframe.library.data.PackItemEntry.MAX_NAME_LENGTH &&
        description.length <= com.donnclab.kinetiframe.library.data.PackItemEntry.MAX_DESCRIPTION_LENGTH
}
