package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.engine.asset.BackgroundImageDecoder
import com.donnclab.kinetiframe.engine.asset.ProjectAssetManager
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.engine.model.ProjectAsset
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Owns the project's single persistent background image: set once, rendered behind every layer on
 * every frame by [com.donnclab.kinetiframe.ui.components.canvas.CanvasDrawingLayer] -- never duplicated
 * per-frame and never trimmed to a frame range, unlike an Actor placement. Two other models were
 * considered:
 *  - `LayerType.BACKGROUND` -- a raster layer type that would need per-frame content like any
 *    other layer (`LayerState.frameLayers`, `FrameCacheManager` keyed by layer+frame). Rejected:
 *    that would mean redrawing/replacing the background on every single frame, not "once."
 *  - `TrackGroupKind.BACKGROUND` -- a placed clip with a start/end frame range on the main
 *    timeline, mirroring an Actor placement. Rejected: an imported background applies to the
 *    whole scene. A frame-ranged background clip would be a separate feature.
 * Instead there is one optional reference (`ProjectDocument.backgroundAssetId`) plus one decoded
 * bitmap held in [ProjectAssetManager], read by the compositor on every frame. It needs no
 * per-frame storage and no timeline UI.
 */
class BackgroundManager(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val assets: ProjectAssetManager,
    private val storageDir: File,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val id: () -> String = { UUID.randomUUID().toString() },
    private val onError: (String) -> Unit = {},
    private val decode: (File) -> Bitmap = BackgroundImageDecoder::decode,
    private val pendingOperations: PendingOperations = PendingOperations(),
) {
    private var generation = 0L

    /** Metadata is durable; the runtime cache is not. Rehydrate once on document creation. */
    fun restore() = scope.launch {
        val project = getState().project
        val assetId = project.backgroundAssetId ?: return@launch
        if (assets.getAsset(assetId) != null) return@launch
        val request = generation
        var bitmap: Bitmap? = null
        var published = false
        try {
            val path = requireNotNull(project.assets[assetId]?.sourcePath) { "Saved background has no image file." }
            withContext(dispatcher) { bitmap = decode(File(path)) }
            if (request == generation && getState().project.backgroundAssetId == assetId) {
                assets.registerAsset(assetId, requireNotNull(bitmap))
                published = true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: OutOfMemoryError) {
            if (request ==
                generation
            ) {
                onError("Not enough memory to load the saved background image.")
            }
        } catch (error: Exception) {
            if (request ==
                generation
            ) {
                onError(error.message ?: "Could not load the saved background image.")
            }
        } finally {
            // Only unpublished bitmaps are safe to recycle explicitly.
            if (!published) bitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }
    fun handle(intent: StudioIntent.Background) = when (intent) {
        is StudioIntent.Background.SetImage -> setImage(intent.bitmap)

        StudioIntent.Background.ToggleVisibility -> updateState { state ->
            if (state.project.backgroundAssetId == null) {
                state
            } else {
                state.copy(canvas = state.canvas.copy(backgroundVisible = !state.canvas.backgroundVisible))
            }
        }

        StudioIntent.Background.Remove -> remove()
    }

    private fun setImage(bitmap: Bitmap) {
        val request = ++generation
        val assetId = "background-${id()}"
        val project = getState().project
        val previousAssetId = project.backgroundAssetId
        pendingOperations.launch(scope, dispatcher) {
            storageDir.mkdirs()
            val file = File(storageDir, "$assetId.png")
            try {
                DurableFile.write(file) { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Could not encode background image" }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                withContext(Dispatchers.Main) {
                    if (request ==
                        generation
                    ) {
                        onError("Background image was not saved: ${error.message}")
                    }
                }
                return@launch
            }
            var published = false
            withContext(Dispatchers.Main) {
                if (request != generation) return@withContext
                assets.registerAsset(assetId, bitmap)
                updateState { state ->
                    val asset = ProjectAsset(
                        id = assetId,
                        kind = AssetKind.IMPORTED_IMAGE,
                        sourcePath = file.absolutePath,
                        width = bitmap.width,
                        height = bitmap.height,
                    )
                    val withoutPrevious = previousAssetId?.let { state.project.assets - it } ?: state.project.assets
                    state.copy(
                        project = state.project.copy(
                            assets = withoutPrevious + (assetId to asset),
                            backgroundAssetId = assetId,
                        ),
                        canvas = state.canvas.copy(backgroundVisible = true),
                    )
                }
                published = true
            }
            if (!published) {
                file.delete()
                return@launch
            }
            // Drop previous cache ownership after publishing the replacement. A render already
            // in flight may keep its own bitmap reference until Android has finished drawing it.
            previousAssetId?.let { assets.releaseAsset(it) }
            // The last committed project may still name the old file until the next Save.
            // Keep immutable source versions on disk; deleting the project removes them together.
        }
    }

    private fun remove() {
        generation++
        val project = getState().project
        val previousAssetId = project.backgroundAssetId ?: return
        updateState { state ->
            state.copy(
                project = state.project.copy(
                    assets = state.project.assets - previousAssetId,
                    backgroundAssetId = null,
                ),
                canvas = state.canvas.copy(backgroundVisible = true),
            )
        }
        scope.launch(dispatcher) {
            assets.releaseAsset(previousAssetId)
        }
    }
}
