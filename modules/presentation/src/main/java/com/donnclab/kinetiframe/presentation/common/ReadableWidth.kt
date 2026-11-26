// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.donnclab.kinetiframe.designsystem.theme.Dimens

/**
 * Keeps a form or settings column to a readable width, centred, on wide windows. On a phone it
 * changes nothing; on a tablet fields and buttons no longer stretch across the whole screen.
 */
fun Modifier.readableWidth(): Modifier =
    wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = Dimens.readableWidth)
