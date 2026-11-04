package com.donnclab.kinetiframe.presentation.common

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource

/**
 * Text a ViewModel hands to its screen without holding a Context. The screen turns it into a
 * string, so it follows the device language.
 */
sealed interface UiText {
    /** A string resource, formatted with [args]. */
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Text that is already worded, such as a failure message from a layer that has no typed
     *  error yet. */
    data class Verbatim(val text: String) : UiText
}

@Composable
@ReadOnlyComposable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Verbatim -> text
}

/** For effects handled outside composition, such as a snackbar shown from a LaunchedEffect. */
fun UiText.asString(resources: Resources): String = when (this) {
    is UiText.Resource -> resources.getString(id, *args.toTypedArray())
    is UiText.Verbatim -> text
}
