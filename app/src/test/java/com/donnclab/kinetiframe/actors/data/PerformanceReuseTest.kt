package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PerformanceReuseTest {
    @TempDir lateinit var root: File

    @Test fun `cross project reuse retains authored motion revision and source across restart`() {
        val files = DocumentFileStore(root)
        val repo = PerformanceRepository(files)
        val source = PerformanceDocument(
            performanceId = "wave", projectId = "first", actorId = "mira",
            actorRevisionId = "revision-front", displayName = "Wave", durationMicros = 1_000_000,
            poseKeys = listOf(ActorPoseKey(0, mapOf("hand" to 30f))),
            createdAtEpochMs = 0, updatedAtEpochMs = 0,
        )
        fun <T> value(result: StoreResult<T>): T = (result as StoreResult.Value).value
        value(repo.save(source))
        val copy = value(repo.copyIntoProject(source, "second"))
        assertNotEquals(source.performanceId, copy.performanceId)
        assertEquals(source.poseKeys, copy.poseKeys)
        assertEquals(source.actorRevisionId, copy.actorRevisionId)
        val reopened = PerformanceRepository(DocumentFileStore(root))
        assertEquals(2, value(reopened.listForActor("mira")).size)
        value(reopened.rename("second", copy.performanceId, "Changed", 10))
        assertEquals(source, value(reopened.load("first", "wave")))
        assertEquals(copy.performanceId, value(reopened.copyIntoProject(copy, "second")).performanceId)
    }
}
