package com.donnclab.kinetiframe.studio.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.collection.LruCache
import com.donnclab.kinetiframe.common.coroutines.PendingJobs
import com.donnclab.kinetiframe.core.document.CelExposureIndex
import com.donnclab.kinetiframe.data.storage.DurableFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A disk-backed LRU cache for frame bitmaps.
 */
class FrameCacheManager(
    private val cacheDir: File,
    private val scope: CoroutineScope,
    private val maxRamBytes: Int = minOf(
        48L * 1024 * 1024,
        Runtime.getRuntime().maxMemory() / 8,
    ).toInt().coerceAtLeast(1),
) {
    private val cacheFilePrefix = "frame_v2_"
    private val defaultLayerId = "main"

    @Volatile
    private var clearing = false

    @Volatile
    private var cacheGeneration = 0
    private val pendingWrites = PendingJobs()
    private val versions = java.util.concurrent.ConcurrentHashMap<String, Long>()

    @Volatile private var linkedCels: Map<String, Map<Int, String>> = emptyMap()

    // Scope is deliberately transient and addressed to one exposure, never a global switch.
    @Volatile private var sharedEditTarget: Pair<String, Int>? = null
    fun exposures(): Map<String, Map<Int, String>> = linkedCels
    fun restoreExposures(exposures: Map<String, Map<Int, String>>, totalFrames: Int) {
        CelExposureIndex.validate(exposures, totalFrames)
        linkedCels = exposures.mapValues { (_, entries) -> entries.toMap() }
    }
    fun editLinked(layerId: String, frame: Int, allLinked: Boolean) {
        sharedEditTarget = if (allLinked) layerId to frame else null
    }
    fun resetEditScope() {
        sharedEditTarget = null
    }

    /** Caller serialises authoring operations. Replacing a range is explicit, never implicit
     * inheritance. One canonical cache key stores the held pixels regardless of range length. */
    suspend fun holdDrawing(layerId: String, first: Int, last: Int, totalFrames: Int) {
        require(first in 0 until totalFrames && last in first until totalFrames)
        val source = requireNotNull(getFrame(layerId, first)) { "This layer has no drawing to hold" }
        val cel = "cel-${java.util.UUID.randomUUID()}"
        val key = linkedKey(layerId, cel)
        // Keep an independent snapshot so cached rendering clients cannot mutate linked pixels.
        val copy = withContext(Dispatchers.Default) { source.copy(Bitmap.Config.ARGB_8888, true) }
        versions.merge(key, 1L, Long::plus)
        ramCache.put(key, copy)
        // Persist before publishing the references: a large source can exceed the RAM budget.
        awaitWrites()
        withContext(Dispatchers.IO) { diskFilesMutex.withLock { writeBitmap(key, copy) } }
        linkedCels = linkedCels + (layerId to CelExposureIndex.link(linkedCels[layerId].orEmpty(), first, last, cel))
        sharedEditTarget = null
    }

    suspend fun detachDrawing(layerId: String, frame: Int) {
        if (linkedCels[layerId]?.containsKey(frame) != true) return
        val source = getFrame(layerId, frame)
        val copy = withContext(Dispatchers.Default) { source?.copy(Bitmap.Config.ARGB_8888, true) }
        linkedCels = linkedCels + (layerId to linkedCels[layerId].orEmpty().minus(frame))
        sharedEditTarget = null
        if (copy != null) putFrame(layerId, frame, copy)
    }

    @Volatile private var writeFailure: Throwable? = null

    // Budget by bytes, not entries. A 1920x1080 ARGB frame is about 8 MB.
    private val ramCache = object : LruCache<String, Bitmap>(maxRamBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            // Clearing discards these frames; do not enqueue new writes while evictAll runs.
            if (evicted && !clearing) {
                saveToDisk(key, oldValue)
            }
        }
    }

    private val diskFilesMutex = Mutex()
    private val diskFiles = mutableSetOf<String>()

    init {
        cacheDir.mkdirs()
        // Rebuild the index after process recreation. This cache is still temporary,
        // but it must not silently forget files that survived a process death.
        cacheDir.listFiles { _, name -> name.startsWith(cacheFilePrefix) && name.endsWith(".png") }
            ?.map { it.name.removePrefix(cacheFilePrefix).removeSuffix(".png") }
            ?.let { diskFiles.addAll(it) }
    }

    private fun saveToDisk(cacheKey: String, bitmap: Bitmap) {
        val generation = cacheGeneration
        val version = versions[cacheKey]
        val job = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            diskFilesMutex.withLock {
                if (clearing || generation != cacheGeneration || versions[cacheKey] != version) return@withLock
                if (version == null && cacheKey in diskFiles) return@withLock
                try {
                    writeBitmap(cacheKey, bitmap)
                } catch (error: Exception) {
                    writeFailure = error
                }
            }
        }
        pendingWrites.register(job)
        job.start()
    }

    private fun writeBitmap(key: String, bitmap: Bitmap) {
        check(!bitmap.isRecycled) { "Cannot save recycled frame: $key" }
        DurableFile.write(File(cacheDir, getFileName(key))) { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode frame: $key" }
        }
        diskFiles.add(key)
    }

    private suspend fun awaitWrites() {
        pendingWrites.awaitIdle()
        writeFailure?.let { throw java.io.IOException("Frame storage failed; project was not saved", it) }
    }

    suspend fun getFrame(frameIndex: Int): Bitmap? = getFrame(defaultLayerId, frameIndex)

    suspend fun getFrame(layerId: String, frameIndex: Int): Bitmap? {
        val cacheKey = cacheKey(layerId, frameIndex)
        // 1. Check RAM first
        val cachedBitmap = ramCache.get(cacheKey)
        if (cachedBitmap != null) {
            return cachedBitmap
        }

        // 2. Check Disk
        awaitWrites()
        val hasOnDisk = diskFilesMutex.withLock { diskFiles.contains(cacheKey) }
        if (hasOnDisk) {
            val file = File(cacheDir, getFileName(cacheKey))
            if (file.exists()) {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    // Loading a disk-backed frame into RAM is a cache warm, not a move: the PNG
                    // stays on disk so a *different* FrameCacheManager instance pointed at this
                    // same directory (a fresh editor session, or an export run) still finds it.
                    ramCache.put(cacheKey, bitmap)
                    return bitmap
                }
            }
        }

        return null
    }

    fun putFrame(frameIndex: Int, bitmap: Bitmap) = putFrame(defaultLayerId, frameIndex, bitmap)

    fun putFrame(layerId: String, frameIndex: Int, bitmap: Bitmap) {
        // Default copy-on-write preserves ordinary frame-local editing. All-linked editing is
        // opt-in for this exact layer/frame; the drawing managers always supply a fresh bitmap.
        if (linkedCels[layerId]?.containsKey(frameIndex) == true && sharedEditTarget != (layerId to frameIndex)) {
            linkedCels = linkedCels + (layerId to linkedCels[layerId].orEmpty().minus(frameIndex))
        }
        val cacheKey = cacheKey(layerId, frameIndex)
        versions.merge(cacheKey, 1L, Long::plus)
        ramCache.put(cacheKey, bitmap)
        // Keep the last committed PNG until its replacement is ready. Deleting it asynchronously
        // raced with Save and could delete the PNG that Save had just written.
    }

    /**
     * Forces every frame this instance currently holds only in RAM out to disk, synchronously.
     * A plain [putFrame] does not do this -- it invalidates any stale disk copy and relies on
     * LRU eviction to ever write a fresh one, which a project small enough to fit entirely in the
     * RAM budget may never trigger. Call this once at Save time so the directory on disk is a
     * true, complete snapshot that a second instance (reopening the project, or an export run)
     * can rely on rather than an incidental side effect of memory pressure.
     */
    suspend fun persistAllToDisk(layerIds: Collection<String>, totalFrames: Int) = withContext(Dispatchers.IO) {
        awaitWrites()
        diskFilesMutex.withLock {
            val written = mutableSetOf<String>()
            for (layerId in layerIds) {
                for (frameIndex in 0 until totalFrames) {
                    val cacheKey = cacheKey(layerId, frameIndex)
                    if (!written.add(cacheKey)) continue
                    val bitmap = ramCache.get(cacheKey) ?: continue
                    writeBitmap(cacheKey, bitmap)
                }
            }
        }
    }

    /**
     * Initializes all frames for a new layer with transparent bitmaps.
     */
    suspend fun initLayer(layerId: String, totalFrames: Int, width: Int, height: Int) =
        withContext(Dispatchers.Default) {
            for (i in 0 until totalFrames) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                putFrame(layerId, i, bitmap)
            }
        }

    suspend fun clear() {
        cacheGeneration++
        clearing = true
        try {
            pendingWrites.awaitIdle()
            ramCache.evictAll()
            val filesToClean = diskFilesMutex.withLock {
                val current = diskFiles.toList()
                diskFiles.clear()
                current
            }
            withContext(Dispatchers.IO) {
                filesToClean.forEach { cacheKey ->
                    File(cacheDir, getFileName(cacheKey)).delete()
                }
                // Also remove legacy v1 files, including malformed frames generated
                // by the previous stroke renderer.
                cacheDir.listFiles { _, name -> name.startsWith("frame_") && name.endsWith(".png") }
                    ?.forEach { it.delete() }
            }
        } finally {
            versions.clear()
            linkedCels = emptyMap()
            sharedEditTarget = null
            writeFailure = null
            clearing = false
        }
    }

    /**
     * Deletes a frame and shifts subsequent frames left to fill the gap.
     */
    suspend fun deleteFrame(index: Int, totalFrames: Int) = deleteFrame(defaultLayerId, index, totalFrames)

    suspend fun deleteFrame(layerId: String, index: Int, totalFrames: Int) {
        awaitWrites()
        // Remove the target frame from memory and disk
        val targetKey = rawFrameKey(layerId, index)
        ramCache.remove(targetKey)
        withContext(Dispatchers.IO) {
            val file = File(cacheDir, getFileName(targetKey))
            if (file.exists()) file.delete()
        }
        diskFilesMutex.withLock { diskFiles.remove(targetKey) }

        // Shift all subsequent frames left
        for (i in index + 1 until totalFrames) {
            moveFrame(layerId, i, i - 1)
        }
        linkedCels = linkedCels + (layerId to CelExposureIndex.delete(linkedCels[layerId].orEmpty(), index))
        sharedEditTarget = null
    }

    /**
     * Shifts subsequent frames right and copies the content of the specified frame.
     */
    suspend fun duplicateFrame(index: Int, totalFrames: Int) = duplicateFrame(defaultLayerId, index, totalFrames)

    suspend fun duplicateFrame(layerId: String, index: Int, totalFrames: Int) {
        awaitWrites()
        val sourceBitmap = getFrame(layerId, index)
        // Shift all subsequent frames right, starting from the end
        for (i in totalFrames - 1 downTo index + 1) {
            moveFrame(layerId, i, i + 1)
        }

        // Copy content from index to index + 1
        linkedCels = linkedCels + (layerId to CelExposureIndex.insert(linkedCels[layerId].orEmpty(), index + 1))
        if (sourceBitmap != null) {
            // Put source back (getFrame removes it from disk/cache)
            // Create a deep copy for the new frame in background
            val copy = withContext(Dispatchers.Default) {
                sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            }
            putFrame(layerId, index + 1, copy)
        }
    }

    /**
     * Shifts subsequent frames right to make space for a new frame at the specified index.
     */
    suspend fun insertFrame(index: Int, totalFrames: Int) = insertFrame(defaultLayerId, index, totalFrames)

    suspend fun insertFrame(layerId: String, index: Int, totalFrames: Int) {
        awaitWrites()
        // Shift all subsequent frames right, starting from the end
        for (i in totalFrames - 1 downTo index) {
            moveFrame(layerId, i, i + 1)
        }
        linkedCels = linkedCels + (layerId to CelExposureIndex.insert(linkedCels[layerId].orEmpty(), index))
        sharedEditTarget = null
        // The frame at 'index' is now empty in both RAM and disk
    }

    private suspend fun moveFrame(layerId: String, fromIndex: Int, toIndex: Int) {
        val fromKey = rawFrameKey(layerId, fromIndex)
        val toKey = rawFrameKey(layerId, toIndex)
        awaitWrites()
        val bitmap = ramCache.remove(fromKey)
        ramCache.remove(toKey)
        versions.merge(fromKey, 1L, Long::plus)
        versions.merge(toKey, 1L, Long::plus)
        withContext(Dispatchers.IO) {
            diskFilesMutex.withLock {
                if (bitmap != null) writeBitmap(fromKey, bitmap)
                val fromFile = File(cacheDir, getFileName(fromKey))
                val toFile = File(cacheDir, getFileName(toKey))
                if (fromFile.isFile) {
                    java.nio.file.Files.move(
                        fromFile.toPath(),
                        toFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    )
                    diskFiles.add(toKey)
                } else {
                    check(!toFile.exists() || toFile.delete()) { "Could not clear empty frame storage" }
                    diskFiles.remove(toKey)
                }
                diskFiles.remove(fromKey)
            }
        }
        if (bitmap != null) ramCache.put(toKey, bitmap)
    }

    private fun cacheKey(layerId: String, frameIndex: Int): String =
        linkedCels[layerId]?.get(frameIndex)?.let { linkedKey(layerId, it) } ?: rawFrameKey(layerId, frameIndex)

    private fun linkedKey(layerId: String, cel: String) = CelExposureIndex.storageKey(layerId, cel)
    private fun rawFrameKey(layerId: String, frameIndex: Int): String = "${sanitizeLayerId(layerId)}_frame_$frameIndex"

    private fun sanitizeLayerId(layerId: String): String =
        layerId.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { defaultLayerId }

    private fun getFileName(cacheKey: String): String = "${cacheFilePrefix}$cacheKey.png"
}
