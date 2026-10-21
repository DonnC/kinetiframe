package com.donnclab.kinetiframe.ui.packs

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.LibraryItemKind
import com.donnclab.kinetiframe.data.actor.LibraryItemManifest
import com.donnclab.kinetiframe.data.actor.LibraryScope
import com.donnclab.kinetiframe.data.library.LibraryRepository
import com.donnclab.kinetiframe.data.library.PackItemEntry
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gallery-only MVI model for Home's Packs tab, mirroring
 *  [com.donnclab.kinetiframe.ui.projects.ProjectManagerViewModel] and
 *  [com.donnclab.kinetiframe.ui.actors.ActorManagerViewModel]'s shape: list/reload/delete, plus opening one
 *  pack's full contents on demand. No canvas or editor state -- editing a pack is a full separate
 *  destination ([com.donnclab.kinetiframe.ui.packs.PackCreatorRoute] with an `editItemId`), not something
 *  this ViewModel drives. Sharing a pack stays a Route-level, Context-dependent concern -- exactly
 *  like [com.donnclab.kinetiframe.ui.home.HomeRoute]'s own `shareProject` -- not a ViewModel intent. */
class PacksTabViewModel(
    private val libraryRepository: LibraryRepository,
    private val scope: LibraryScope = LibraryScope.PERSONAL,
    private val projectId: String? = null,
) : BaseViewModel<PacksTabState, PacksTabIntent, PacksTabEffect>(PacksTabState()) {
    private val organizationStore = libraryRepository.organizationStore()
    private var reloadJob: kotlinx.coroutines.Job? = null
    private val mutations = kotlinx.coroutines.sync.Mutex()
    private var browseRevision = 0L
    init {
        reload()
    }

    override fun handleIntent(intent: PacksTabIntent) = when (intent) {
        PacksTabIntent.Reload -> reload()

        is PacksTabIntent.Delete -> delete(intent.libraryItemId)

        is PacksTabIntent.OpenDetails -> openDetails(intent.pack)

        PacksTabIntent.CloseDetails -> updateState { it.copy(openDetails = null, detailsItems = emptyList()) }

        is PacksTabIntent.Browse -> {
            browseRevision++
            updateState { it.copy(browser = intent.position) }
            persist { organizationStore.rememberBrowser("packs", intent.position) }
        }

        is PacksTabIntent.ToggleFavourite -> persist {
            organizationStore.updateItem(PackBrowserIndex.key(intent.pack), PackBrowserIndex.defaults(intent.pack)) {
                it.copy(favourite = !it.favourite)
            }
        }

        is PacksTabIntent.Organise -> persist {
            organizationStore.updateItem(PackBrowserIndex.key(intent.pack)) {
                intent.value.copy(lastUsedEpochMs = it.lastUsedEpochMs, favourite = it.favourite)
            }
        }

        is PacksTabIntent.Used -> persist {
            organizationStore.updateItem(PackBrowserIndex.key(intent.pack), PackBrowserIndex.defaults(intent.pack)) {
                it.copy(lastUsedEpochMs = System.currentTimeMillis())
            }
        }
    }

    private fun persist(write: () -> com.donnclab.kinetiframe.data.library.LibraryOrganization) {
        viewModelScope.launch {
            mutations.lock()
            try {
                val saved = withContext(Dispatchers.IO) { write() }
                updateState { it.copy(organization = saved) }
            } catch (
                cancelled: kotlinx.coroutines.CancellationException,
            ) {
                throw cancelled
            } catch (error: Exception) {
                sendEffect(PacksTabEffect.ShowError(error.message ?: "Could not save organisation"))
            } finally {
                mutations.unlock()
            }
        }
    }

    private fun reload() {
        reloadJob?.cancel()
        val revisionAtStart = browseRevision
        updateState { it.copy(loading = true, error = null) }
        reloadJob = viewModelScope.launch {
            try {
                when (val result = withContext(Dispatchers.IO) { libraryRepository.list(scope, projectId) }) {
                    is ActorStoreResult.Value -> {
                        val packs = result.value.filter { it.kind == LibraryItemKind.PACK }
                        val counts = mutableMapOf<String, Int>()
                        val previews = mutableMapOf<String, List<PackItemEntry>>()
                        val names = mutableMapOf<String, List<String>>()
                        val organization = withContext(Dispatchers.IO) { organizationStore.load() }
                        withContext(Dispatchers.IO) {
                            packs.forEach { pack ->
                                when (
                                    val loaded = libraryRepository.loadPack(
                                        pack.libraryItemId,
                                        pack.scope,
                                        pack.sourceProjectId,
                                    )
                                ) {
                                    is ActorStoreResult.Value -> {
                                        counts[pack.libraryItemId] = loaded.value.second.totalItems
                                        previews[pack.libraryItemId] =
                                            loaded.value.second.items.sortedBy { it.index }.take(4)
                                        names[PackBrowserIndex.key(pack)] =
                                            loaded.value.second.items.map { it.name + " " + it.desc }
                                    }

                                    is ActorStoreResult.Failure -> Unit
                                }
                            }
                        }
                        updateState {
                            it.copy(
                                loading = false,
                                packs = packs,
                                itemCounts = counts,
                                previewItems = previews,
                                itemNames = names,
                                organization = organization,
                                browser = if (revisionAtStart !=
                                    browseRevision
                                ) {
                                    it.browser
                                } else {
                                    organization.browsers["packs"] ?: it.browser
                                },
                            )
                        }
                    }

                    is ActorStoreResult.Failure -> updateState { it.copy(loading = false, error = result.message) }
                }
            } catch (
                cancelled: kotlinx.coroutines.CancellationException,
            ) {
                throw cancelled
            } catch (error: Exception) {
                updateState {
                    it.copy(
                        loading = false,
                        error =
                        error.message ?: "Could not load packs",
                    )
                }
            }
        }
    }

    private fun delete(libraryItemId: String) {
        viewModelScope.launch {
            when (
                val result = withContext(Dispatchers.IO) {
                    libraryRepository.delete(libraryItemId, scope, projectId)
                }
            ) {
                is ActorStoreResult.Value -> reload()
                is ActorStoreResult.Failure -> sendEffect(PacksTabEffect.ShowError(result.message))
            }
        }
    }

    /** Loads every drawing in [pack] (not just the 4-item preview [reload] already cached) for the
     *  drill-down screen -- opened optimistically with whatever preview items are already on hand
     *  so the screen isn't blank while the full list loads. */
    private fun openDetails(pack: LibraryItemManifest) {
        handleIntent(PacksTabIntent.Used(pack))
        updateState {
            it.copy(
                openDetails = pack,
                detailsItems = currentState.previewItems[pack.libraryItemId].orEmpty(),
                detailsLoading = true,
            )
        }
        viewModelScope.launch {
            when (
                val loaded =
                    withContext(Dispatchers.IO) {
                        libraryRepository.loadPack(pack.libraryItemId, pack.scope, pack.sourceProjectId)
                    }
            ) {
                is ActorStoreResult.Value -> updateState {
                    // The details screen may have been dismissed (or a different pack opened)
                    // while this load was in flight -- only apply the result if it's still current.
                    if (it.openDetails?.libraryItemId == pack.libraryItemId) {
                        it.copy(
                            detailsItems = loaded.value.second.items.sortedBy { entry ->
                                entry.index
                            },
                            detailsLoading = false,
                        )
                    } else {
                        it
                    }
                }

                is ActorStoreResult.Failure -> {
                    updateState {
                        if (it.openDetails?.libraryItemId ==
                            pack.libraryItemId
                        ) {
                            it.copy(detailsLoading = false)
                        } else {
                            it
                        }
                    }
                    sendEffect(PacksTabEffect.ShowError(loaded.message))
                }
            }
        }
    }
}
