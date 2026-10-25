package com.donnclab.kinetiframe.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The app's ViewModel factory, which builds every ViewModel from the app's services. MainActivity
 * provides it around the navigation graph; screens read it through [appViewModel].
 */
val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModel factory. MainActivity provides one around the navigation graph.")
}

/**
 * The [VM] for the current navigation entry, built by the app's factory the first time it's
 * asked for. [key] separates several instances of one class in the same entry. [arguments] adds
 * the values [VM] reads from its creation extras, such as the document it edits; they are only
 * read when the ViewModel is created.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    noinline arguments: MutableCreationExtras.() -> Unit = {},
): VM {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner for ${VM::class.simpleName}" }
    val defaults =
        (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    return viewModel(owner, key, LocalViewModelFactory.current, MutableCreationExtras(defaults).apply(arguments))
}
