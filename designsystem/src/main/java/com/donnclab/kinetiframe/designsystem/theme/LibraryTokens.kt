// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color

object LibraryTokens {
    /** Behind a pack's stamp drawings, which are dark marks on transparent paper. */
    val stampWell = Color(0xFFF1F1F1)
    val trayWell = Color(0x11000000)

    // A local image's gradient while it loads.
    val thumbnailLoadingStart = Color(0xFFF3F0F4)
    val thumbnailLoadingEnd = Color(0xFFE5E0E8)

    // Cards for library items without an image.
    val itemCardStart = Color(0xFF677D9A)
    val itemCardEnd = Color(0xFF374C67)

    /** Start of an audio row's play button gradient, which ends in the primary color. */
    val playGradientStart = Color(0xFFFFD9CC)
}
