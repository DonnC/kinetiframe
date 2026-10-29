package com.donnclab.kinetiframe.presentation.library.packs

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.model.PackItemEntry
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gallery-only MVI model for Home's Packs tab, mirroring
 *  [com.donnclab.kinetiframe.presentation.project.ProjectManagerViewModel] and
 *  [com.donnclab.kinetiframe.presentation.actors.ActorManagerViewModel]'s shape: list/reload/delete, plus opening one
 *  pack's full contents on demand. No canvas or editor state -- editing a pack is a full separate
 *  destination ([com.donnclab.kinetiframe.presentation.library.packs.PackCreatorRoute] with an `editItemId`), not something
 *  this ViewModel drives. Sharing a pack stays a Route-level, Context-dependent concern -- exactly
 *  like [com.donnclab.kinetiframe.home.HomeRoute]'s own `shareProject` -- not a ViewModel intent. */
class PacksTabViewModel(
    private val libraryRepository: LibraryRepository,
    private val scope: LibraryScope = LibraryScope.PERSONAL,
    private val projectId: String? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<PacksTabState, PacksTabIntent, PacksTabEffect>(PacksTabState()) {
    companion object {
        /** The library scope to list, read by the app's ViewModel factory. Personal when absent. */
        val ScopeKey = object : CreationExtras.Key<LibraryScope> {}

        /** The project whose library to list, for project scope. */
        val ProjectIdKey = object : CreationExtras.Key<String> {}
    }

    private val organizationStore = libraryRepository.organizationStore()

    /** Where [fileName] of the pack [itemId] is stored, for drawing its preview in place. */
    fun packItemFile(itemId: String, fileName: String): java.io.File =
        libraryRepository.packItemFile(itemId, fileName, scope, projectId)

    /** The image a pack's card shows, or null. Call from a background thread. */
    fun previewImage(pack: LibraryItemManifest): java.io.File? = libraryRepository.previewImage(pack)

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

    private fun persist(write: () -> com.donnclab.kinetiframe.library.model.LibraryOrganization) {
        viewModelScope.launch {
            mutations.lock()
            try {
                val saved = withContext(ioDispatcher) { write() }
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
                when (val result = withContext(ioDispatcher) { libraryRepository.list(scope, projectId) }) {
                    is StoreResult.Value -> {
                        val packs = result.value.filter { it.kind == LibraryItemKind.PACK }
                        val counts = mutableMapOf<String, Int>()
                        val previews = mutableMapOf<String, List<PackItemEntry>>()
                        val names = mutableMapOf<String, List<String>>()
                        val organization = withContext(ioDispatcher) { organizationStore.load() }
                        withContext(ioDispatcher) {
                            packs.forEach { pack ->
                                when (
                                    val loaded = libraryRepository.loadPack(
                                        pack.libraryItemId,
                                        pack.scope,
                                        pack.sourceProjectId,
                                    )
                                ) {
                                    is StoreResult.Value -> {
                                        counts[pack.libraryItemId] = loaded.value.second.totalItems
                                        previews[pack.libraryItemId] =
                                            loaded.value.second.items.sortedBy { it.index }.take(4)
                                        names[PackBrowserIndex.key(pack)] =
                                            loaded.value.second.items.map { it.name + " " + it.desc }
                                    }

                                    is StoreResult.Failure -> Unit
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

                    is StoreResult.Failure -> updateState { it.copy(loading = false, error = result.message) }
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
                val result = withContext(ioDispatcher) {
                    libraryRepository.delete(libraryItemId, scope, projectId)
                }
            ) {
                is StoreResult.Value -> reload()
                is StoreResult.Failure -> sendEffect(PacksTabEffect.ShowError(result.message))
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
                    withContext(ioDispatcher) {
                        libraryRepository.loadPack(pack.libraryItemId, pack.scope, pack.sourceProjectId)
                    }
            ) {
                is StoreResult.Value -> updateState {
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

                is StoreResult.Failure -> {
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
