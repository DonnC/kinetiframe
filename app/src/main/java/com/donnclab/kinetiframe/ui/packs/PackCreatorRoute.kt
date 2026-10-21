package com.donnclab.kinetiframe.ui.packs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.LibraryItemManifest
import com.donnclab.kinetiframe.data.actor.LibraryScope
import com.donnclab.kinetiframe.data.library.LibraryRepository
import com.donnclab.kinetiframe.data.library.PackArchiver
import com.donnclab.kinetiframe.data.library.PackItemEntry
import com.donnclab.kinetiframe.data.library.PackManifest
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.project.ProjectSaveDocument
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.ui.EditorScreen
import com.donnclab.kinetiframe.ui.components.dialogs.FullScreenDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The Pack Creator micro-editor route, built as a genuine reuse of the ordinary project-editing
 * machinery rather than a parallel canvas: a pack draft is, on disk, a completely normal
 * `projects/<id>/` directory ([ProjectManifest.isPackDraft] just keeps it out of the gallery),
 * opened through the exact same [EditorScreen] every real project uses, only with
 * [PackCreatorConfig] engaged -- one workspace the animator already knows how to use, instead of a
 * second drawing interface built from scratch.
 *
 * Flow for a brand-new pack ([editingItemId] null): pick a square canvas size -> a scratch draft
 * project is created at that size -> draw and tag frames in [EditorScreen] -> "Save Pack" flattens
 * every tagged frame (via a short-lived [FrameCacheManager] pointed at the draft's own frame
 * cache -- no live ViewModel needed, since those PNGs are permanent once cached), builds a
 * [PackManifest], saves it as a new PACK library item, and deletes the now-redundant scratch draft.
 *
 * Flow for reopening a saved pack ([editingItemId] set): the size picker is skipped entirely --
 * the same scratch-draft machinery is seeded with copies of that pack's own drawings and tags
 * (see [loadDraftForEdit]) so the animator lands exactly back where the pack left off, free to
 * redraw, retag, add or remove frames. "Save Pack" then overwrites that SAME library item
 * (matched by [editingItemId]) instead of creating a new one, so the pack keeps its identity
 * (and anything already shared as a `.kpack` stays a snapshot of the pre-edit version, same as
 * editing a project never rewrites a `.kframe` someone already has).
 *
 * This route only saves; it never touches [PackArchiver] or the share sheet. Sharing happens from
 * Home's Packs tab, so a pack stays private until the animator chooses to send it.
 *
 * A new pack is made in one sitting; unfinished new packs are not kept. Backing out before saving
 * ([onExit]) deletes the scratch draft. That is safe when editing too, because the draft is a
 * working copy and the library item is only replaced when Save Pack succeeds.
 */
@Composable
fun PackCreatorRoute(
    storageRoot: File,
    onExit: () -> Unit,
    editingItemId: String? = null,
    editingScope: LibraryScope = LibraryScope.PERSONAL,
    sourceProjectId: String? = null,
    onHome: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val files = remember(storageRoot) { ActorFileStore(storageRoot) }
    val projectRepository = remember(files) { ProjectRepository(files) }
    val libraryRepository = remember(files, context) {
        LibraryRepository(
            files,
            dao = com.donnclab.kinetiframe.data.project.db.AppDatabase.getInstance(context).libraryItemDao(),
        )
    }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var canvasDimension by remember { mutableStateOf<Int?>(null) }
    var draftId by remember { mutableStateOf<String?>(null) }
    var draftError by remember { mutableStateOf<String?>(null) }
    var tags by remember { mutableStateOf<Map<Int, PackFrameTag>>(emptyMap()) }
    var addPackFormState by remember { mutableStateOf(AddPackFormState()) }
    var packName by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    // Only set when editing -- preserves the original item's creation timestamp across the
    // overwrite, and is what tells savePack() to reuse [editingItemId] instead of minting a new id.
    var editingManifest by remember { mutableStateOf<LibraryItemManifest?>(null) }

    // Deliberately synchronous, not `scope.launch` -- this only ever runs from
    // DisposableEffect's onDispose, which Compose fires as part of tearing this composable's own
    // rememberCoroutineScope down; a coroutine launched on that same scope right there can be
    // cancelled before it ever deletes anything, silently defeating the whole point of this
    // cleanup. A pack draft is just a handful of small raster PNGs, so a brief blocking delete
    // here is a deliberate, safe trade-off, not an oversight.
    fun deleteDraft(id: String) {
        files.projectDirectory(id).deleteRecursively()
    }

    fun createDraft(dimension: Int) {
        canvasDimension = dimension
        scope.launch {
            val id = "pack-draft-${UUID.randomUUID()}"
            val time = System.currentTimeMillis()
            val stage = StageDimensions(dimension, dimension)
            val mainLayer = Layer(name = "Main Layer", id = "main")
            val document = ProjectSaveDocument(
                project = ProjectDocument(id = id, stage = stage, fps = 12, layers = listOf(mainLayer)),
                frameLayers = mapOf(0 to listOf(mainLayer)),
                totalFrames = 1,
            )
            val manifest = ProjectManifest(
                projectId = id,
                displayName = "Pack Draft",
                stage = stage,
                fps = 12,
                createdAtEpochMs = time,
                updatedAtEpochMs = time,
                totalFrames = 1,
                isPackDraft = true,
            )
            when (val saved = withContext(Dispatchers.IO) { projectRepository.save(manifest, document) }) {
                is ActorStoreResult.Value -> draftId = id
                is ActorStoreResult.Failure -> draftError = saved.message
            }
        }
    }

    // Reopens a previously saved pack: reads its manifest + PackManifest back, builds a fresh
    // scratch draft project sized to match, and copies every one of its drawings into that
    // draft's own frame cache under the same "main" layer id [EditorScreen]/[FrameCacheManager]
    // already expect -- so from EditorScreen's point of view this looks exactly like reopening
    // any other in-progress project, it just happens to have started from an existing pack's
    // content instead of a blank canvas.
    fun loadDraftForEdit(itemId: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val ioScope = this
                runCatching {
                    val (manifest, pack) = when (
                        val loaded = libraryRepository.loadPack(
                            itemId,
                            editingScope,
                            sourceProjectId,
                        )
                    ) {
                        is ActorStoreResult.Value -> loaded.value
                        is ActorStoreResult.Failure -> throw IllegalStateException(loaded.message, loaded.cause)
                    }
                    val sortedItems = pack.items.sortedBy { it.index }
                    val totalFrames = sortedItems.size.coerceAtLeast(1)
                    val id = "pack-draft-${UUID.randomUUID()}"
                    val time = System.currentTimeMillis()
                    val stage = StageDimensions(pack.canvasDimension, pack.canvasDimension)
                    val mainLayer = Layer(name = "Main Layer", id = "main")
                    val document = ProjectSaveDocument(
                        project = ProjectDocument(id = id, stage = stage, fps = 12, layers = listOf(mainLayer)),
                        frameLayers = mapOf(0 to listOf(mainLayer)),
                        totalFrames = totalFrames,
                    )
                    val draftManifest = ProjectManifest(
                        projectId = id,
                        displayName = "Pack Draft",
                        stage = stage,
                        fps = 12,
                        createdAtEpochMs = time,
                        updatedAtEpochMs = time,
                        totalFrames = totalFrames,
                        isPackDraft = true,
                    )
                    when (val saved = projectRepository.save(draftManifest, document)) {
                        is ActorStoreResult.Value -> Unit
                        is ActorStoreResult.Failure -> throw IllegalStateException(saved.message, saved.cause)
                    }
                    val cacheManager =
                        FrameCacheManager(cacheDir = File(files.projectDirectory(id), "frames"), scope = ioScope)
                    val loadedTags = mutableMapOf<Int, PackFrameTag>()
                    sortedItems.forEachIndexed { newIndex, entry ->
                        val sourceFile = libraryRepository.packItemFile(
                            itemId,
                            entry.file,
                            editingScope,
                            sourceProjectId,
                        )
                        val bitmap = android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath)
                            ?: throw IllegalStateException("One of this pack's drawings could not be read")
                        cacheManager.putFrame("main", newIndex, bitmap)
                        loadedTags[newIndex] = PackFrameTag(entry.name, entry.desc)
                    }
                    // A plain putFrame only guarantees the RAM cache -- force every seeded frame
                    // out to disk now so the very first EditorScreen composition (which may read
                    // straight from disk before this cache instance is even reachable again) sees
                    // a complete draft, not one that only half-loaded.
                    cacheManager.persistAllToDisk(listOf("main"), totalFrames)
                    Triple(id, manifest, loadedTags.toMap()) to pack.canvasDimension
                }
            }
            result.fold(
                onSuccess = { (idManifestTags, dimension) ->
                    val (id, manifest, loadedTags) = idManifestTags
                    editingManifest = manifest
                    packName = manifest.displayName
                    tags = loadedTags
                    canvasDimension = dimension
                    draftId = id
                },
                onFailure = { draftError = it.message ?: "Could not open this pack" },
            )
        }
    }

    LaunchedEffect(editingItemId) {
        editingItemId?.let { loadDraftForEdit(it) }
    }

    fun savePack(packName: String, totalFrames: Int) {
        val id = draftId ?: return
        val dimension = canvasDimension ?: return
        require(totalFrames > 0) { "A pack must contain at least one frame" }
        saving = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val ioScope = this
                runCatching {
                    val cacheManager =
                        FrameCacheManager(cacheDir = File(files.projectDirectory(id), "frames"), scope = ioScope)
                    // The Pack Creator's editor can add/remove frames after this scratch draft's
                    // initial ProjectSaveDocument was written. Its live timeline count, supplied
                    // by EditorScreen after flushing the cache, is therefore the only valid export
                    // range. The original document still has the draft's starting frame count.
                    val untagged = (0 until totalFrames).filter { (tags[it]?.name ?: "").isBlank() }
                    require(untagged.isEmpty()) {
                        "Every frame needs a name before you can save this pack (frame ${untagged.first() + 1} is untagged)"
                    }

                    val stagingDir = File(context.cacheDir, "pack-bundle-${UUID.randomUUID()}").apply { mkdirs() }
                    try {
                        val frameFiles = mutableMapOf<String, File>()
                        val items = mutableListOf<PackItemEntry>()
                        for (index in 0 until totalFrames) {
                            val bitmap = cacheManager.getFrame("main", index)
                                ?: throw IllegalStateException("Frame ${index + 1} has no drawing yet")
                            val fileName = "frame_%03d.png".format(index + 1)
                            val frameFile = File(stagingDir, fileName)
                            frameFile.outputStream().use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                            }
                            frameFiles[fileName] = frameFile
                            val tag = tags.getValue(index)
                            items +=
                                PackItemEntry(file = fileName, index = index, name = tag.name, desc = tag.description)
                        }
                        val packManifest = PackManifest(
                            packName = packName,
                            totalItems = items.size,
                            canvasDimension = dimension,
                            items = items,
                        ).validate()
                        // Editing a saved pack reuses its existing id and original creation time
                        // so it keeps its identity in place; a brand-new pack mints both fresh.
                        // exportPack overwrites the item's payload directory in place either way --
                        // any drawing that was renamed/removed since the last save (e.g. the pack
                        // shrank from 6 frames to 5) just leaves its old frame_00N.png unreferenced
                        // in that directory, harmless since only the frame files this manifest's
                        // own [PackItemEntry] list names are ever read back.
                        val itemId = editingManifest?.libraryItemId ?: "library-${UUID.randomUUID()}"
                        val nowEpochMs = editingManifest?.createdAtEpochMs ?: System.currentTimeMillis()
                        // Writing the PACK library item is the whole action; sharing happens
                        // from Home's Packs tab.
                        when (
                            val exported = libraryRepository.exportPack(
                                itemId = itemId,
                                displayName = packName,
                                packManifest = packManifest,
                                frameFiles = frameFiles,
                                scope = editingManifest?.scope ?: LibraryScope.PERSONAL,
                                projectId = editingManifest?.sourceProjectId,
                                tags = editingManifest?.tags ?: emptySet(),
                                nowEpochMs = nowEpochMs,
                            )
                        ) {
                            is ActorStoreResult.Value -> Unit
                            is ActorStoreResult.Failure -> throw IllegalStateException(exported.message, exported.cause)
                        }
                        projectRepository.delete(id)
                    } finally {
                        stagingDir.deleteRecursively()
                    }
                }
            }
            saving = false
            result.fold(
                onSuccess = {
                    draftId = null
                    onExit()
                },
                onFailure = { saveError = it.message ?: "Could not save this pack" },
            )
        }
    }

    when {
        canvasDimension == null && editingItemId == null -> FullScreenDialog(onDismiss = onExit) {
            AddPackScreen(
                state = addPackFormState,
                callbacks = AddPackCallbacks(
                    onNameChange = { addPackFormState = addPackFormState.copy(name = it) },
                    onDimensionChange = { addPackFormState = addPackFormState.copy(dimension = it) },
                    onBack = onExit,
                    onCreate = {
                        packName = addPackFormState.name
                        createDraft(addPackFormState.dimension)
                    },
                ),
            )
        }

        draftError != null -> AlertDialog(
            onDismissRequest = onExit,
            title = { Text("Pack Creator") },
            text = { Text(draftError.orEmpty()) },
            confirmButton = { TextButton(onClick = onExit) { Text("OK") } },
        )

        draftId == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        else -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.padding(padding)) {
                EditorScreen(
                    onBackClick = onExit,
                    onHomeClick = onHome,
                    projectId = draftId,
                    packCreatorConfig = PackCreatorConfig(
                        canvasDimension = canvasDimension ?: 512,
                        tags = tags,
                        onSetTag = { frameIndex, tag -> tags = tags + (frameIndex to tag) },
                        onSavePack = { totalFrames -> savePack(packName, totalFrames) },
                        onReplaceTags = { tags = it },
                    ),
                )
            }
        }
    }

    if (saving) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Saving pack...") },
            text = { CircularProgressIndicator() },
            confirmButton = {},
        )
    }
    saveError?.let { message ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text("Could not save this pack") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { saveError = null }) { Text("OK") } },
        )
    }

    // Unfinished drafts are not kept, so leaving the route deletes the scratch draft. In edit
    // mode the draft is a working copy, so the library item it was loaded from is untouched.
    DisposableEffect(Unit) {
        onDispose { draftId?.let { if (saveError == null) deleteDraft(it) } }
    }
}
