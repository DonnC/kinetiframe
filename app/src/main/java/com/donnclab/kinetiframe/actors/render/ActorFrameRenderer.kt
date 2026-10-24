package com.donnclab.kinetiframe.actors.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.domain.ActorPlacementMapper
import com.donnclab.kinetiframe.actors.domain.ResolveActorRenderUseCase
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.studio.render.ProjectFrameCompositor
import com.donnclab.kinetiframe.studio.render.SharedResourceCache
import java.io.File

/**
 * Resolves and draws actor placements onto a plain [android.graphics.Canvas]. The same math and
 * asset-loading rules serve three callers that must never drift apart: the live editor canvas, project video export
 * ([ProjectFrameCompositor]), and gallery thumbnail generation. This class knows nothing about
 * Compose -- it is deliberately callable from a headless export coroutine with no composition in
 * progress.
 */
class ActorFrameRenderer(
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
    private val files: DocumentFileStore,
    // Three full-body actors already use 48 active parts. Leave slot headroom for recent facial/
    // hand swaps to remain cached beyond the previous display snapshot. The
    // independent byte budget below remains authoritative on low-memory devices.
    cacheCapacity: Int = 96,
    cacheBytes: Long = minOf(24L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 16).coerceAtLeast(1),
) {
    data class Render(
        val zIndex: Int,
        val commands: List<PuppetRenderCommand>,
        val assets: Map<String, Bitmap>,
        val geometry: ActorSelectionGeometry.Object,
    )
    data class Diagnostic(val placementId: String, val message: String)
    data class Resolution(val render: Render? = null, val diagnostic: Diagnostic? = null)

    private val bitmapCache =
        SharedResourceCache<String, Bitmap>(cacheCapacity, cacheBytes) { it.allocationByteCount.toLong() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val revisionCache = ActorDocumentCache<com.donnclab.kinetiframe.core.actor.document.ActorRevision>()
    private val performanceCache =
        ActorDocumentCache<com.donnclab.kinetiframe.core.actor.document.PerformanceDocument>()
    private val resolver = ResolveActorRenderUseCase(
        actors,
        performances,
        loadPerformance = { project, id ->
            performanceCache.load(files.performanceFile(project, id)) { performances.load(project, id) }
        },
        loadRevision = { actor, id ->
            revisionCache.load(File(files.actorDirectory(actor), "revisions/$id.json")) {
                actors.loadRevision(actor, id)
            }
        },
    )
    private var lastRenders: List<Render>? = null
    private var orderedRenders: List<Render> = emptyList()

    /** Resolves every placement for one frame. Safe to call with placements outside their own
     *  active window -- [ResolveActorRenderUseCase]'s own "Placement is not active" outcome comes
     *  back as a bare [Resolution] with neither a render nor a diagnostic, not an error, so a
     *  caller that doesn't bother pre-filtering by frame range still gets correct behavior. */
    fun resolveAll(
        placements: Collection<ActorPlacementReference>,
        projectId: String,
        frame: Int,
        fps: Int,
    ): List<Resolution> = placements.map { resolve(it, projectId, frame, fps) }

    /** Draws already-resolved renders, back to front by [Render.zIndex], through [viewport] --
     *  pass an identity matrix for stage-space output (export, thumbnails); the live editor
     *  passes its own pan/zoom/rotation canvas transform instead. */
    fun draw(
        canvas: Canvas,
        viewport: Matrix,
        renders: List<Render>,
        selectedIds: Set<String> = emptySet(),
        preview: Matrix? = null,
    ) {
        canvas.save()
        canvas.concat(viewport)
        if (lastRenders !== renders) {
            lastRenders = renders
            orderedRenders = renders.sortedBy(Render::zIndex)
        }
        orderedRenders.forEach { render ->
            canvas.save()
            if (render.geometry.placement.placementId in selectedIds && preview != null) canvas.concat(preview)
            render.commands.forEach { command ->
                render.assets[command.assetId]?.takeUnless(Bitmap::isRecycled)?.let { bitmap ->
                    PuppetArtworkRenderer.draw(canvas, command, bitmap, paint)
                }
            }
            canvas.restore()
        }
        canvas.restore()
    }

    /** Drops cached revision artwork references. Call once when done with this renderer -- an
     *  export run or a thumbnail generation, same as the editor canvas disposing its own copy. */
    fun clear() {
        lastRenders = null
        orderedRenders = emptyList()
        bitmapCache.clear()
        revisionCache.clear()
        performanceCache.clear()
    }

    private fun resolve(placement: ActorPlacementReference, projectId: String, frame: Int, fps: Int): Resolution {
        val document = runCatching { ActorPlacementMapper.document(projectId, placement) }
            .getOrElse { return placementDiagnostic(placement, "Placement data is invalid") }
        val input = when (val result = resolver.execute(document, frame, fps)) {
            is StoreResult.Value -> result.value

            is StoreResult.Failure -> return if (result.message.startsWith("Placement is not active")) {
                Resolution()
            } else {
                placementDiagnostic(placement, result.message)
            }
        }
        if (input.performance.actorId != placement.actorId ||
            input.performance.actorRevisionId != placement.actorRevisionId
        ) {
            return placementDiagnostic(placement, "Performance no longer matches this actor revision")
        }
        val revision = input.revision
        val posedGraph = input.posedGraph
        val definition = PuppetDefinition(id = revision.revisionId, name = placement.label, sceneGraph = posedGraph)
        val commands = PuppetRenderPlan.create(
            definition,
            PuppetInstance(
                id = placement.placementId,
                definitionId = definition.id,
                transform = placement.transform,
            ),
        )
        val placementAffine = Matrix().apply { setValues(placement.selectionTransform.androidValues()) }
        commands.forEach { it.matrix.postConcat(placementAffine) }
        val savedBindings = com.donnclab.kinetiframe.actors.rig.model.ActorArtwork.bindings(revision.sceneGraph)
        val activeAssets = commands.map { it.assetId }.toSet()
        val assets = revision.artworkAssetPaths.mapNotNull { (partId, relativePath) ->
            // Resolve paths against the saved default/variant bindings, not a posed replacement:
            // a mouth swap must never load the default mouth PNG under the variant asset ID.
            val assetId = savedBindings[partId]?.assetId ?: return@mapNotNull null
            if (assetId !in activeAssets) return@mapNotNull null
            val file = File(files.actorDirectory(placement.actorId), relativePath).canonicalFile
            val root = files.actorDirectory(placement.actorId).canonicalFile
            if (!file.path.startsWith(root.path + File.separator)) return@mapNotNull null
            bitmapCache.getOrLoad("${placement.actorId}:${placement.actorRevisionId}:$relativePath") {
                BitmapFactory.decodeFile(file.path)
            }?.let { assetId to it }
        }.toMap()
        val missingParts = activeAssets - assets.keys
        if (missingParts.isNotEmpty()) {
            return placementDiagnostic(placement, "Bound artwork is unavailable for ${missingParts.size} part(s)")
        }
        if (commands.any { command ->
                command.mesh?.let { mesh ->
                    assets[command.assetId]?.let { it.width != mesh.width || it.height != mesh.height } ?: false
                } == true
            }
        ) {
            return placementDiagnostic(placement, "Continuous artwork dimensions changed; rebind the limb")
        }
        val quads = commands.mapNotNull { command ->
            assets[command.assetId]?.let { bitmap ->
                val points = PuppetArtworkRenderer.outline(command, bitmap.width, bitmap.height)
                // SelectionGeometry consumes convex quads. A bent mesh needs its complete extent,
                // not the rigid source rectangle (which can exclude a displaced wrist).
                if (command.mesh ==
                    null
                ) {
                    List(4) { index -> androidx.compose.ui.geometry.Offset(points[index * 2], points[index * 2 + 1]) }
                } else {
                    var left = Float.POSITIVE_INFINITY
                    var top = Float.POSITIVE_INFINITY
                    var right = Float.NEGATIVE_INFINITY
                    var bottom = Float.NEGATIVE_INFINITY
                    var index = 0
                    while (index < points.size) {
                        left = minOf(left, points[index])
                        right = maxOf(right, points[index])
                        top = minOf(top, points[index + 1])
                        bottom = maxOf(bottom, points[index + 1])
                        index += 2
                    }
                    listOf(
                        androidx.compose.ui.geometry.Offset(left, top),
                        androidx.compose.ui.geometry.Offset(right, top),
                        androidx.compose.ui.geometry.Offset(right, bottom),
                        androidx.compose.ui.geometry.Offset(left, bottom),
                    )
                }
            }
        }
        return Resolution(
            render = Render(placement.zIndex, commands, assets, ActorSelectionGeometry.Object(placement, quads)),
        )
    }

    private fun placementDiagnostic(placement: ActorPlacementReference, message: String) =
        Resolution(diagnostic = Diagnostic(placement.placementId, "${placement.label}: $message"))
}
