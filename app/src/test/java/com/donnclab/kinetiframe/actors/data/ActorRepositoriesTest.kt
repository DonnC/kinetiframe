package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.actors.data.db.ActorDao
import com.donnclab.kinetiframe.actors.data.db.ActorEntity
import com.donnclab.kinetiframe.actors.data.db.PerformanceDao
import com.donnclab.kinetiframe.actors.data.db.PerformanceEntity
import com.donnclab.kinetiframe.actors.domain.PerformanceCompatibility
import com.donnclab.kinetiframe.actors.domain.ResolveActorRenderUseCase
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurvePoint
import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ActorRepositoriesTest {
    @TempDir lateinit var root: File

    @Test fun `saved revision cannot be overwritten but actor rename remains possible`() {
        val repository = ActorRepository(ActorFileStore(root))
        val original = actorRevision()
        requireSuccess(repository.save(actorManifest(), original))
        val changed = original.copy(createdAtEpochMs = 2)
        assertTrue(repository.save(actorManifest(displayName = "Wrong"), changed) is ActorStoreResult.Failure)
        assertEquals(original, requireSuccess(repository.loadRevision(original.actorId, original.revisionId)))
        assertEquals("Alice", requireSuccess(repository.load(original.actorId)).first.displayName)
        requireSuccess(repository.rename(original.actorId, "Renamed", 3))
        assertEquals(original, requireSuccess(repository.load(original.actorId)).second)
    }

    @Test fun `failed revision write leaves manifest pointing at the previous complete revision`() {
        val files = ActorFileStore(root)
        val repository = ActorRepository(files)
        requireSuccess(repository.save(actorManifest(), actorRevision()))
        // Make the temporary-file destination unwritable without relying on OS permissions.
        File(files.actorDirectory("actor-alice"), "revisions/rev-next.json").mkdirs()
        assertTrue(
            repository.saveRevision(
                "actor-alice",
                actorRevision().sceneGraph,
                "rev-next",
                2,
            ) is ActorStoreResult.Failure,
        )
        assertEquals("rev-alice-1", requireSuccess(repository.load("actor-alice")).first.currentRevisionId)
    }

    @Test fun `explicit empty artwork removes paths while omitted artwork preserves them`() {
        val repository = ActorRepository(ActorFileStore(root))
        val original = actorRevision().copy(artworkAssetPaths = mapOf("head" to "head.png"))
        requireSuccess(repository.save(actorManifest(), original))
        val retained = requireSuccess(repository.saveRevision("actor-alice", original.sceneGraph, "rev-next", 2)).second
        assertEquals(original.artworkAssetPaths, retained.artworkAssetPaths)
        val cleared = requireSuccess(
            repository.saveRevision("actor-alice", original.sceneGraph, "rev-empty", 3, emptyMap()),
        ).second
        assertTrue(cleared.artworkAssetPaths.isEmpty())
        assertEquals(original, requireSuccess(repository.loadRevision("actor-alice", original.revisionId)))
    }

    @Test fun `revision loaded under the wrong file identity is rejected`() {
        val files = ActorFileStore(root)
        val repository = ActorRepository(files)
        requireSuccess(repository.save(actorManifest(), actorRevision()))
        val directory = files.actorDirectory("actor-alice")
        File(directory, "revisions/rev-alice-1.json").copyTo(File(directory, "revisions/rev-wrong.json"))
        assertTrue(repository.loadRevision("actor-alice", "rev-wrong") is ActorStoreResult.Failure)
    }

    @Test fun `actor revision round trip is keyed by stable IDs not display name`() {
        val repository = ActorRepository(ActorFileStore(root))
        val manifest = actorManifest(displayName = "Alice One")
        val revision = actorRevision()

        val saved = repository.save(manifest, revision)
        assertTrue(saved is ActorStoreResult.Value<*>)
        val loaded = requireSuccess(repository.load("actor-alice"))

        assertEquals("Alice One", loaded.first.displayName)
        assertEquals("rev-alice-1", loaded.second.revisionId)
        assertEquals(
            ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd).bones.keys,
            loaded.second.sceneGraph.bones.keys,
        )
    }

    @Test fun `malformed manifest is surfaced as a typed storage failure`() {
        val store = ActorFileStore(root)
        val manifest = File(store.actorDirectory("actor-broken"), "manifest.json")
        manifest.parentFile!!.mkdirs()
        manifest.writeText("not json")

        assertEquals(emptyList<ActorManifest>(), requireSuccess(ActorRepository(store).list()))
        assertTrue(ActorRepository(store).load("actor-broken") is ActorStoreResult.Failure)
    }

    @Test fun `unsupported contract versions and missing required human parts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { actorManifest().copy(schemaVersion = 99).validate() }
        val store = ActorFileStore(root)
        val incomplete = actorRevision().copy(
            sceneGraph = actorRevision().sceneGraph.copy(
                bones =
                actorRevision().sceneGraph.bones - "head",
            ),
        )
        assertTrue(ActorRepository(store).save(actorManifest(), incomplete) is ActorStoreResult.Failure)
    }

    @Test fun `stale interrupted-write temporary file never replaces the durable document`() {
        val store = ActorFileStore(root)
        val file = File(store.actorDirectory("actor-alice"), "manifest.json")
        store.writeAtomically(file, "durable")
        File(file.parentFile, ".${file.name}.tmp").writeText("interrupted")

        assertEquals("durable", store.readOrNull(file))
    }

    @Test fun `performance references to missing immutable parts are rejected`() {
        val revision = actorRevision()
        val document = performance("performance-wave", "actor-alice").copy(
            actorRevisionId = "rev-alice-1",
            poseKeys = listOf(
                ActorPoseKey(0, mapOf("missing-part" to 10f)),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) { PerformanceCompatibility.validate(document, revision) }
    }

    @Test fun `performance is project scoped and filters by actor`() {
        val repository = PerformanceRepository(ActorFileStore(root))
        val alice = performance("perf-alice", "actor-alice")
        val other = performance("perf-other", "actor-other")
        repository.save(alice)
        repository.save(other)

        val values = requireSuccess(repository.list("project-film", "actor-alice"))
        assertEquals(listOf("perf-alice"), values.map { it.performanceId })
    }

    @Test fun `performance names are unique per actor within a project`() {
        val repository = PerformanceRepository(ActorFileStore(root))
        requireSuccess(repository.save(performance("perf-wave", "actor-alice").copy(displayName = "Wave")))
        val duplicate = performance("perf-wave-copy", "actor-alice").copy(displayName = "wave")
        assertTrue(repository.save(duplicate) is ActorStoreResult.Failure)
        // The same human-readable name remains valid for a different actor source.
        requireSuccess(repository.save(performance("perf-other", "actor-other").copy(displayName = "Wave")))
    }

    @Test fun `unknown revision part artwork is rejected`() {
        val revision = actorRevision().copy(artworkAssetPaths = mapOf("not-a-part" to "art.png"))
        val error = runCatching { revision.validate() }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("unknown actor part"))
    }

    @Test fun `document artwork paths cannot escape an actor or library root`() {
        val actorRoot = File(root, "actor")
        assertThrows(IllegalArgumentException::class.java) {
            ActorPathPolicy.resolveWithin(actorRoot, "../outside.png", "Artwork")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActorPathPolicy.resolveWithin(actorRoot, "", "Artwork")
        }
        assertEquals(
            File(actorRoot, "revisions/rev-1/artwork/hand.png").canonicalFile,
            ActorPathPolicy.resolveWithin(actorRoot, "revisions/rev-1/artwork/hand.png", "Artwork"),
        )
    }

    @Test fun `actor thumbnail resolves persisted artwork deterministically`() {
        val store = ActorFileStore(root)
        val repository = ActorRepository(store)
        val path = "revisions/rev-alice-1/artwork/hand.png"
        val revision = actorRevision().copy(artworkAssetPaths = mapOf("left_hand" to path))
        val artwork = File(store.actorDirectory("actor-alice"), path)
        artwork.parentFile!!.mkdirs()
        artwork.writeBytes(byteArrayOf(1))
        requireSuccess(repository.save(actorManifest(), revision))

        assertEquals(artwork.canonicalPath, requireSuccess(repository.thumbnailPath("actor-alice")))
    }

    @Test fun `performance keys must be ordered and within duration`() {
        val invalid = performance("perf-alice", "actor-alice").copy(
            poseKeys = listOf(
                ActorPoseKey(100, emptyMap()),
                ActorPoseKey(100, emptyMap()),
            ),
        )
        assertTrue(runCatching { invalid.validate() }.exceptionOrNull() != null)
    }

    @Test fun `performance curve payloads have bounded deterministic limits`() {
        val excessivePoints = List(RotationCurve.MAX_POINTS + 1) { index ->
            RotationCurvePoint(index.toLong(), 0f)
        }
        val invalid = performance("performance-wave", "actor-alice").copy(
            rotationCurves = listOf(
                RotationCurve("head", points = excessivePoints),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) { invalid.validate() }
    }

    @Test fun `placement resolves the pinned historical actor revision`() {
        val store = ActorFileStore(root)
        val actors = ActorRepository(store)
        val performance = PerformanceRepository(store)
        actors.save(actorManifest(), actorRevision())
        performance.save(performance("performance-wave", "actor-alice").copy(actorRevisionId = "rev-alice-1"))
        val result = ResolveActorRenderUseCase(actors, performance).execute(
            ActorPlacement(
                placementId = "placement-1",
                projectId = "project-film",
                performanceId = "performance-wave",
                startFrame = 0,
                durationFrames = 12,
            ),
            frame = 1,
            fps = 12,
        )
        assertEquals("rev-alice-1", requireSuccess(result).revision.revisionId)
    }

    @Test fun `durable placement resolution restarts at each repeat`() {
        val store = ActorFileStore(root)
        val actors = ActorRepository(store)
        val performances = PerformanceRepository(store)
        actors.save(actorManifest(), actorRevision())
        performances.save(performance("performance-wave", "actor-alice").copy(actorRevisionId = "rev-alice-1"))

        val result = ResolveActorRenderUseCase(actors, performances).execute(
            ActorPlacement(
                placementId = "placement-repeat",
                projectId = "project-film",
                performanceId = "performance-wave",
                startFrame = 10,
                durationFrames = 12,
                repeatCount = 2,
            ),
            frame = 22,
            fps = 12,
        )

        assertEquals("rev-alice-1", requireSuccess(result).revision.revisionId)
    }

    @Test fun `placement storage archives recoverably`() {
        val store = ActorFileStore(root)
        val placements = ActorPlacementRepository(store)
        val document = ActorPlacement(
            placementId = "placement-wave",
            projectId = "project-film",
            performanceId = "performance-wave",
            startFrame = 0,
            durationFrames = 12,
        )
        requireSuccess(placements.save(document))
        requireSuccess(placements.archive("project-film", "placement-wave"))
        assertTrue(requireSuccess(placements.list("project-film")).isEmpty())
        assertTrue(File(store.projectDirectory("project-film"), "archive/placements/placement-wave.json").isFile)
    }

    @Test fun `archive is recoverable and blocked while referenced`() {
        val store = ActorFileStore(root)
        val actors = ActorRepository(store)
        actors.save(actorManifest(), actorRevision())
        assertTrue(actors.archive("actor-alice") { true } is ActorStoreResult.Failure)
        assertTrue(actors.archive("actor-alice") { false } is ActorStoreResult.Value<*>)
        assertTrue(store.archiveDirectory("actor-alice").isDirectory)
    }

    @Test fun `duplicate has fresh identity and preserves the source graph`() {
        val repository = ActorRepository(ActorFileStore(root))
        repository.save(actorManifest(), actorRevision())
        requireSuccess(repository.duplicate("actor-alice", "actor-alice-copy", "revision-copy", "Alice copy", 2))
        assertTrue(
            repository.duplicate(
                "actor-alice",
                "actor-alice-copy",
                "revision-another",
                "Alice copy 2",
                3,
            ) is ActorStoreResult.Failure,
        )
        val copied = requireSuccess(repository.load("actor-alice-copy"))
        assertEquals("Alice copy", copied.first.displayName)
        assertEquals(actorRevision().sceneGraph, copied.second.sceneGraph)
    }

    @Test fun `actor library export imports with fresh identity`() {
        val store = ActorFileStore(root)
        val actors = ActorRepository(store)
        val performances = PerformanceRepository(store)
        actors.save(actorManifest(), actorRevision())
        val bundles = ActorBundleRepository(store, actors, performances)
        requireSuccess(bundles.exportActor("actor-alice", "library-alice", "Alice bundle", 2))
        val imported =
            requireSuccess(bundles.importActor("library-alice", "actor-alice-imported", "revision-imported", 3))
        assertEquals("actor-alice-imported", imported.actorId)
        assertEquals("actor-alice-imported", requireSuccess(actors.load("actor-alice-imported")).second.actorId)
    }

    @Test fun `performance library export imports into a chosen project with its pinned revision`() {
        val store = ActorFileStore(root)
        val actors = ActorRepository(store)
        val performances = PerformanceRepository(store)
        requireSuccess(actors.save(actorManifest(), actorRevision()))
        val source = performance("performance-wave", "actor-alice").copy(actorRevisionId = "rev-alice-1")
        requireSuccess(performances.save(source))
        val bundles = ActorBundleRepository(store, actors, performances)

        requireSuccess(bundles.exportPerformance("project-film", "performance-wave", "library-wave", "Wave", 2))
        val imported =
            requireSuccess(bundles.importPerformance("library-wave", "project-new", "performance-wave-copy", 3))

        assertEquals("project-new", imported.projectId)
        assertEquals("performance-wave-copy", imported.performanceId)
        assertEquals("rev-alice-1", imported.actorRevisionId)
        assertEquals(imported, requireSuccess(performances.load("project-new", "performance-wave-copy")))
    }

    @Test fun `duplicate and library import own artwork instead of retaining source paths`() {
        val store = ActorFileStore(root)
        val actors = ActorRepository(store)
        val sourcePath = "revisions/rev-alice-1/artwork/hand.png"
        val revision = actorRevision().copy(artworkAssetPaths = mapOf("left_hand" to sourcePath))
        val source = File(store.actorDirectory("actor-alice"), sourcePath)
        source.parentFile!!.mkdirs()
        source.writeBytes(byteArrayOf(1, 2, 3))
        requireSuccess(actors.save(actorManifest(), revision))

        requireSuccess(actors.duplicate("actor-alice", "actor-copy", "revision-copy", "Alice copy", 2))
        val copied = requireSuccess(actors.load("actor-copy")).second
        val copiedPath = copied.artworkAssetPaths.getValue("left_hand")
        assertTrue(File(store.actorDirectory("actor-copy"), copiedPath).isFile)
        assertEquals(
            byteArrayOf(1, 2, 3).toList(),
            File(store.actorDirectory("actor-copy"), copiedPath).readBytes().toList(),
        )

        val bundles = ActorBundleRepository(store, actors, PerformanceRepository(store))
        requireSuccess(bundles.exportActor("actor-alice", "library-alice", "Alice", 3))
        requireSuccess(bundles.importActor("library-alice", "actor-import", "revision-import", 4))
        val imported = requireSuccess(actors.load("actor-import")).second
        assertTrue(File(store.actorDirectory("actor-import"), imported.artworkAssetPaths.getValue("left_hand")).isFile)
    }

    private fun actorManifest(displayName: String = "Alice") = ActorManifest(
        actorId = "actor-alice",
        displayName = displayName,
        templateId = ActorManifest.HUMAN_TEMPLATE_ID,
        currentRevisionId = "rev-alice-1",
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )

    @Test fun `a populated actor index recovers actors whose indexing write was missed`() {
        val rows = mutableMapOf<String, ActorEntity>()
        val dao = object : ActorDao {
            override fun getAll() = rows.values.toList()
            override fun upsert(entity: ActorEntity) {
                rows[entity.actorId] =
                    entity
            }
            override fun deleteById(actorId: String) {
                rows.remove(actorId)
            }
        }
        val files = ActorFileStore(root)
        requireSuccess(ActorRepository(files, dao = dao).save(actorManifest(), actorRevision()))
        requireSuccess(
            ActorRepository(
                files,
            ).save(actorManifest("Bob").copy(actorId = "actor-bob"), actorRevision().copy(actorId = "actor-bob")),
        )
        assertEquals(1, rows.size)
        assertEquals(2, requireSuccess(ActorRepository(ActorFileStore(root), dao = dao).list()).size)
        assertEquals(2, rows.size)
    }

    @Test fun `a populated performance index cannot hide saved gestures or lose pose payloads`() {
        val rows = mutableMapOf<String, PerformanceEntity>()
        val dao = object : PerformanceDao {
            override fun getForProject(projectId: String) = rows.values.filter { it.projectId == projectId }
            override fun upsert(entity: PerformanceEntity) {
                rows[entity.performanceId] =
                    entity
            }
            override fun deleteById(performanceId: String) {
                rows.remove(performanceId)
            }
        }
        val files = ActorFileStore(root)
        val first = performance("wave", "actor-alice").copy(
            poseKeys = listOf(
                ActorPoseKey(0, mapOf("head" to 25f)),
                ActorPoseKey(500_000, mapOf("head" to 45f)),
            ),
        )
        val second = performance("jump", "actor-alice")
        requireSuccess(PerformanceRepository(files, dao = dao).save(first))
        requireSuccess(PerformanceRepository(files).save(second))
        assertEquals(1, rows.size)
        val loaded = requireSuccess(PerformanceRepository(ActorFileStore(root), dao = dao).list("project-film"))
        assertEquals(listOf(second, first), loaded)
        assertEquals(2, rows.size)
    }

    @Test fun `performance loaded under another file identity is rejected`() {
        val files = ActorFileStore(root)
        val repo = PerformanceRepository(files)
        requireSuccess(repo.save(performance("wave", "actor-alice")))
        files.performanceFile("project-film", "wave").copyTo(files.performanceFile("project-film", "wrong"))
        assertTrue(repo.load("project-film", "wrong") is ActorStoreResult.Failure)
    }

    private fun actorRevision() = ActorRevision(
        revisionId = "rev-alice-1",
        actorId = "actor-alice",
        sceneGraph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd),
        createdAtEpochMs = 1,
    )

    private fun performance(id: String, actorId: String) = PerformanceDocument(
        performanceId = id,
        projectId = "project-film",
        actorId = actorId,
        actorRevisionId = "rev-$actorId-1",
        displayName = id,
        durationMicros = 1_000_000,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> requireSuccess(result: ActorStoreResult<T>): T {
        assertTrue(result is ActorStoreResult.Value<*>, (result as? ActorStoreResult.Failure)?.message)
        return (result as ActorStoreResult.Value<T>).value
    }
}
