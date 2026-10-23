// SPDX-License-Identifier: Apache-2.0
package quality

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FeatureLayeringTest {
    @TempDir lateinit var root: File

    @Test fun `a feature may use the features below it but not above`() {
        assertNull(FeatureLayering.violation("studio", "com.donnclab.kinetiframe.actors.render.ActorFrameRenderer"))
        assertNull(FeatureLayering.violation("actors", "com.donnclab.kinetiframe.project.model.Layer"))
        assertEquals("studio", FeatureLayering.violation("actors", "com.donnclab.kinetiframe.studio.StudioState"))
        assertEquals(
            "actors",
            FeatureLayering.violation("project", "com.donnclab.kinetiframe.actors.data.ActorFileStore"),
        )
    }

    @Test fun `the root package, resources and other libraries are not features`() {
        assertNull(FeatureLayering.violation("project", "com.donnclab.kinetiframe.R"))
        assertNull(FeatureLayering.violation("project", "com.donnclab.kinetiframe.MainActivity"))
        assertNull(FeatureLayering.violation("project", "androidx.compose.runtime.Composable"))
    }

    @Test fun `upward references are counted per file and target in app main code only`() {
        val source = """
            package com.donnclab.kinetiframe.actors.ui
            import com.donnclab.kinetiframe.studio.StudioState
            import com.donnclab.kinetiframe.project.model.Layer
            fun mode() = com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP
        """.trimIndent()
        fixture("main", source)
        fixture("test", source)
        val findings = KotlinBoundaryScanner.scan(root, listOf("app")).filterKeys { it.startsWith("layering|") }
        assertEquals(mapOf("layering|app/src/main/kotlin/Actor.kt|studio" to 2), findings)
    }

    private fun fixture(sourceSet: String, source: String) {
        val file = File(root, "app/src/$sourceSet/kotlin/Actor.kt")
        file.parentFile.mkdirs()
        file.writeText(source)
    }
}
