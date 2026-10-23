package com.donnclab.kinetiframe.actors.ui

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.actors.domain.PerformanceAuthoring
import com.donnclab.kinetiframe.actors.domain.PerformanceCompatibility
import com.donnclab.kinetiframe.actors.domain.PerformanceDuration
import com.donnclab.kinetiframe.actors.rig.model.ActorView
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Owns only a persistent actor revision; canvas mechanics stay in the shared engine adapter. */
class ActorStudioDocumentViewModel(
    target: ActorStudioTarget,
    private val repository: ActorRepository,
    private val performances: PerformanceRepository? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val id: () -> String = { UUID.randomUUID().toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<ActorStudioDocumentState, ActorStudioDocumentIntent, ActorStudioDocumentEffect>(
    ActorStudioDocumentState(),
) {
    private val actorId = target.actorId
    private val projectId = target.projectId
    private val performanceId = target.performanceId

    init {
        reload()
    }

    override fun handleIntent(intent: ActorStudioDocumentIntent) = when (intent) {
        ActorStudioDocumentIntent.Reload -> reload()

        is ActorStudioDocumentIntent.SelectView -> changeView(intent.view, create = false)

        is ActorStudioDocumentIntent.CreateView -> changeView(intent.view, create = true)

        is ActorStudioDocumentIntent.SetDirty -> updateState { state -> state.copy(isDirty = intent.isDirty) }

        ActorStudioDocumentIntent.RequestExit -> {
            if (currentState.isSaving) {
                Unit
            } else {
                sendEffect(ActorStudioDocumentEffect.ConfirmDiscard)
            }
        }

        ActorStudioDocumentIntent.DiscardAndExit -> sendEffect(ActorStudioDocumentEffect.Exit)

        is ActorStudioDocumentIntent.SaveGraph -> save(
            intent.sceneGraph,
            intent.revisionId,
            intent.artworkAssetPaths,
            intent.exitAfterSaving,
        )

        is ActorStudioDocumentIntent.SavePerformance -> savePerformance(
            intent.name,
            intent.poseKeys,
            intent.motionPreset,
            intent.durationMicros,
            intent.rotationCurves,
            intent.poseInterpolation,
            intent.artworkKeys,
            intent.rootMotionCurves,
        )

        is ActorStudioDocumentIntent.DuplicatePerformance -> duplicatePerformance(intent.name)

        ActorStudioDocumentIntent.ArchivePerformance -> archivePerformance()
    }

    private fun reload() {
        updateState { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.load(actorId) }) {
                is ActorStoreResult.Value -> {
                    val performanceResult =
                        withContext(ioDispatcher) {
                            performanceId?.let { id -> projectId?.let { project -> performances?.load(project, id) } }
                        }
                    if (performanceResult is ActorStoreResult.Failure) {
                        updateState { it.copy(loading = false, error = performanceResult.message) }
                        return@launch
                    }
                    val performance = (performanceResult as? ActorStoreResult.Value)?.value
                    val revision = if (performance ==
                        null
                    ) {
                        result.value.second
                    } else {
                        when (
                            val pinned =
                                withContext(ioDispatcher) {
                                    repository.loadRevision(actorId, performance.actorRevisionId)
                                }
                        ) {
                            is ActorStoreResult.Value -> pinned.value

                            is ActorStoreResult.Failure -> {
                                updateState { it.copy(loading = false, error = pinned.message) }
                                return@launch
                            }
                        }
                    }
                    runCatching { performance?.let { PerformanceCompatibility.validate(it, revision) } }
                        .onFailure {
                            updateState { state ->
                                state.copy(
                                    loading = false,
                                    error =
                                    it.message ?: "Incompatible performance",
                                )
                            }
                            return@launch
                        }
                    updateState {
                        it.copy(
                            loading = false,
                            manifest = result.value.first,
                            revision = revision,
                            performance = performance,
                        )
                    }
                }

                is ActorStoreResult.Failure -> updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun changeView(view: ActorView, create: Boolean) {
        if (currentState.loading || currentState.isSaving) return
        if (currentState.isDirty || performanceId != null || currentState.performance != null ||
            (create && projectId != null)
        ) {
            sendEffect(
                ActorStudioDocumentEffect.ShowError(
                    if (currentState.isDirty) {
                        "Save your edits before changing views."
                    } else {
                        "Edit actor views in rig setup. Existing performances keep their authored view."
                    },
                ),
            )
            return
        }
        updateState { it.copy(loading = true) }
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    if (create) {
                        repository.createView(actorId, view, "revision-${id()}", now())
                    } else {
                        repository.selectView(actorId, view, now())
                    }
                }
            ) {
                is ActorStoreResult.Value -> updateState {
                    it.copy(
                        loading = false,
                        manifest = result.value.first,
                        revision = result.value.second,
                        isDirty = false,
                    )
                }

                is ActorStoreResult.Failure -> {
                    updateState { it.copy(loading = false) }
                    sendEffect(ActorStudioDocumentEffect.ShowError(result.message))
                }
            }
        }
    }

    private fun save(graph: SceneGraph, revisionId: String, artwork: Map<String, String>, exitAfterSaving: Boolean) {
        if (projectId != null || currentState.loading || currentState.isSaving) return
        val view = currentState.revision?.view ?: return
        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (
                val result =
                    withContext(ioDispatcher) {
                        repository.saveRevision(actorId, graph, revisionId, now(), artwork, view)
                    }
            ) {
                is ActorStoreResult.Value -> {
                    updateState {
                        it.copy(
                            isSaving = false,
                            manifest = result.value.first,
                            revision = result.value.second,
                            isDirty = false,
                        )
                    }
                    if (exitAfterSaving) sendEffect(ActorStudioDocumentEffect.Exit)
                }

                is ActorStoreResult.Failure -> {
                    updateState { it.copy(isSaving = false) }
                    sendEffect(ActorStudioDocumentEffect.ShowError(result.message))
                }
            }
        }
    }

    private fun savePerformance(
        name: String,
        keys: List<ActorPoseKey>,
        preset: AppliedActorPreset?,
        requestedDurationMicros: Long,
        retainedCurves: List<RotationCurve>,
        interpolation: CurveInterpolation,
        artworkKeys: List<ActorArtworkKey>,
        rootMotionCurves: List<RootMotionCurve>,
    ) {
        if (currentState.loading || currentState.isSaving) return
        val actor = currentState.manifest ?: return
        val revision = currentState.revision ?: return
        val destination =
            projectId
                ?: run {
                    sendEffect(
                        ActorStudioDocumentEffect.ShowError("Open Actor Studio from a project to save a performance."),
                    )
                    return
                }
        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            // A preset applied but never separately keyframed must still produce a performance
            // with real duration and motion -- see LegacyActorMigration.performance, which this
            // mirrors, and PerformanceEvaluator.rotations, which already blends presetSnapshot in.
            val requestedDuration = if (preset != null && artworkKeys.isEmpty() && keys.isEmpty() &&
                retainedCurves.isEmpty() &&
                rootMotionCurves.isEmpty()
            ) {
                preset.durationMicros
            } else {
                maxOf(
                    1L,
                    requestedDurationMicros,
                    preset?.durationMicros ?: 0L,
                    keys.maxOfOrNull { it.timeMicros } ?: 0L,
                    artworkKeys.maxOfOrNull { it.timeMicros } ?: 0L,
                    retainedCurves.maxOfOrNull { it.points.lastOrNull()?.timeMicros ?: 0L } ?: 0L,
                    rootMotionCurves.maxOfOrNull { it.points.lastOrNull()?.timeMicros ?: 0L } ?: 0L,
                )
            }
            val duration = if (artworkKeys.isEmpty()) {
                requestedDuration
            } else {
                PerformanceDuration.expressionLoop(requestedDuration, preset)
            }
            val existing = currentState.performance
            when (
                val result = withContext(ioDispatcher) {
                    storeOperation {
                        val document = PerformanceDocument(
                            performanceId = existing?.performanceId ?: "performance-${id()}",
                            projectId = destination,
                            actorId = actor.actorId,
                            actorRevisionId = revision.revisionId, displayName = name.trim(), durationMicros = duration,
                            playback = if (preset?.recipe?.loop ==
                                false
                            ) {
                                PerformancePlayback.END_HOLD
                            } else {
                                existing?.playback ?: PerformancePlayback.LOOP
                            },
                            poseKeys = keys,
                            artworkKeys = artworkKeys,
                            presetSnapshot = preset,
                            poseInterpolation = interpolation,
                            rotationCurves = retainedCurves.ifEmpty {
                                PerformanceAuthoring.rotationCurves(keys, duration, interpolation)
                            },
                            rootMotionCurves = rootMotionCurves,
                            createdAtEpochMs = existing?.createdAtEpochMs ?: now(), updatedAtEpochMs = now(),
                        )
                        PerformanceCompatibility.validate(document, revision)
                        requireNotNull(performances).save(document)
                    }
                }
            ) {
                is ActorStoreResult.Value -> {
                    updateState { it.copy(isSaving = false, performance = result.value, isDirty = false) }
                    sendEffect(ActorStudioDocumentEffect.PerformanceSaved(result.value))
                }

                is ActorStoreResult.Failure -> {
                    updateState { it.copy(isSaving = false) }
                    sendEffect(ActorStudioDocumentEffect.ShowError(result.message))
                }
            }
        }
    }

    private fun duplicatePerformance(name: String) {
        val existing = currentState.performance ?: return
        val destination = projectId ?: return
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    storeOperation {
                        val copy = existing.copy(
                            performanceId = "performance-${id()}",
                            displayName = name.trim(),
                            createdAtEpochMs = now(),
                            updatedAtEpochMs = now(),
                        ).validate()
                        requireNotNull(performances).save(copy)
                    }
                }
            ) {
                is ActorStoreResult.Value -> updateState { it.copy(performance = result.value, isDirty = false) }
                is ActorStoreResult.Failure -> sendEffect(ActorStudioDocumentEffect.ShowError(result.message))
            }
        }
    }

    private fun archivePerformance() {
        val existing = currentState.performance ?: return
        val destination = projectId ?: return
        viewModelScope.launch {
            when (
                val result =
                    withContext(ioDispatcher) {
                        requireNotNull(performances).archive(destination, existing.performanceId)
                    }
            ) {
                is ActorStoreResult.Value -> sendEffect(ActorStudioDocumentEffect.Exit)
                is ActorStoreResult.Failure -> sendEffect(ActorStudioDocumentEffect.ShowError(result.message))
            }
        }
    }

    /** Validation failures are user-visible effects; coroutine cancellation still propagates. */
    private inline fun <T> storeOperation(block: () -> ActorStoreResult<T>): ActorStoreResult<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        ActorStoreResult.Failure(error.message ?: "Could not save performance", error)
    }
}
