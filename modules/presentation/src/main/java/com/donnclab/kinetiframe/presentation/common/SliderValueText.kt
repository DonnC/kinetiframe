// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources

/**
 * A slider's value text, formatted by the string resource [id] with the argument [transform]
 * makes from the value, such as `valueText(R.string.value_percent) { (it * 100).toInt() }`.
 */
@Composable
@ReadOnlyComposable
fun valueText(@StringRes id: Int, transform: (Float) -> Any): (Float) -> String {
    val resources = LocalResources.current
    return { resources.getString(id, transform(it)) }
}
