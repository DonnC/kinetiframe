package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformanceLimits
import com.donnclab.kinetiframe.data.actor.RootMotionPoint
import com.donnclab.kinetiframe.data.actor.RotationCurve
import com.donnclab.kinetiframe.data.actor.RotationCurvePoint
import com.donnclab.kinetiframe.domain.actor.PerformanceAuthoring
import com.donnclab.kinetiframe.domain.actor.RecordedContactBends
import com.donnclab.kinetiframe.domain.actor.RecordedMotionCurves
import com.donnclab.kinetiframe.domain.actor.RecordedRootMotionCurves
import com.donnclab.kinetiframe.engine.model.PuppetInstance
import com.donnclab.kinetiframe.engine.rigging.math.ActorMotionPose
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.model.ActorArtwork
import com.donnclab.kinetiframe.engine.rigging.model.ActorArtworkKey
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseTrack
import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.ui.PerformanceTakePhase
import com.donnclab.kinetiframe.ui.StudioEffect
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Transient take transaction. No project mutations while capturing; review is reversible and
 * Keep creates one undo checkpoint. A monotonic clock, not pointer frequency, owns timing.
 */
class ActorPerformanceRecorder(
    private val scope: CoroutineScope,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val checkpoint: () -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
    private val clockMicros: () -> Long = { System.nanoTime() / 1000 },
    private val awaitFrame: suspend () -> Unit = { delay(16) },
) {
    private var original: PuppetInstance? = null
    private var originalDuration = 0L
    private var start = 0L
    private var startedAt = 0L
    private var end = 0L
    private var ticker: Job? = null
    private var baseline: SceneGraph? = null
    private var restGraph: SceneGraph? = null
    private val heldRoots = mutableMapOf<String, RootTranslation>()
    private val rootSamples = mutableMapOf<String, MutableList<RootMotionPoint>>()
    private val takeEnd get() = minOf(start + PerformanceLimits.MAX_TAKE_MICROS, MAX_TIME)
    private val held = mutableMapOf<String, Float>()
    private val samples = mutableMapOf<String, MutableList<RotationCurvePoint>>()
    private var artwork = emptyList<ActorArtworkKey>()
    val active get() = original != null

    fun handle(intent: StudioIntent.ActorMotion): Boolean {
        when (intent) {
            is StudioIntent.ActorMotion.SetTakeMoveCharacter -> updateState {
                it.copy(actor = it.actor.copy(takeMoveCharacter = intent.value))
            }

            StudioIntent.ActorMotion.StartTake -> arm()

            StudioIntent.ActorMotion.StopTake -> finish()

            StudioIntent.ActorMotion.KeepTake -> keep()

            StudioIntent.ActorMotion.Retake -> {
                cancel()
                arm()
            }

            StudioIntent.ActorMotion.CancelTake -> cancel()

            is StudioIntent.ActorMotion.SetTakeSmoothness -> {
                if (!intent.value.isFinite()) return true
                updateState { it.copy(actor = it.actor.copy(takeSmoothness = intent.value.coerceIn(0f, 1f))) }
                if (getState().actor.takePhase == PerformanceTakePhase.REVIEW) publishReview()
            }

            else -> return false
        }
        return true
    }

    private fun arm() {
        if (active) return
        val state = getState()
        val actor = state.activeActor() ?: return
        if (state.scene.studioMode != StudioMode.ANIMATE_IK) return
        if (state.actor.presetPreview != null) {
            sendEffect(StudioEffect.ShowError("Apply the preset before recording a take."))
            return
        }
        if (state.actor.previewTimeMicros >= MAX_TIME) {
            sendEffect(
                StudioEffect.ShowError("Move the playhead earlier to record. Performances can be up to two minutes."),
            )
            return
        }
        original = actor
        originalDuration = state.actor.performanceDurationMicros
        start = state.actor.previewTimeMicros
        end = start
        artwork = emptyList()
        restGraph = state.actorDefinition()?.sceneGraph
        baseline = state.copy(actor = state.actor.copy(previewGraph = null)).actorDefinition(true)?.sceneGraph
        updateState {
            it.copy(
                tool = it.tool.copy(activeTool = com.donnclab.kinetiframe.engine.model.DrawingTool.RIG_SELECT),
                actor = it.actor.copy(
                    takePhase = PerformanceTakePhase.ARMED,
                    takeStartMicros = start,
                    previewPlaying = false,
                    previewGraph = null,
                ),
            )
        }
    }

    /** Runs before the normal IK drag; untouched channels are evaluated at the current clock. */
    fun beforeDrag(intent: StudioIntent.Actor.Drag): Boolean {
        if (!active) return false
        if (getState().actor.takePhase == PerformanceTakePhase.REVIEW) return true
        begin()
        tick()
        return false
    }

    fun afterDrag() {
        if (getState().actor.takePhase != PerformanceTakePhase.RECORDING) return
        val graph = getState().actor.previewGraph ?: return
        val base = baseline ?: return
        val rest = restGraph ?: return
        val roots = ActorPoseTrack.capture(end, graph, rest).rootTranslations
        val previousRoots = ActorPoseTrack.capture(end, base, rest).rootTranslations
        roots.forEach { (id, value) ->
            val old = previousRoots.getValue(id)
            if (kotlin.math.abs(value.x - old.x) + kotlin.math.abs(value.y - old.y) > .001f) {
                heldRoots[id] = value
                rootSamples.getOrPut(id) {
                    val initial =
                        originalGraph(start)?.let { ActorPoseTrack.capture(start, it, rest).rootTranslations[id] }
                            ?: old
                    mutableListOf(RootMotionPoint(start, initial.x, initial.y)).also {
                        if (end > start) it.add(RootMotionPoint(end, old.x, old.y))
                    }
                }
            }
        }
        graph.bones.forEach { (id, bone) ->
            val old = base.bones[id]?.localTransform?.rotationDegrees ?: return@forEach
            val angle = bone.localTransform.rotationDegrees
            if (kotlin.math.abs(CompiledMotionCurve.shortestAngle(angle - old)) > .001f) {
                held[id] = angle
                samples.getOrPut(id) {
                    val source = requireNotNull(original)
                    val atStart = getState().copy(
                        project = getState().project.copy(
                            puppetInstances =
                            getState().project.puppetInstances + (source.id to source),
                        ),
                        actor = getState().actor.copy(previewTimeMicros = start, previewGraph = null),
                    ).actorDefinition(true)?.sceneGraph
                        ?.bones?.get(id)?.localTransform?.rotationDegrees ?: old
                    mutableListOf(RotationCurvePoint(start, atStart)).also {
                        if (end > start) it.add(RotationCurvePoint(end, old))
                    }
                }
            }
        }
    }

    fun release() {
        if (getState().actor.takePhase == PerformanceTakePhase.RECORDING) sample(force = true)
    }

    fun selectArtwork(intent: StudioIntent.Actor.SelectArtwork) {
        if (getState().actor.takePhase == PerformanceTakePhase.REVIEW) return
        val bone = getState().actorDefinition()?.sceneGraph?.bones?.get(intent.partId) ?: return
        if (intent.variantId.isNotEmpty() && intent.variantId !in bone.artworkVariants) return
        begin()
        tick()
        val time = getState().actor.previewTimeMicros
        if (artwork.size >= RotationCurve.MAX_POINTS && artwork.none { it.timeMicros == time }) {
            sendEffect(StudioEffect.ShowError("This performance has reached its artwork key limit."))
            return
        }
        val previous = artwork
        artwork = ActorArtwork.put(artwork, time, intent.partId, intent.variantId)
        try {
            mergedArtwork()
        } catch (error: IllegalArgumentException) {
            artwork = previous
            sendEffect(StudioEffect.ShowError(error.message ?: "Artwork key limit reached"))
            return
        }
        tick()
    }

    private fun begin() {
        if (getState().actor.takePhase != PerformanceTakePhase.ARMED) return
        startedAt = clockMicros()
        updateState { it.copy(actor = it.actor.copy(takePhase = PerformanceTakePhase.RECORDING)) }
        ticker = scope.launch {
            while (isActive && getState().actor.takePhase == PerformanceTakePhase.RECORDING) {
                awaitFrame()
                if (getState().activeActor()?.id != original?.id ||
                    getState().actorDefinition()?.sceneGraph != restGraph ||
                    getState().scene.studioMode != StudioMode.ANIMATE_IK
                ) {
                    cancel()
                    break
                }
                tick()
                sample()
                if (end >= takeEnd) {
                    finish()
                    break
                }
            }
        }
    }

    private fun tick() {
        if (getState().actor.takePhase != PerformanceTakePhase.RECORDING) return
        end = (start + (clockMicros() - startedAt).coerceAtLeast(1L)).coerceAtMost(takeEnd)
        val state = getState()
        val source = original ?: return
        val baseState = state.copy(
            project = state.project.copy(
                puppetInstances = state.project.puppetInstances +
                    (source.id to source.copy(artworkKeys = mergedArtwork())),
            ),
            actor = state.actor.copy(previewTimeMicros = end, previewGraph = null),
        )
        val graph = baseState.actorDefinition(true)?.sceneGraph ?: return
        baseline = graph
        val posed = graph.copy(
            bones = graph.bones.mapValues { (id, bone) ->
                val root = heldRoots[id]
                val rest = restGraph?.bones?.get(id)?.localTransform
                bone.withRest(
                    localTransform = bone.localTransform.copy(
                        rotationDegrees = held[id] ?: bone.localTransform.rotationDegrees,
                        translation = com.donnclab.kinetiframe.core.geometry.Point2(
                            if (root != null && rest != null) rest.x + root.x else bone.localTransform.x,
                            if (root != null && rest != null) rest.y + root.y else bone.localTransform.y,
                        ),
                    ),
                )
            },
        )
        val contacts = ActorMotionPose.followBendSide(posed, ActorPoseTrack.contacts(source.poseKeys, end))
        val constrained = ActorMotionPose.apply(posed, emptyMap(), emptyMap(), contacts)
        updateState {
            it.copy(
                actor = it.actor.copy(
                    previewTimeMicros = end,
                    performanceDurationMicros = maxOf(originalDuration, end),
                    previewGraph = constrained,
                    previewDefinitionId = state.actorDefinition()?.id,
                ),
            )
        }
    }

    private fun sample(force: Boolean = false) {
        rootSamples.forEach { (id, points) ->
            val value = heldRoots[id] ?: return@forEach
            if (end <= points.last().timeMicros) return@forEach
            val nextBucket = (end - start) / SAMPLE_INTERVAL > (points.last().timeMicros - start) / SAMPLE_INTERVAL
            if (force || nextBucket) {
                val point = RootMotionPoint(end, value.x, value.y)
                if (!nextBucket && points.size > 1) {
                    points[points.lastIndex] = point
                } else if (points.size < RotationCurve.MAX_POINTS) {
                    points.add(point)
                }
            }
        }
        samples.forEach { (id, points) ->
            val value = held[id] ?: return@forEach
            if (end <= points.last().timeMicros) return@forEach
            val nextBucket = (end - start) / SAMPLE_INTERVAL > (points.last().timeMicros - start) / SAMPLE_INTERVAL
            if (force || nextBucket) {
                // Pointer-up may arrive more often than the capture rate. Coalesce within a
                // fixed clock bucket, never erase earlier samples to make room at the cap.
                val point = RotationCurvePoint(end, CompiledMotionCurve.shortestAngle(value))
                if (!nextBucket && points.size > 1) {
                    points[points.lastIndex] = point
                } else if (points.size < RotationCurve.MAX_POINTS) {
                    points.add(point)
                }
            }
        }
    }

    private fun finish() {
        if (!active) return
        if (getState().actor.takePhase == PerformanceTakePhase.ARMED) {
            cancel()
            return
        }
        if (getState().actor.takePhase != PerformanceTakePhase.RECORDING) return
        tick()
        sample(force = true)
        ticker?.cancel()
        ticker = null
        if (samples.isEmpty() && rootSamples.isEmpty() && artwork.isEmpty()) {
            cancel()
            return
        }
        publishReview()
    }

    private fun candidate(): PuppetInstance {
        val source = requireNotNull(original)
        val curves = source.rotationCurves.ifEmpty {
            PerformanceAuthoring.rotationCurves(source.poseKeys, maxOf(originalDuration, end), source.poseInterpolation)
        }
        val result = RecordedMotionCurves.merge(
            curves,
            samples,
            start,
            end,
            maxOf(originalDuration, end),
            getState().actor.takeSmoothness,
        )
        require(result.size <= PerformanceDocument.MAX_ROTATION_CURVES) {
            "Too many recorded parts in this performance."
        }
        val previousRoots = source.rootMotionCurves.ifEmpty {
            if (rootSamples.isEmpty()) {
                emptyList()
            } else {
                PerformanceAuthoring.rootCurves(
                    source.poseKeys,
                    maxOf(originalDuration, end),
                    source.poseInterpolation,
                )
            }
        }
        val roots = RecordedRootMotionCurves.merge(
            previousRoots,
            rootSamples,
            start,
            end,
            maxOf(originalDuration, end),
            getState().actor.takeSmoothness,
        )
        require(roots.size <= 16) { "Too many recorded roots in this performance." }
        val keys = RecordedContactBends.merge(
            source.poseKeys,
            requireNotNull(restGraph),
            result,
            samples.keys,
            samples.values.flatMap { points -> points.map { it.timeMicros } },
            start,
            end,
            maxOf(originalDuration, end),
        )
        require(keys.size <= RotationCurve.MAX_POINTS) { "Too many contact keys in this performance." }
        return source.copy(
            rotationCurves = result,
            rootMotionCurves = roots,
            poseKeys = keys,
            artworkKeys = mergedArtwork(),
        )
    }

    private fun mergedArtwork(): List<ActorArtworkKey> {
        val touched = artwork.flatMap { it.selections.keys }.toSet()
        var result = original?.artworkKeys.orEmpty().mapNotNull { key ->
            val selections = if (key.timeMicros in start..end) key.selections - touched else key.selections
            if (selections.isEmpty()) null else key.copy(selections = selections)
        }
        artwork.forEach { key ->
            key.selections.forEach { (part, variant) ->
                result =
                    ActorArtwork.put(result, key.timeMicros, part, variant)
            }
        }
        require(result.size <= RotationCurve.MAX_POINTS) { "Too many artwork keys in this performance." }
        return result
    }

    private fun publishReview() {
        try {
            val take = candidate()
            updateState {
                it.copy(
                    project = it.project.copy(
                        puppetInstances =
                        it.project.puppetInstances + (take.id to take),
                    ),
                    actor = it.actor.copy(
                        takePhase = PerformanceTakePhase.REVIEW,
                        takeEndMicros = end,
                        previewTimeMicros = start,
                        previewGraph = null,
                        previewPlaying = false,
                    ),
                )
            }
        } catch (error: IllegalArgumentException) {
            cancel()
            sendEffect(StudioEffect.ShowError(error.message ?: "This take exceeded the performance limit."))
        }
    }

    private fun keep() {
        if (getState().actor.takePhase != PerformanceTakePhase.REVIEW) return
        val take = candidate()
        val duration = maxOf(originalDuration, end)
        restore()
        checkpoint()
        updateState {
            it.copy(
                project = it.project.copy(puppetInstances = it.project.puppetInstances + (take.id to take)),
                actor = it.actor.copy(performanceDurationMicros = duration, previewTimeMicros = start),
            )
        }
        clear()
    }

    private fun restore() {
        val source = original ?: return
        updateState {
            it.copy(
                project = it.project.copy(
                    puppetInstances =
                    it.project.puppetInstances + (source.id to source),
                ),
                actor = it.actor.copy(
                    takePhase = PerformanceTakePhase.IDLE,
                    performanceDurationMicros = originalDuration,
                    previewTimeMicros = start,
                    previewGraph = null,
                    previewPlaying = false,
                ),
            )
        }
    }
    fun cancel() {
        ticker?.cancel()
        ticker = null
        restore()
        clear()
    }
    private fun originalGraph(time: Long): SceneGraph? {
        val state = getState()
        val source = original ?: return null
        return state.copy(
            project = state.project.copy(
                puppetInstances =
                state.project.puppetInstances + (source.id to source),
            ),
            actor = state.actor.copy(previewTimeMicros = time, previewGraph = null),
        ).actorDefinition(true)?.sceneGraph
    }
    private fun clear() {
        original = null
        baseline = null
        restGraph = null
        held.clear()
        samples.clear()
        heldRoots.clear()
        rootSamples.clear()
        artwork =
            emptyList()
    }
    companion object {
        const val MAX_TIME = PerformanceLimits.MAX_DURATION_MICROS
        const val SAMPLE_INTERVAL = 50_000L
    }
}
