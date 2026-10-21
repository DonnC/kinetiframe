package com.donnclab.kinetiframe.ui.library

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real disk-backed Library; callers seed bundled resources before dispatching Reload.
 */
class LibraryViewModel(private val catalog: LocalLibraryCatalog) :
    BaseViewModel<LibraryState, LibraryIntent, LibraryEffect>(LibraryState()) {
    private var items = emptyMap<String, com.donnclab.kinetiframe.data.actor.LibraryItemManifest>()
    private var loadJob: Job? = null
    private var packJob: Job? = null
    private val organizationWrites = kotlinx.coroutines.sync.Mutex()
    private var browseRevision = 0L

    override fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.Reload -> reload()

            is LibraryIntent.Search -> updateState { it.copy(query = intent.query) }

            is LibraryIntent.SelectTab -> updateState { it.copy(selectedTab = intent.index, selectedFilter = 0) }

            is LibraryIntent.SelectFilter -> updateState { it.copy(selectedFilter = intent.index) }

            is LibraryIntent.BrowsePacks -> {
                browseRevision++
                updateState { it.copy(packBrowser = intent.position) }
                viewModelScope.launch {
                    organizationWrites.lock()
                    try {
                        withContext(Dispatchers.IO) {
                            catalog.organizationStore().rememberBrowser("packs", intent.position)
                        }
                    } catch (
                        cancelled: kotlinx.coroutines.CancellationException,
                    ) {
                        throw cancelled
                    } catch (error: Exception) {
                        sendEffect(LibraryEffect.ShowMessage(error.message ?: "Could not remember filters"))
                    } finally {
                        organizationWrites.unlock()
                    }
                }
            }

            is LibraryIntent.OpenDetails -> updateState { it.copy(openDetails = intent.details) }

            LibraryIntent.DismissDetails -> updateState { it.copy(openDetails = null) }

            is LibraryIntent.OpenPackContents -> {
                packJob?.cancel()
                updateState {
                    it.copy(
                        openDetails = null,
                        openPackContents = intent.pack,
                        packContents = emptyList(),
                        packLoading = true,
                        packError = null,
                    )
                }
                packJob = viewModelScope.launch {
                    try {
                        val item = requireNotNull(items[intent.pack.id])
                        val (contents, organization) = withContext(Dispatchers.IO) {
                            catalog.packContents(item) to catalog.organizationStore().updateItem(
                                com.donnclab.kinetiframe.ui.packs.PackBrowserIndex.key(item),
                                com.donnclab.kinetiframe.ui.packs.PackBrowserIndex.defaults(item),
                            ) {
                                it.copy(lastUsedEpochMs = System.currentTimeMillis())
                            }
                        }
                        updateState {
                            it.copy(packContents = contents, packLoading = false, organization = organization)
                        }
                    } catch (
                        cancelled: kotlinx.coroutines.CancellationException,
                    ) {
                        throw cancelled
                    } catch (
                        error: Exception,
                    ) {
                        updateState {
                            it.copy(
                                packLoading = false,
                                packError =
                                error.message ?: "Could not read pack",
                            )
                        }
                    }
                }
            }

            LibraryIntent.ClosePackContents -> {
                packJob?.cancel()
                updateState { it.copy(openPackContents = null, packContents = emptyList()) }
            }

            LibraryIntent.EditOpenPack -> currentState.openPackContents?.let { pack ->
                items[pack.id]?.let { sendEffect(LibraryEffect.EditPack(it)) }
            }

            is LibraryIntent.AddToCurrentProject -> addToCurrentProject(intent.assetId)
        }
    }

    private fun addToCurrentProject(assetId: String) {
        val target = libraryOpenTarget(assetId)
        if (target != null) {
            updateState { it.copy(openDetails = null) }
            sendEffect(LibraryEffect.OpenDocument(target))
        } else {
            sendEffect(LibraryEffect.ShowMessage("Open a project, then use Add → From Library to insert this item."))
        }
    }

    private fun reload() {
        loadJob?.cancel()
        val revisionAtStart = browseRevision
        loadJob = viewModelScope.launch {
            updateState { it.copy(loading = true) }
            try {
                val loaded = withContext(Dispatchers.IO) { catalog.load() }
                items = loaded.items
                updateState {
                    it.copy(
                        loading = false,
                        projects = loaded.projects,
                        actors = loaded.actors,
                        packs = loaded.packs,
                        audio = loaded.audio,
                        packManifests = loaded.items.filterValues { item ->
                            item.kind ==
                                com.donnclab.kinetiframe.data.actor.LibraryItemKind.PACK
                        },
                        packItemNames = loaded.packItemNames, organization = loaded.organization,
                        packBrowser = if (revisionAtStart !=
                            browseRevision
                        ) {
                            it.packBrowser
                        } else {
                            loaded.organization.browsers["packs"] ?: it.packBrowser
                        },
                    )
                }
                currentState.openPackContents?.let { open ->
                    loaded.packs.firstOrNull { it.id == open.id }?.let {
                        handleIntent(LibraryIntent.OpenPackContents(it))
                    } ?: handleIntent(LibraryIntent.ClosePackContents)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState { it.copy(loading = false) }
                sendEffect(LibraryEffect.ShowMessage(error.message ?: "Could not read Library"))
            }
        }
    }
}
