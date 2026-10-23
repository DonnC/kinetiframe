package com.donnclab.kinetiframe.project.data

import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectRepositoryTest {
    @TempDir lateinit var root: File

    @Test fun `camera shots and faded audio survive a fresh repository reopen`() {
        val files = ActorFileStore(root)
        val base = document("effects")
        val shot = com.donnclab.kinetiframe.studio.camera.SceneCamera.preset(
            "pan",
            com.donnclab.kinetiframe.core.document.camera.CameraAction.PAN,
            0,
            24,
            com.donnclab.kinetiframe.core.document.camera.CameraPose(),
        ).copy(waitFrames = 3, settleFrames = 4, frequencyHz = 5f)
        val audio = com.donnclab.kinetiframe.core.document.AudioClipPlacement(
            "voice", "asset", 0, "Voice", 0, 24,
            fadeInFrames = 6, fadeOutFrames = 6, envelopeDurationFrames = 24, equalPowerFades = true,
            voiceEffect = com.donnclab.kinetiframe.core.document.VoiceEffect.RADIO, originalAssetId = "original",
        )
        val doc = base.copy(
            totalFrames = 24,
            project = base.project.copy(cameraShots = listOf(shot), audioClips = mapOf("voice" to audio)),
        )
        requireSuccess(ProjectRepository(files).save(manifest("effects", "Effects"), doc))
        val reopened = requireSuccess(ProjectRepository(ActorFileStore(root)).load("effects")).second
        assertEquals(listOf(shot), reopened.project.cameraShots)
        assertEquals(audio, reopened.project.audioClips["voice"])
    }

    @Test fun `held drawing references and canonical pixels survive portable project import`() {
        val files = ActorFileStore(root)
        val doc = document("held-project").copy(
            totalFrames = 3,
            linkedCels = mapOf("main" to mapOf(0 to "cel-a", 1 to "cel-a", 2 to "cel-a")),
        )
        val frames = File(files.projectDirectory("held-project"), "frames").apply { mkdirs() }
        // Archiving treats PNGs as opaque bytes; bitmap decoding is covered by cache tests.
        val pixels = File(frames, "frame_v2_main_linked_cel-a.png").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        requireSuccess(ProjectRepository(files).save(manifest("held-project", "Held drawing"), doc))
        assertEquals(doc.linkedCels, requireSuccess(ProjectRepository(files).load("held-project")).second.linkedCels)
        val archive = File(root, "held.kframe")
        requireSuccess(KframeArchiver.export(files, "held-project", archive))
        val destination = ActorFileStore(File(root, "imported"))
        requireSuccess(KframeArchiver.import(destination, archive))
        val restored = requireSuccess(ProjectRepository(destination).load("held-project")).second
        assertEquals(doc.linkedCels, restored.linkedCels)
        assertTrue(
            pixels.readBytes().contentEquals(
                File(destination.projectDirectory("held-project"), "frames/${pixels.name}").readBytes(),
            ),
        )
    }

    private fun document(
        projectId: String,
        stage: StageDimensions = StageDimensions.LandscapeHd,
        fps: Int = 12,
    ): ProjectSaveDocument {
        val mainLayer = Layer(name = "Main Layer", id = "main")
        return ProjectSaveDocument(
            project = ProjectDocument(id = projectId, stage = stage, fps = fps, layers = listOf(mainLayer)),
            frameLayers = mapOf(0 to listOf(mainLayer)),
            totalFrames = 1,
        )
    }

    private fun manifest(
        projectId: String,
        name: String,
        stage: StageDimensions = StageDimensions.LandscapeHd,
        fps: Int = 12,
        time: Long = 1L,
    ) = ProjectManifest(
        projectId = projectId,
        displayName = name,
        stage = stage,
        fps = fps,
        createdAtEpochMs = time,
        updatedAtEpochMs = time,
    )

    private fun <T> requireSuccess(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value

        is ActorStoreResult.Failure -> throw AssertionError(
            "Expected success but got failure: ${result.message}",
            result.cause,
        )
    }

    @Test fun `a saved project round trips through its manifest and document`() {
        val repository = ProjectRepository(ActorFileStore(root))
        val saved = repository.save(manifest("project-a", "My Cartoon"), document("project-a"))
        assertTrue(saved is ActorStoreResult.Value<*>)

        val (loadedManifest, loadedDocument) = requireSuccess(repository.load("project-a"))
        assertEquals("My Cartoon", loadedManifest.displayName)
        assertEquals("project-a", loadedDocument.project.id)
        assertEquals(1, loadedDocument.totalFrames)
        assertEquals(listOf("main"), loadedDocument.project.layers.map(Layer::id))
    }

    @Test fun `embedded video survives closing repository and portable archive import`() {
        val files = ActorFileStore(root)
        val source = File(root, "screen.mp4").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val clip = com.donnclab.kinetiframe.core.document.VideoClipPlacement(
            "screen", "video", "Story", 12, 48,
            videoSourceOffsetSeconds = 1.5, isSceneContent = true, x = 600f, y = 40f, width = 400f, height = 225f,
            selectionTransform = com.donnclab.kinetiframe.core.geometry.Affine2D(tx = 12f),
        )
        val base = document("project-video")
        val doc = base.copy(
            totalFrames = 48,
            project = base.project.copy(
                videoClips = mapOf("screen" to clip),
                assets = mapOf(
                    "video" to
                        com.donnclab.kinetiframe.core.document.ProjectAsset(
                            "video",
                            com.donnclab.kinetiframe.core.document.AssetKind.VIDEO,
                            source.absolutePath,
                            1280,
                            720,
                        ),
                ),
            ),
        )
        requireSuccess(ProjectRepository(files).save(manifest("project-video", "Screen story"), doc))
        assertEquals(
            clip,
            requireSuccess(
                ProjectRepository(ActorFileStore(root)).load("project-video"),
            ).second.project.videoClips["screen"],
        )
        val archive = File(root, "screen.kframe")
        requireSuccess(KframeArchiver.export(files, "project-video", archive))
        val otherFiles = ActorFileStore(File(root, "other-device"))
        requireSuccess(KframeArchiver.import(otherFiles, archive))
        val reopened = requireSuccess(ProjectRepository(otherFiles).load("project-video")).second.project
        assertEquals(clip, reopened.videoClips["screen"])
        val relocated = File(requireNotNull(reopened.assets["video"]?.sourcePath))
        assertTrue(relocated.isFile)
        assertTrue(relocated.absolutePath.startsWith(otherFiles.projectDirectory("project-video").absolutePath))
        assertTrue(source.readBytes().contentEquals(relocated.readBytes()))
    }

    @Test fun `the gallery lists newest-updated projects first`() {
        val repository = ProjectRepository(ActorFileStore(root))
        repository.save(manifest("project-a", "Older", time = 1L), document("project-a"))
        repository.save(manifest("project-b", "Newer", time = 2L), document("project-b"))

        val listed = requireSuccess(repository.list())
        assertEquals(listOf("Newer", "Older"), listed.map { it.displayName })
    }

    @Test fun `project names must be unique, case-insensitively`() {
        val repository = ProjectRepository(ActorFileStore(root))
        repository.save(manifest("project-a", "My Cartoon"), document("project-a"))

        assertEquals(false, requireSuccess(repository.isDisplayNameAvailable("my cartoon")))
        assertEquals(
            true,
            requireSuccess(repository.isDisplayNameAvailable("my cartoon", excludingProjectId = "project-a")),
        )
        assertEquals(true, requireSuccess(repository.isDisplayNameAvailable("A Different Name")))
    }

    @Test fun `renaming a project preserves its document and updates the manifest only`() {
        val repository = ProjectRepository(ActorFileStore(root))
        repository.save(manifest("project-a", "Old Name", time = 1L), document("project-a"))

        val renamed = requireSuccess(repository.rename("project-a", "New Name", 2L))
        assertEquals("New Name", renamed.displayName)
        assertEquals(2L, renamed.updatedAtEpochMs)

        val (_, stillLoadedDocument) = requireSuccess(repository.load("project-a"))
        assertEquals("project-a", stillLoadedDocument.project.id)
    }

    @Test fun `deleting a project removes its whole directory, including performances placed elsewhere in it`() {
        val store = ActorFileStore(root)
        val repository = ProjectRepository(store)
        repository.save(manifest("project-a", "My Cartoon"), document("project-a"))
        // Simulate a sibling performance file living under the same project directory, the way
        // PerformanceRepository actually writes it -- a project delete must take this with it too.
        val performanceFile = File(store.projectDirectory("project-a"), "performances/perf-1.json")
        performanceFile.parentFile!!.mkdirs()
        performanceFile.writeText("{}")

        assertTrue(repository.delete("project-a") is ActorStoreResult.Value<*>)
        assertTrue(repository.load("project-a") is ActorStoreResult.Failure)
        assertTrue(!store.projectDirectory("project-a").exists())
    }

    @Test fun `deleting a project that does not exist is a typed failure, not a crash`() {
        val repository = ProjectRepository(ActorFileStore(root))
        assertTrue(repository.delete("no-such-project") is ActorStoreResult.Failure)
    }

    @Test fun `a manifest whose project ID disagrees with its document is rejected`() {
        val repository = ProjectRepository(ActorFileStore(root))
        val result = repository.save(manifest("project-a", "My Cartoon"), document("project-b"))
        assertTrue(result is ActorStoreResult.Failure)
    }

    @Test fun `a malformed manifest file is surfaced as a typed storage failure, not a crash`() {
        val store = ActorFileStore(root)
        val manifestFile = File(store.projectDirectory("project-broken"), "manifest.json")
        manifestFile.parentFile!!.mkdirs()
        manifestFile.writeText("not json")

        assertEquals(emptyList<ProjectManifest>(), requireSuccess(ProjectRepository(store).list()))
        assertTrue(ProjectRepository(store).load("project-broken") is ActorStoreResult.Failure)
    }

    @Test fun `loading is null-safe against a project with no manifest at all`() {
        val repository = ProjectRepository(ActorFileStore(root))
        assertTrue(repository.load("never-created") is ActorStoreResult.Failure)
    }

    @Test fun `fresh repository reads committed snapshot despite interrupted legacy updates`() {
        val store = ActorFileStore(root)
        requireSuccess(ProjectRepository(store).save(manifest("project-a", "Saved"), document("project-a")))
        File(store.projectDirectory("project-a"), "manifest.json").writeText("interrupted")
        File(store.projectDirectory("project-a"), "document.json").writeText("interrupted")
        val reopened = ProjectRepository(ActorFileStore(root))
        assertEquals("Saved", requireSuccess(reopened.load("project-a")).first.displayName)
        assertEquals(1, requireSuccess(reopened.list()).size)
    }

    @Test fun `canvas and FPS disagreement cannot replace a committed project`() {
        val repo = ProjectRepository(ActorFileStore(root))
        requireSuccess(repo.save(manifest("project-a", "Saved"), document("project-a")))
        assertTrue(
            repo.save(manifest("project-a", "Wrong", fps = 24), document("project-a")) is ActorStoreResult.Failure,
        )
        assertEquals(12, requireSuccess(ProjectRepository(ActorFileStore(root)).load("project-a")).second.project.fps)
    }

    @Test fun `editable text and independent frame layer stacks survive reopening`() {
        val saved = document("project-text", StageDimensions.Square, 24)
        val text = com.donnclab.kinetiframe.core.document.TextNode(
            id = "caption",
            text = "Hello",
            x = 100f,
            rotation = 35f,
        )
        val front = Layer(name = "Front", id = "front")
        val complete = saved.copy(
            project = saved.project.copy(nodes = mapOf(text.id to text)),
            totalFrames = 2,
            frameLayers = saved.frameLayers + (1 to listOf(front)),
        )
        requireSuccess(
            ProjectRepository(
                ActorFileStore(root),
            ).save(manifest("project-text", "Text", StageDimensions.Square, 24), complete),
        )
        val reopened = requireSuccess(ProjectRepository(ActorFileStore(root)).load("project-text"))
        assertEquals(complete, reopened.second)
        assertEquals(2, reopened.first.totalFrames)
        assertEquals(24, reopened.first.fps)
    }
}
