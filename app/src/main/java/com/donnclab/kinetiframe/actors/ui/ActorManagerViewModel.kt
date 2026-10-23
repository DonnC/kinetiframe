package com.donnclab.kinetiframe.actors.ui

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.actors.data.ActorBundleRepository
import com.donnclab.kinetiframe.actors.data.ActorManifest
import com.donnclab.kinetiframe.actors.data.ActorReferenceIndex
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Dashboard-only MVI model; it never owns a canvas, rig drag or project timeline. */
class ActorManagerViewModel(
    private val repository: ActorRepository,
    private val references: ActorReferenceIndex,
    private val bundles: ActorBundleRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val id: () -> String = { UUID.randomUUID().toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<ActorManagerState, ActorManagerIntent, ActorManagerEffect>(ActorManagerState()) {
    init {
        reload()
    }

    override fun handleIntent(intent: ActorManagerIntent) = when (intent) {
        ActorManagerIntent.Reload -> reload()

        is ActorManagerIntent.Create -> create(intent.name, intent.template)

        is ActorManagerIntent.Rename -> rename(intent.actorId, intent.name)

        is ActorManagerIntent.Duplicate -> duplicate(intent.actorId)

        is ActorManagerIntent.Export -> export(intent.actorId)

        is ActorManagerIntent.Archive -> mutate { repository.archive(intent.actorId, references::isReferenced) }

        is ActorManagerIntent.Open -> sendEffect(ActorManagerEffect.OpenActorStudio(intent.actorId))

        is ActorManagerIntent.OpenView -> {
            viewModelScope.launch {
                when (
                    val result = withContext(ioDispatcher) {
                        repository.selectView(intent.actorId, intent.view, now())
                    }
                ) {
                    is ActorStoreResult.Value -> sendEffect(ActorManagerEffect.OpenActorStudio(intent.actorId))
                    is ActorStoreResult.Failure -> sendEffect(ActorManagerEffect.ShowError(result.message))
                }
            }
            Unit
        }
    }

    private fun create(name: String, template: ActorTemplate) = mutate {
        requireNameAvailable(name, null)
        val time = now()
        val actorId = "actor-${id()}"
        val revisionId = "revision-${id()}"
        val templateId = when (template) {
            ActorTemplate.HUMAN -> ActorManifest.HUMAN_TEMPLATE_ID
            ActorTemplate.ANIMAL -> ActorManifest.ANIMAL_TEMPLATE_ID
            ActorTemplate.CUSTOM -> ActorManifest.CUSTOM_TEMPLATE_ID
        }
        val view = when (template) {
            ActorTemplate.HUMAN -> com.donnclab.kinetiframe.actors.rig.model.ActorView.FRONT
            ActorTemplate.ANIMAL -> com.donnclab.kinetiframe.actors.rig.model.ActorView.PROFILE
            ActorTemplate.CUSTOM -> com.donnclab.kinetiframe.actors.rig.model.ActorView.ORIGINAL
        }
        repository.save(
            ActorManifest(
                actorId = actorId,
                displayName = name.trim(),
                templateId = templateId,
                currentRevisionId = revisionId,
                createdAtEpochMs = time,
                updatedAtEpochMs = time,
                viewRevisions = mapOf(view to revisionId),
            ),
            ActorRevision(
                revisionId = revisionId,
                actorId = actorId,
                sceneGraph = ActorTemplates.create(template, StageDimensions.LandscapeHd, view),
                createdAtEpochMs = time,
                view = view,
            ),
        )
    }

    private fun rename(actorId: String, name: String) = mutate {
        requireNameAvailable(name, actorId)
        repository.rename(actorId, name, now())
    }

    private fun duplicate(actorId: String) = mutate {
        val source = when (val loaded = repository.load(actorId)) {
            is ActorStoreResult.Value -> loaded.value.first
            is ActorStoreResult.Failure -> return@mutate loaded
        }
        var copyIndex = 1
        var name = "${source.displayName} copy"
        while (!nameIsAvailable(name, null)) {
            copyIndex++
            name = "${source.displayName} copy $copyIndex"
        }
        repository.duplicate(actorId, "actor-${id()}", "revision-${id()}", name, now())
    }

    private fun export(actorId: String) = mutate {
        val actor = when (val loaded = repository.load(actorId)) {
            is ActorStoreResult.Value -> loaded.value.first
            is ActorStoreResult.Failure -> return@mutate loaded
        }
        bundles.exportActor(actorId, "library-${id()}", actor.displayName, now())
    }

    private fun requireNameAvailable(name: String, excludingActorId: String?) {
        require(nameIsAvailable(name, excludingActorId)) { "An actor named '${name.trim()}' already exists" }
    }

    private fun nameIsAvailable(name: String, excludingActorId: String?): Boolean =
        when (val result = repository.isDisplayNameAvailable(name, excludingActorId)) {
            is ActorStoreResult.Value -> result.value
            is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }

    private fun reload() {
        updateState { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // A warm index answers this in one Room read with each actor's thumbnail already
            // attached -- no per-actor artwork-file walk on every dashboard open, unlike before
            // this repository had a Room-backed index.
            when (val result = withContext(ioDispatcher) { repository.listWithThumbnails() }) {
                is ActorStoreResult.Value -> {
                    val actors = result.value.map { it.first }
                    val thumbnails = withContext(ioDispatcher) {
                        actors.mapNotNull { actor ->
                            when (val preview = repository.characterPreview(actor.actorId)) {
                                is ActorStoreResult.Value -> preview.value?.let { actor.actorId to it }
                                is ActorStoreResult.Failure -> null
                            }
                        }.toMap()
                    }
                    val performances = withContext(ioDispatcher) { repository.relatedPerformances() }
                    updateState {
                        it.copy(
                            loading = false,
                            actors = actors,
                            thumbnailPaths = thumbnails,
                            performances = performances,
                        )
                    }
                }

                is ActorStoreResult.Failure -> updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun mutate(action: () -> ActorStoreResult<*>) {
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    try {
                        action()
                    } catch (
                        cancel: kotlinx.coroutines.CancellationException,
                    ) {
                        throw cancel
                    } catch (
                        error: Exception,
                    ) {
                        ActorStoreResult.Failure(error.message ?: "Actor operation failed", error)
                    }
                }
            ) {
                is ActorStoreResult.Value -> reload()
                is ActorStoreResult.Failure -> sendEffect(ActorManagerEffect.ShowError(result.message))
            }
        }
    }
}
