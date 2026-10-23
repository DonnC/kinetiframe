package com.donnclab.kinetiframe.project.data

import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ActorDependencyMatchTest {
    @TempDir lateinit var root: File

    @Test fun `only validated equal revision data can differ in formatting`() {
        val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Body", length = 10f)))
        val revision = ActorRevision(revisionId = "rest", actorId = "dot", sceneGraph = graph, createdAtEpochMs = 0)
        val a = File(root, "first.json").apply { writeText(Json.encodeToString(revision)) }
        val b = File(root, "second.json").apply {
            writeText(
                Json {
                    prettyPrint = true
                    encodeDefaults = true
                }.encodeToString(revision),
            )
        }
        assertTrue(ActorDependencyMatch.equivalentRevision("revisions/rest.json", a, b))
        assertFalse(ActorDependencyMatch.equivalentRevision("artwork/rest.json", a, b))
        b.writeText(
            Json.encodeToString(
                revision.copy(
                    sceneGraph = graph.copy(
                        bones =
                        graph.bones + ("root" to graph.bones.getValue("root").withRest(length = 20f)),
                    ),
                ),
            ),
        )
        assertFalse(ActorDependencyMatch.equivalentRevision("revisions/rest.json", a, b))
        b.writeText("corrupt")
        assertFalse(ActorDependencyMatch.equivalentRevision("revisions/rest.json", a, b))
    }
}
