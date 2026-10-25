package com.donnclab.kinetiframe.export.ui

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.export.domain.VideoExporter
import com.donnclab.kinetiframe.export.model.ExportPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val video = File("cartoon.mp4")

    private class FakeExporter(private val result: StoreResult<File>) : VideoExporter {
        var exportedAt: StageDimensions? = null
        val saved = mutableListOf<String>()

        override fun projectName(projectId: String) = "My Cartoon"

        override suspend fun export(
            projectId: String,
            targetDimensions: StageDimensions?,
            onProgress: (Float) -> Unit,
        ): StoreResult<File> {
            exportedAt = targetDimensions
            onProgress(0.5f)
            return result
        }

        override fun saveToGallery(file: File, displayName: String): StoreResult<Unit> {
            saved += displayName
            return StoreResult.Value(Unit)
        }
    }

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    @Test fun `an export renders at the chosen preset and is saved and shared under the project's name`() =
        runTest(dispatcher) {
            val exporter = FakeExporter(StoreResult.Value(video))
            val model = ExportViewModel(exporter, "project-a", dispatcher)
            advanceUntilIdle()
            assertEquals("My Cartoon", model.state.value.displayName)

            val square = ExportPresets.Presets.first { it.dimensions == StageDimensions.Square }
            model.handleIntent(ExportIntent.ChoosePreset(square))
            model.handleIntent(ExportIntent.Start)
            advanceUntilIdle()
            assertEquals(StageDimensions.Square, exporter.exportedAt)
            assertEquals(ExportPhase.Done(video), model.state.value.phase)
            assertEquals(0.5f, model.state.value.progress)

            model.handleIntent(ExportIntent.SaveToGallery)
            advanceUntilIdle()
            assertEquals(ExportEffect.ShowMessage("Saved to your gallery"), model.effect.first())
            assertEquals(listOf("My Cartoon"), exporter.saved)

            model.handleIntent(ExportIntent.Share)
            assertEquals(ExportEffect.Share(video, "Share My Cartoon"), model.effect.first())
        }

    @Test fun `a failed export shows its reason`() = runTest(dispatcher) {
        val model =
            ExportViewModel(FakeExporter(StoreResult.Failure("Project has no frames to export")), "p", dispatcher)

        model.handleIntent(ExportIntent.Start)
        advanceUntilIdle()

        assertEquals(ExportPhase.Failed("Project has no frames to export"), model.state.value.phase)
    }
}
