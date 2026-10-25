package com.donnclab.kinetiframe.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserControls
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserIndex
import com.donnclab.kinetiframe.library.ui.packs.PacksTabEffect
import com.donnclab.kinetiframe.library.ui.packs.PacksTabIntent
import com.donnclab.kinetiframe.library.ui.packs.PacksTabViewModel
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.ui.appViewModel

/** Shared Home policy, remembered collection/category, and item-name search while inserting. */
@Composable
internal fun PackPickerBrowser(
    library: LibraryRepository,
    scope: LibraryScope,
    projectId: String?,
    onOpen: (LibraryItemManifest) -> Unit,
) {
    val model: PacksTabViewModel = appViewModel(key = "pack-picker-$scope-$projectId") {
        set(PacksTabViewModel.ScopeKey, scope)
        projectId?.let { set(PacksTabViewModel.ProjectIdKey, it) }
    }
    val state by model.state.collectAsStateWithLifecycle()
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var actionError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(model) { model.handleIntent(PacksTabIntent.Reload) }
    LaunchedEffect(model) { model.effect.collect { if (it is PacksTabEffect.ShowError) actionError = it.message } }
    val visible = remember(state, query) {
        PackBrowserIndex.filter(state.packs, state.itemNames, state.organization.items, state.browser, query)
    }
    Column(Modifier.fillMaxSize()) {
        CompactTextField(query, { query = it }, singleLine = true, placeholder = { Text("Search packs & items") })
        PackBrowserControls(
            state.browser,
            state.packs.map {
                (state.organization.items[PackBrowserIndex.key(it)] ?: PackBrowserIndex.defaults(it)).collection
            }.distinct().sorted(),
            { model.handleIntent(PacksTabIntent.Browse(it)) },
        )
        if (state.error != null || actionError != null) Text(state.error ?: actionError.orEmpty())
        if (state.loading) {
            androidx.compose.material3.CircularProgressIndicator()
        } else if (visible.isEmpty()) {
            Text("No matching packs")
        } else {
            LazyVerticalGrid(
                GridCells.Adaptive(140.dp),
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(visible, key = { it.libraryItemId }) { pack ->
                    LibraryItemCard(
                        pack.copy(
                            displayName =
                            state.organization.items[PackBrowserIndex.key(pack)]?.displayName ?: pack.displayName,
                        ),
                        onClick = {
                            model.handleIntent(PacksTabIntent.Used(pack))
                            onOpen(pack)
                        },
                        localLibrary = library,
                    )
                }
            }
        }
    }
}
