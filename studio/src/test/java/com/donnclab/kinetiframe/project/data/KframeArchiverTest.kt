package com.donnclab.kinetiframe.project.data

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KframeArchiverTest {
    @TempDir lateinit var sourceRoot: File

    @TempDir lateinit var otherRoot: File

    @TempDir lateinit var exportDir: File

    private fun document(projectId: String) = ProjectSaveDocument(
        project = ProjectDocument(
            id = projectId,
            stage = StageDimensions.LandscapeHd,
            fps = 12,
            layers = listOf(Layer(name = "Main Layer", id = "main")),
        ),
        frameLayers = mapOf(0 to listOf(Layer(name = "Main Layer", id = "main"))),
        totalFrames = 3,
    )

    private fun manifest(projectId: String) = ProjectManifest(
        projectId = projectId,
        displayName = "My Cartoon",
        stage = StageDimensions.LandscapeHd,
        fps = 12,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
        totalFrames = 3,
    )

    private fun <T> requireSuccess(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value

        is StoreResult.Failure -> throw AssertionError(
            "Expected success but got failure: ${result.message}",
            result.cause,
        )
    }

    @Test fun `exporting then importing round trips the project's manifest and document, keeping its id`() {
        val sourceFiles = DocumentFileStore(sourceRoot)
        val repository = ProjectRepository(sourceFiles)
        requireSuccess(repository.save(manifest("project-a"), document("project-a")))
        // A file that isn't manifest.json/document.json (a frame PNG stand-in) must survive too.
        File(sourceFiles.projectDirectory("project-a"), "frames").apply {
            mkdirs()
        }.let { File(it, "0.png").writeText("pixels") }

        val archive = File(exportDir, "export.kframe")
        requireSuccess(KframeArchiver.export(sourceFiles, "project-a", archive))
        assertTrue(archive.isFile)

        val destinationFiles = DocumentFileStore(otherRoot)
        val importedId = requireSuccess(KframeArchiver.import(destinationFiles, archive))
        assertEquals("project-a", importedId)

        val (importedManifest, importedDocument) = requireSuccess(ProjectRepository(destinationFiles).load("project-a"))
        assertEquals("My Cartoon", importedManifest.displayName)
        assertEquals(3, importedDocument.totalFrames)
        assertTrue(File(destinationFiles.projectDirectory("project-a"), "frames/0.png").readText() == "pixels")
    }

    @Test fun `importing an archive whose id already exists locally is refused, not silently overwritten`() {
        val files = DocumentFileStore(sourceRoot)
        requireSuccess(ProjectRepository(files).save(manifest("project-a"), document("project-a")))
        val archive = File(exportDir, "export.kframe")
        requireSuccess(KframeArchiver.export(files, "project-a", archive))

        // Import into the very same store that already has this project id.
        val result = KframeArchiver.import(files, archive)
        assertTrue(result is StoreResult.Failure)
        assertEquals("A project with this ID already exists locally", (result as StoreResult.Failure).message)
    }

    @Test fun `importing a file that is not a kframe archive fails cleanly`() {
        val notAnArchive = File(exportDir, "notes.txt").apply { writeText("hello") }
        val result = KframeArchiver.import(DocumentFileStore(otherRoot), notAnArchive)
        assertTrue(result is StoreResult.Failure)
    }

    @Test fun `importing to another storage root relocates audio and background paths`() {
        val files = DocumentFileStore(sourceRoot)
        val audio = File(exportDir, "sound.m4a").apply { writeText("sound bytes") }
        val background = File(files.projectDirectory("project-a"), "project-assets/background.png")
        background.parentFile!!.mkdirs()
        background.writeText("background pixels")
        val original = document("project-a")
        val project = original.project.copy(
            assets = mapOf(
                "sound" to
                    com.donnclab.kinetiframe.core.document.ProjectAsset(
                        "sound",
                        com.donnclab.kinetiframe.core.document.AssetKind.AUDIO,
                        audio.path,
                        1,
                        1,
                    ),
                "background" to
                    com.donnclab.kinetiframe.core.document.ProjectAsset(
                        "background",
                        com.donnclab.kinetiframe.core.document.AssetKind.IMPORTED_IMAGE,
                        background.path,
                        128,
                        128,
                    ),
            ),
            backgroundAssetId = "background",
        )
        requireSuccess(ProjectRepository(files).save(manifest("project-a"), original.copy(project = project)))
        val archive = File(exportDir, "portable.kframe")
        requireSuccess(KframeArchiver.export(files, "project-a", archive))
        val destination = DocumentFileStore(otherRoot)
        requireSuccess(KframeArchiver.import(destination, archive))
        val restored = requireSuccess(ProjectRepository(destination).load("project-a")).second.project
        assertEquals("sound bytes", File(restored.assets.getValue("sound").sourcePath!!).readText())
        assertEquals("background pixels", File(restored.assets.getValue("background").sourcePath!!).readText())
        assertTrue(
            restored.assets.values.all {
                File(it.sourcePath!!).canonicalPath.startsWith(otherRoot.canonicalPath)
            },
        )
    }

    @Test fun `archive includes actor revision dependencies outside the project directory`() {
        val files = DocumentFileStore(sourceRoot)
        val actor = com.donnclab.kinetiframe.core.actor.document.ActorManifest(
            actorId = "alice",
            displayName = "Alice",
            templateId = com.donnclab.kinetiframe.core.actor.document.ActorManifest.HUMAN_TEMPLATE_ID,
            currentRevisionId = "revision-one",
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )
        val revision = com.donnclab.kinetiframe.core.actor.document.ActorRevision(
            actorId = "alice",
            revisionId = "revision-one",
            sceneGraph = com.donnclab.kinetiframe.actors.rig.model.ActorTemplates.create(
                com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.HUMAN,
                StageDimensions.LandscapeHd,
            ),
            createdAtEpochMs = 1,
        )
        requireSuccess(com.donnclab.kinetiframe.actors.data.ActorRepository(files).save(actor, revision))
        val performance = com.donnclab.kinetiframe.core.actor.document.PerformanceDocument(
            performanceId = "wave",
            projectId = "project-a",
            actorId = "alice",
            actorRevisionId = "revision-one",
            displayName = "Wave",
            durationMicros = 1_000_000,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )
        requireSuccess(com.donnclab.kinetiframe.actors.data.PerformanceRepository(files).save(performance))
        val original = document("project-a")
        val placement = com.donnclab.kinetiframe.core.document.ActorPlacementReference(
            "placed",
            "wave",
            "alice",
            "revision-one",
            "Wave",
            0,
            3,
        )
        requireSuccess(
            ProjectRepository(
                files,
            ).save(
                manifest("project-a"),
                original.copy(project = original.project.copy(actorPlacements = mapOf("placed" to placement))),
            ),
        )
        val archive = File(exportDir, "actor.kframe")
        requireSuccess(KframeArchiver.export(files, "project-a", archive))
        val destination = DocumentFileStore(otherRoot)
        requireSuccess(
            com.donnclab.kinetiframe.actors.data.ActorRepository(
                destination,
            ).save(actor.copy(displayName = "My Alice", updatedAtEpochMs = 2), revision),
        )
        val localRevision = File(destination.actorDirectory("alice"), "revisions/revision-one.json")
        val preservedRevision = "\n  " + localRevision.readText()
        localRevision.writeText(preservedRevision)
        requireSuccess(KframeArchiver.import(destination, archive))
        assertEquals(
            revision,
            requireSuccess(
                com.donnclab.kinetiframe.actors.data.ActorRepository(destination).loadRevision("alice", "revision-one"),
            ),
        )
        assertEquals(
            "My Alice",
            requireSuccess(
                com.donnclab.kinetiframe.actors.data.ActorRepository(destination).load("alice"),
            ).first.displayName,
        )
        assertEquals(preservedRevision, localRevision.readText())
        assertEquals(
            performance,
            requireSuccess(
                com.donnclab.kinetiframe.actors.data.PerformanceRepository(destination).load("project-a", "wave"),
            ),
        )
    }

    @Test fun `archive uses committed snapshot and relocates its thumbnail consistently`() {
        val files = DocumentFileStore(sourceRoot)
        val thumbnail = File(files.projectDirectory("project-a"), "thumbnail.png")
        thumbnail.parentFile!!.mkdirs()
        thumbnail.writeText("thumbnail")
        requireSuccess(
            ProjectRepository(
                files,
            ).save(manifest("project-a").copy(thumbnailPath = thumbnail.absolutePath), document("project-a")),
        )
        // Simulate interrupted compatibility-file updates after a previously successful save.
        File(files.projectDirectory("project-a"), "document.json").writeText("partial")
        File(files.projectDirectory("project-a"), "manifest.json").writeText("partial")
        val archive = File(exportDir, "committed.kframe")
        requireSuccess(KframeArchiver.export(files, "project-a", archive))
        val destination = DocumentFileStore(otherRoot)
        requireSuccess(KframeArchiver.import(destination, archive))
        val restored = requireSuccess(ProjectRepository(destination).load("project-a"))
        assertEquals(document("project-a"), restored.second)
        assertEquals("thumbnail", File(restored.first.thumbnailPath!!).readText())
        assertTrue(File(restored.first.thumbnailPath!!).canonicalPath.startsWith(otherRoot.canonicalPath))
    }
}
