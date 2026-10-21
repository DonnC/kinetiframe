package com.donnclab.kinetiframe.data.actor

import com.donnclab.kinetiframe.domain.actor.PerformanceCompatibility
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates
import com.donnclab.kinetiframe.engine.rigging.model.ActorView
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ActorViewsTest {
    @TempDir lateinit var root: File
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }
    private fun seed(template: ActorTemplate = ActorTemplate.HUMAN): ActorRepository {
        val repository = ActorRepository(ActorFileStore(root))
        val type = when (template) {
            ActorTemplate.HUMAN -> ActorManifest.HUMAN_TEMPLATE_ID
            ActorTemplate.ANIMAL -> ActorManifest.ANIMAL_TEMPLATE_ID
            ActorTemplate.CUSTOM -> ActorManifest.CUSTOM_TEMPLATE_ID
        }
        value(
            repository.save(
                ActorManifest(
                    actorId = "sample",
                    displayName = "Sample",
                    templateId = type,
                    currentRevisionId = "original",
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 1,
                ),
                ActorRevision(
                    revisionId = "original",
                    actorId = "sample",
                    sceneGraph = ActorTemplates.create(template, StageDimensions.LandscapeHd),
                    createdAtEpochMs = 1,
                ),
            ),
        )
        return repository
    }

    @Test fun `legacy actor gains independent view without rewriting original revision`() {
        val repository = seed()
        val original = value(repository.load("sample")).second
        val profile = value(repository.createView("sample", ActorView.PROFILE, "profile", 2)).second
        assertTrue(profile.artworkAssetPaths.isEmpty())
        assertNotEquals(original.sceneGraph, profile.sceneGraph)
        val saved =
            value(
                repository.saveRevision(
                    "sample",
                    profile.sceneGraph,
                    "profile-edited",
                    3,
                    emptyMap(),
                    ActorView.PROFILE,
                ),
            )
        assertEquals(
            mapOf(ActorView.ORIGINAL to "original", ActorView.PROFILE to "profile-edited"),
            saved.first.authoredViews(),
        )
        val fresh = ActorRepository(ActorFileStore(root))
        assertEquals(
            profile.copy(revisionId = "profile-edited", createdAtEpochMs = 3),
            value(fresh.load("sample")).second,
        )
        value(fresh.selectView("sample", ActorView.ORIGINAL, 4))
        assertEquals(original, value(fresh.load("sample")).second)
        val motion =
            PerformanceDocument(
                performanceId = "talk",
                projectId = "film",
                actorId = "sample",
                actorRevisionId = "original",
                displayName = "Talk",
                durationMicros = 1_000_000,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            )
        PerformanceCompatibility.validate(motion, original)
        assertThrows(IllegalArgumentException::class.java) { PerformanceCompatibility.validate(motion, profile) }
    }

    @Test fun `character hierarchy retains project ownership and pinned revisions across views`() {
        val repository = seed()
        val profile = value(repository.createView("sample", ActorView.PROFILE, "profile", 2)).second
        val performances = PerformanceRepository(ActorFileStore(root))
        val talk = PerformanceDocument(
            performanceId = "talk",
            projectId = "episode-one",
            actorId = "sample",
            actorRevisionId = "original",
            displayName = "Talk",
            durationMicros = 1_000_000,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )
        val run = talk.copy(
            performanceId = "run",
            projectId = "episode-two",
            actorRevisionId = profile.revisionId,
            displayName = "Run",
        )
        value(performances.save(talk))
        value(performances.save(run))
        assertEquals(setOf(talk, run), repository.relatedPerformances().toSet())
        value(repository.selectView("sample", ActorView.ORIGINAL, 3))
        assertEquals(setOf(talk, run), ActorRepository(ActorFileStore(root)).relatedPerformances().toSet())
        assertEquals(talk, value(performances.load("episode-one", "talk")))
    }

    @Test fun `library round trip retains all views and separate same named artwork`() {
        val repository = seed()
        val files = ActorFileStore(root)
        val original = value(repository.load("sample")).second
        File(files.actorDirectory("sample"), "front/head.png").apply {
            requireNotNull(parentFile).mkdirs()
            writeBytes(byteArrayOf(1))
        }
        value(repository.saveRevision("sample", original.sceneGraph, "front-art", 2, mapOf("head" to "front/head.png")))
        val profile = value(repository.createView("sample", ActorView.PROFILE, "profile", 3)).second
        File(files.actorDirectory("sample"), "side/head.png").apply {
            requireNotNull(parentFile).mkdirs()
            writeBytes(byteArrayOf(2))
        }
        value(repository.saveRevision("sample", profile.sceneGraph, "profile-art", 4, mapOf("head" to "side/head.png")))
        val bundles = ActorBundleRepository(files, repository, PerformanceRepository(files))
        value(bundles.exportActor("sample", "library", "Sample", 4))
        val imported = value(bundles.importActor("library", "imported", "imported-rev", 5))
        assertEquals(2, imported.authoredViews().size)
        imported.authoredViews().forEach { (view, id) ->
            val revision = value(repository.loadRevision("imported", id))
            assertEquals(view, revision.view)
            val bytes = File(files.actorDirectory("imported"), revision.artworkAssetPaths.getValue("head")).readBytes()
            assertArrayEquals(byteArrayOf(if (view == ActorView.PROFILE) 2 else 1), bytes)
        }
    }

    @Test fun `duplicates preserve non human template and every view`() {
        for (template in listOf(ActorTemplate.ANIMAL, ActorTemplate.CUSTOM)) {
            val folder = File(root, template.name)
            val repository = ActorRepository(ActorFileStore(folder))
            val type = if (template ==
                ActorTemplate.ANIMAL
            ) {
                ActorManifest.ANIMAL_TEMPLATE_ID
            } else {
                ActorManifest.CUSTOM_TEMPLATE_ID
            }
            val manifest =
                ActorManifest(
                    actorId = "sample",
                    displayName = "Sample",
                    templateId = type,
                    currentRevisionId = "original",
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 1,
                )
            value(
                repository.save(
                    manifest,
                    ActorRevision(
                        revisionId = "original",
                        actorId = "sample",
                        sceneGraph = ActorTemplates.create(template, StageDimensions.LandscapeHd),
                        createdAtEpochMs = 1,
                    ),
                ),
            )
            value(repository.createView("sample", ActorView.BACK, "back", 2))
            val copy = value(repository.duplicate("sample", "copy", "copy-rev", "Copy", 3))
            assertEquals(type, copy.templateId)
            assertEquals(2, copy.authoredViews().size)
            assertTrue(copy.authoredViews().values.none { it in listOf("original", "back") })
        }
    }

    @Test fun `invalid or missing catalog revision cannot publish`() {
        val repository = seed()
        val original = value(repository.load("sample"))
        val corrupt = original.first.copy(viewRevisions = mapOf(ActorView.FRONT to "original"))
        assertTrue(repository.save(corrupt, original.second) is ActorStoreResult.Failure)
        assertTrue(
            repository.save(
                original.first.copy(
                    viewRevisions = mapOf(
                        ActorView.ORIGINAL to "original",
                        ActorView.PROFILE to "absent",
                    ),
                ),
                original.second,
            ) is ActorStoreResult.Failure,
        )
        assertEquals(original, value(repository.load("sample")))
        assertTrue(repository.createView("sample", ActorView.ORIGINAL, "bad", 2) is ActorStoreResult.Failure)
    }
}
