package com.donnclab.kinetiframe.presentation.common

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Text a ViewModel hands to its screen without holding a Context. The screen turns it into a
 * string, so it follows the device language.
 */
sealed interface UiText {
    /** A string resource, formatted with [args]. An arg that is itself a [UiText] is resolved first. */
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** A plurals resource chosen by [count] and formatted with [args]. */
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    /** Text that is already worded, such as a failure message from a layer that has no typed
     *  error yet. */
    data class Verbatim(val text: String) : UiText
}

@Composable
@ReadOnlyComposable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(id, *args.resolved())
    is UiText.Plural -> pluralStringResource(id, count, *args.resolved())
    is UiText.Verbatim -> text
}

/** A typed failure's wording, else the failure's own message, or [fallback] when it has none. */
fun Throwable.toUiText(@StringRes fallback: Int): UiText =
    typedFailureText() ?: message?.let(UiText::Verbatim) ?: UiText.Resource(fallback)

/** For effects handled outside composition, such as a snackbar shown from a LaunchedEffect. */
fun UiText.asString(resources: Resources): String = when (this) {
    is UiText.Resource -> resources.getString(id, *args.resolved(resources))
    is UiText.Plural -> resources.getQuantityString(id, count, *args.resolved(resources))
    is UiText.Verbatim -> text
}

@Composable
@ReadOnlyComposable
private fun List<Any>.resolved(): Array<Any> = map { if (it is UiText) it.asString() else it }.toTypedArray()

private fun List<Any>.resolved(resources: Resources): Array<Any> =
    map { if (it is UiText) it.asString(resources) else it }.toTypedArray()
