package com.donnclab.kinetiframe.actors.ui

import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorManifest
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.document.StageDimensions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ActorStudioDocumentViewModelTest {
    @TempDir lateinit var root: File

    @Test fun `studio exit always offers a save choice even for a clean session`() = runBlocking {
        val model = ActorStudioDocumentViewModel(ActorStudioTarget("sample"), actorWithViews())
        model.state.first { !it.loading }
        model.handleIntent(ActorStudioDocumentIntent.RequestExit)
        assertEquals(ActorStudioDocumentEffect.ConfirmDiscard, model.effect.first())
    }

    @Test fun `save and exit only exits after revision is committed`() = runBlocking {
        val repository = actorWithViews()
        val model = ActorStudioDocumentViewModel(ActorStudioTarget("sample"), repository)
        val revision = model.state.first { !it.loading }.revision!!
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(true))
        model.handleIntent(
            ActorStudioDocumentIntent.SaveGraph(revision.sceneGraph, "exit-save", exitAfterSaving = true),
        )
        assertEquals(ActorStudioDocumentEffect.Exit, model.effect.first())
        assertEquals("exit-save", model.state.value.revision!!.revisionId)
        assertFalse(model.state.value.isDirty)
    }

    @Test fun `failed save and exit leaves studio dirty and reports failure instead of exiting`() = runBlocking {
        val repository = actorWithViews()
        val model = ActorStudioDocumentViewModel(ActorStudioTarget("sample"), repository)
        val revision = model.state.first { !it.loading }.revision!!
        File(root, "actors/sample/revisions/blocked.json").mkdirs()
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(true))
        model.handleIntent(ActorStudioDocumentIntent.SaveGraph(revision.sceneGraph, "blocked", exitAfterSaving = true))
        org.junit.jupiter.api.Assertions.assertTrue(model.effect.first() is ActorStudioDocumentEffect.ShowError)
        org.junit.jupiter.api.Assertions.assertTrue(model.state.value.isDirty)
        assertEquals(revision, model.state.value.revision)
    }

    @Test fun `invalid performance save preserves dirty live canvas and pinned revision`() = runBlocking {
        val repository = actorWithViews()
        val model =
            ActorStudioDocumentViewModel(
                ActorStudioTarget("sample", "film"),
                repository,
                PerformanceRepository(ActorFileStore(root)),
            )
        val revision = model.state.first { !it.loading }.revision
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(true))
        model.handleIntent(ActorStudioDocumentIntent.SavePerformance("", emptyList()))
        model.effect.first { it is ActorStudioDocumentEffect.ShowError }
        assertEquals(revision, model.state.value.revision)
        org.junit.jupiter.api.Assertions.assertTrue(model.state.value.isDirty)
        assertFalse(model.state.value.loading)
        assertFalse(model.state.value.isSaving)
        org.junit.jupiter.api.Assertions.assertNull(model.state.value.performance)
    }

    @Test fun `failed actor write preserves dirty canvas without triggering reload`() = runBlocking {
        val repository = actorWithViews()
        val model = ActorStudioDocumentViewModel(ActorStudioTarget("sample"), repository)
        val revision = model.state.first { !it.loading }.revision!!
        File(root, "actors/sample/revisions/blocked.json").mkdirs()
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(true))
        model.handleIntent(ActorStudioDocumentIntent.SaveGraph(revision.sceneGraph, "blocked"))
        model.effect.first { it is ActorStudioDocumentEffect.ShowError }
        assertEquals(revision, model.state.value.revision)
        org.junit.jupiter.api.Assertions.assertTrue(model.state.value.isDirty)
        assertFalse(model.state.value.loading)
        assertFalse(model.state.value.isSaving)
    }

    private fun actorWithViews(): ActorRepository {
        val repository = ActorRepository(ActorFileStore(root))
        repository.save(
            ActorManifest(
                actorId = "sample",
                displayName = "Sample",
                templateId = ActorManifest.HUMAN_TEMPLATE_ID,
                currentRevisionId = "original",
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            ),
            ActorRevision(
                revisionId = "original",
                actorId = "sample",
                sceneGraph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd),
                createdAtEpochMs = 1,
            ),
        )
        repository.createView("sample", com.donnclab.kinetiframe.actors.rig.model.ActorView.PROFILE, "profile", 2)
        return repository
    }

    @Test fun `dirty view refuses switch and new performance can choose a saved view`() = runBlocking {
        val repository = actorWithViews()
        val model =
            ActorStudioDocumentViewModel(
                ActorStudioTarget("sample", "film"),
                repository,
                PerformanceRepository(
                    ActorFileStore(root),
                ),
                now = {
                    3
                },
            )
        model.state.first { !it.loading }
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(true))
        model.handleIntent(
            ActorStudioDocumentIntent.SelectView(com.donnclab.kinetiframe.actors.rig.model.ActorView.ORIGINAL),
        )
        val error = model.effect.first() as ActorStudioDocumentEffect.ShowError
        assertEquals("Save your edits before changing views.", error.message)
        assertEquals("profile", model.state.value.revision?.revisionId)
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(false))
        model.handleIntent(
            ActorStudioDocumentIntent.SelectView(com.donnclab.kinetiframe.actors.rig.model.ActorView.ORIGINAL),
        )
        model.state.first { !it.loading && it.revision?.revisionId == "original" }
        model.handleIntent(ActorStudioDocumentIntent.SavePerformance("Talk", emptyList()))
        val saved = model.effect.first() as ActorStudioDocumentEffect.PerformanceSaved
        assertEquals("original", saved.document.actorRevisionId)
    }

    @Test fun `reopening performance resolves pinned view not actors current editable view`() = runBlocking {
        val repository = actorWithViews()
        val performances = PerformanceRepository(ActorFileStore(root))
        performances.save(
            com.donnclab.kinetiframe.actors.data.PerformanceDocument(
                performanceId = "talk",
                projectId = "film",
                actorId = "sample",
                actorRevisionId = "original",
                displayName = "Talk",
                durationMicros = 1_000_000,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            ),
        )
        val model =
            ActorStudioDocumentViewModel(ActorStudioTarget("sample", "film", "talk"), repository, performances, now = {
                3
            })
        val state = model.state.first { !it.loading }
        assertEquals("profile", state.manifest?.currentRevisionId)
        assertEquals("original", state.revision?.revisionId)
        model.handleIntent(
            ActorStudioDocumentIntent.SelectView(com.donnclab.kinetiframe.actors.rig.model.ActorView.PROFILE),
        )
        model.effect.first { it is ActorStudioDocumentEffect.ShowError }
        assertEquals("original", model.state.value.revision?.revisionId)
    }

    @Test fun `saving a project performance emits the saved document for FxF return`() = runBlocking {
        val store = ActorFileStore(root)
        val actor = ActorRepository(store)
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        actor.save(
            ActorManifest(
                actorId = "actor-alice",
                displayName = "Alice",
                templateId = ActorManifest.HUMAN_TEMPLATE_ID,
                currentRevisionId = "revision-1",
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            ),
            ActorRevision(revisionId = "revision-1", actorId = "actor-alice", sceneGraph = graph, createdAtEpochMs = 1),
        )
        val model =
            ActorStudioDocumentViewModel(
                ActorStudioTarget(
                    "actor-alice",
                    "project-film",
                ),
                actor,
                PerformanceRepository(store),
                now = {
                    2
                },
                id = { "wave" },
            )
        model.state.first { !it.loading }
        model.handleIntent(ActorStudioDocumentIntent.SavePerformance("Wave", emptyList(), durationMicros = 2_500_000L))
        val effect = model.effect.first {
            it is ActorStudioDocumentEffect.PerformanceSaved
        } as ActorStudioDocumentEffect.PerformanceSaved
        assertEquals("performance-wave", effect.document.performanceId)
        assertEquals("project-film", effect.document.projectId)
        assertEquals("revision-1", effect.document.actorRevisionId)
        assertEquals(2_500_000L, effect.document.durationMicros)
    }

    @Test fun `saving a performance while a preset is applied keeps the preset`() = runBlocking {
        val store = ActorFileStore(root)
        val actor = ActorRepository(store)
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        actor.save(
            ActorManifest(
                actorId = "actor-alice",
                displayName = "Alice",
                templateId = ActorManifest.HUMAN_TEMPLATE_ID,
                currentRevisionId = "revision-1",
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            ),
            ActorRevision(revisionId = "revision-1", actorId = "actor-alice", sceneGraph = graph, createdAtEpochMs = 1),
        )
        val model =
            ActorStudioDocumentViewModel(
                ActorStudioTarget(
                    "actor-alice",
                    "project-film",
                ),
                actor,
                PerformanceRepository(store),
                now = {
                    2
                },
                id = { "cheer" },
            )
        model.state.first { !it.loading }
        val preset = com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset(
            com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset(
                id = "human-cheer",
                name = "Cheer",
                description = "",
                durationSeconds = 2f,
                requiredBones = listOf("torso"),
                channels = listOf(
                    com.donnclab.kinetiframe.actors.rig.presets.RotationChannel(
                        boneId = "torso",
                        amplitudeDegrees = 15f,
                    ),
                ),
                loop = false,
            ),
            speed = 2f,
        )
        // No pose keys were recorded, so the applied preset is the only motion. It must still
        // be saved rather than producing a motionless performance.
        model.handleIntent(ActorStudioDocumentIntent.SavePerformance("Cheer", emptyList(), preset))
        val effect = model.effect.first {
            it is ActorStudioDocumentEffect.PerformanceSaved
        } as ActorStudioDocumentEffect.PerformanceSaved
        assertEquals(preset, effect.document.presetSnapshot)
        // durationSeconds 2f at speed 2x plays out in 1 real second -- the saved clip must reflect
        // that, not the hardcoded 1-second fallback used when there is truly no motion at all.
        assertEquals(1_000_000L, effect.document.durationMicros)
    }

    @Test fun `dirty actor studio asks before exit and exits after discard`() = runBlocking {
        val model =
            ActorStudioDocumentViewModel(ActorStudioTarget("actor-alice"), ActorRepository(ActorFileStore(root)))
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(true))
        model.handleIntent(ActorStudioDocumentIntent.RequestExit)
        assertEquals(ActorStudioDocumentEffect.ConfirmDiscard, model.effect.first())
        model.handleIntent(ActorStudioDocumentIntent.DiscardAndExit)
        assertEquals(ActorStudioDocumentEffect.Exit, model.effect.first())
        model.handleIntent(ActorStudioDocumentIntent.SetDirty(false))
        assertFalse(model.state.value.isDirty)
    }

    @Test fun `existing performance can duplicate and archive without changing its pinned revision`() = runBlocking {
        val store = ActorFileStore(root)
        val actor = ActorRepository(store)
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        actor.save(
            ActorManifest(
                actorId = "actor-alice",
                displayName = "Alice",
                templateId = ActorManifest.HUMAN_TEMPLATE_ID,
                currentRevisionId = "revision-1",
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            ),
            ActorRevision(revisionId = "revision-1", actorId = "actor-alice", sceneGraph = graph, createdAtEpochMs = 1),
        )
        val performanceRepository = PerformanceRepository(store)
        val original = com.donnclab.kinetiframe.actors.data.PerformanceDocument(
            performanceId = "performance-wave",
            projectId = "project-film",
            actorId = "actor-alice",
            actorRevisionId = "revision-1",
            displayName = "Wave",
            durationMicros = 1_000_000,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )
        performanceRepository.save(original)
        val model =
            ActorStudioDocumentViewModel(
                ActorStudioTarget("actor-alice", "project-film", "performance-wave"),
                actor,
                performanceRepository,
                now = { 2 },
                id = { "copy" },
            )
        model.state.first { !it.loading }

        model.handleIntent(ActorStudioDocumentIntent.DuplicatePerformance("Wave copy"))
        val duplicate = model.state.first { it.performance?.performanceId == "performance-copy" }.performance!!
        assertEquals("revision-1", duplicate.actorRevisionId)
        assertEquals("Wave copy", duplicate.displayName)

        model.handleIntent(ActorStudioDocumentIntent.ArchivePerformance)
        assertEquals(ActorStudioDocumentEffect.Exit, model.effect.first())
    }
}
