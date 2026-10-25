// SPDX-License-Identifier: Apache-2.0
package quality

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinBoundaryScannerTest {
    @TempDir lateinit var root: File

    @Test fun `nested generics and function types do not invent declared parameters`() {
        fixture(
            "core",
            "api/Example.kt",
            """
            package api
            fun valid(a: Map<String, Pair<Int, Int>>, b: (Int, Int) -> Unit) = Unit
            class Invalid(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int)
            """.trimIndent(),
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("core"))
        assertEquals(1, findings.size)
        assertEquals(8, findings.values.single())
        assertTrue(findings.keys.single().contains("Invalid.constructor"))
    }

    @Test fun `new public code cannot import platform UI private storage or exceed size`() {
        fixture("core", "api/Example.kt", "import android.graphics.Bitmap\n" + "// line\n".repeat(500))
        fixture("engine", "math/Example.kt", "import androidx.compose.ui.geometry.Offset")
        fixture("data", "api/Example.kt", "val mesh = com.donnclab.kinetiframe.engine.rigging.Mesh")
        val findings = KotlinBoundaryScanner.scan(root, listOf("core", "engine", "data"))
        assertEquals(4, findings.size)
        assertTrue(findings.keys.any { it.startsWith("lines|core/") })
        assertEquals(3, findings.keys.count { it.startsWith("dependency|") })
    }

    @Test fun `design system rejects product types and ViewModels while allowing Compose`() {
        fixture("designsystem", "theme/Theme.kt", "import androidx.compose.material3.MaterialTheme")
        fixture(
            "designsystem",
            "Bad.kt",
            """
            import com.donnclab.kinetiframe.engine.rigging.SceneGraph
            import com.donnclab.kinetiframe.data.Repository
            import com.donnclab.kinetiframe.core.document.Stage
            import com.donnclab.kinetiframe.ui.StudioState
            import androidx.lifecycle.ViewModel
            """.trimIndent(),
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("designsystem"))
        assertEquals(5, findings.size)
        assertTrue(findings.keys.all { it.startsWith("dependency|") })
    }

    @Test fun `library modules cannot reference app feature packages`() {
        fixture("engine", "Bad.kt", "import com.donnclab.kinetiframe.studio.ui.StudioScreen")
        fixture("core", "Bad.kt", "val x = com.donnclab.kinetiframe.home.HomeRoute()")
        fixture(
            "app",
            "studio/Good.kt",
            "package com.donnclab.kinetiframe.studio\nimport com.donnclab.kinetiframe.library.ui.Card",
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("engine", "core", "app"))
        assertEquals(2, findings.size)
        assertTrue(findings.keys.all { it.startsWith("dependency|") && !it.contains("app/") })
    }

    @Test fun `app code cannot live in layer or module named packages`() {
        fixture("app", "ui/Old.kt", "package com.donnclab.kinetiframe.ui.home")
        fixture("app", "engine/Old.kt", "package com.donnclab.kinetiframe.engine")
        fixture("app", "home/New.kt", "package com.donnclab.kinetiframe.home")
        fixture("app", "Root.kt", "package com.donnclab.kinetiframe")
        val findings = KotlinBoundaryScanner.scan(root, listOf("app"))
        assertEquals(
            setOf("com.donnclab.kinetiframe.ui.home", "com.donnclab.kinetiframe.engine"),
            findings.keys.map { it.substringAfterLast('|') }.toSet(),
        )
    }

    @Test fun `non UI modules cannot depend on design system types`() {
        listOf("core", "engine", "data").forEach { module ->
            fixture(module, "Bad.kt", "import com.donnclab.kinetiframe.designsystem.theme.EditorTokens")
        }
        assertEquals(3, KotlinBoundaryScanner.scan(root, listOf("core", "engine", "data")).size)
    }

    @Test fun `Windows and Unix source have the same declared parameter counts`() {
        val source = "fun valid(\n a: Int,\n b: Int,\n c: Int\n) = Unit\n"
        fixture("designsystem", "Unix.kt", source)
        fixture("designsystem", "Windows.kt", source.replace("\n", "\r\n"))
        assertTrue(KotlinBoundaryScanner.scan(root, listOf("designsystem")).isEmpty())
        fixture(
            "designsystem",
            "Invalid.kt",
            "fun invalid(\r\n a: Int,\r\n b: Int,\r\n c: Int,\r\n d: Int,\r\n e: Int,\r\n f: Int,\r\n g: Int\r\n) = Unit",
        )
        assertEquals(listOf(7), KotlinBoundaryScanner.scan(root, listOf("designsystem")).values.toList())
    }

    @Test fun `functions allow six parameters and constructors seven`() {
        fixture(
            "core",
            "Limits.kt",
            """
            fun six(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) = Unit
            fun seven(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int) = Unit
            class Seven(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int)
            """.trimIndent(),
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("core"))
        assertEquals(1, findings.size)
        assertTrue(findings.keys.single().contains(".seven("))
    }

    @Test fun `data classes are exempt and composables count only required parameters`() {
        fixture(
            "designsystem",
            "Compose.kt",
            """
            data class Style(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int, val f: Int, val g: Int, val h: Int)
            @Composable
            fun Card(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, a: Int = 0, b: Int = 0, c: Int = 0, d: Int = 0)
            @Composable
            fun Crowded(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, modifier: Modifier = Modifier)
            """.trimIndent(),
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("designsystem"))
        assertEquals(mapOf(".Crowded" to 7), findings.mapKeys { it.key.substringAfterLast('|').substringBefore('(') })
    }

    private fun fixture(module: String, path: String, source: String) {
        val file = File(root, "$module/src/main/kotlin/$path")
        file.parentFile.mkdirs()
        file.writeText(source)
    }
}
