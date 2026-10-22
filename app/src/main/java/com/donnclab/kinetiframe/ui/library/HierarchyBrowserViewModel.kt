package com.donnclab.kinetiframe.ui.library

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.data.library.AssetOrganization
import com.donnclab.kinetiframe.data.library.BrowseMode
import com.donnclab.kinetiframe.data.library.BrowserPosition
import com.donnclab.kinetiframe.data.library.LibraryOrganization
import com.donnclab.kinetiframe.data.library.LibraryOrganizationStore
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class HierarchyBrowserState(
    val organization: LibraryOrganization = LibraryOrganization(),
    val error: String? = null,
) : ViewState
sealed interface HierarchyBrowserIntent : ViewIntent {
    data class Organise(val key: String, val item: AssetOrganization) : HierarchyBrowserIntent
    data class Browse(val key: String, val position: BrowserPosition) : HierarchyBrowserIntent
    data class Used(val key: String) : HierarchyBrowserIntent
    data class Favourite(val key: String) : HierarchyBrowserIntent
}
sealed interface HierarchyBrowserEffect : ViewEffect

/** Organisation never changes actor IDs, project paths, revisions or exported documents. */
class HierarchyBrowserViewModel(
    private val store: LibraryOrganizationStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<HierarchyBrowserState, HierarchyBrowserIntent, HierarchyBrowserEffect>(HierarchyBrowserState()) {
    private val writes = Mutex()
    init {
        change { store.load() }
    }
    override fun handleIntent(intent: HierarchyBrowserIntent) {
        change {
            when (intent) {
                is HierarchyBrowserIntent.Organise -> store.updateItem(intent.key) { intent.item }

                is HierarchyBrowserIntent.Browse -> store.rememberBrowser(intent.key, intent.position)

                is HierarchyBrowserIntent.Used -> store.updateItem(intent.key) {
                    it.copy(lastUsedEpochMs = System.currentTimeMillis())
                }

                is HierarchyBrowserIntent.Favourite -> store.updateItem(intent.key) {
                    it.copy(favourite = !it.favourite)
                }
            }
        }
    }
    private fun change(action: () -> LibraryOrganization) {
        viewModelScope.launch {
            writes.withLock {
                try {
                    val next = withContext(ioDispatcher) { action() }
                    updateState { it.copy(organization = next, error = null) }
                } catch (
                    cancel: kotlinx.coroutines.CancellationException,
                ) {
                    throw cancel
                } catch (error: Exception) {
                    updateState {
                        it.copy(
                            error =
                            error.message ?: "Organisation could not be saved",
                        )
                    }
                }
            }
        }
    }
}

/** Stable, deterministic projection shared by both browsers; unrelated metadata never leaks in. */
object HierarchyBrowserIndex {
    fun <T> filter(
        items: List<T>,
        key: (T) -> String,
        name: (T) -> String,
        updated: (T) -> Long,
        organization: LibraryOrganization,
        position: BrowserPosition,
        query: String,
    ): List<T> = items.filter { item ->
        val metadata = organization.items[key(item)] ?: AssetOrganization()
        name(item).contains(query.trim(), ignoreCase = true) && when (position.mode) {
            BrowseMode.FAVOURITES -> metadata.favourite
            BrowseMode.COLLECTIONS -> position.collection == null || metadata.collection == position.collection
            else -> true
        }
    }.let { filtered ->
        if (position.mode == BrowseMode.RECENT) {
            filtered.sortedByDescending {
                maxOf(updated(it), organization.items[key(it)]?.lastUsedEpochMs ?: 0)
            }
        } else {
            filtered.sortedBy { name(it).lowercase() }
        }
    }
    fun <T> collections(items: List<T>, key: (T) -> String, organization: LibraryOrganization) =
        items.map { organization.items[key(it)]?.collection ?: "General Essentials" }.distinct().sorted()
}
