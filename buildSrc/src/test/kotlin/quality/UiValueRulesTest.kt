// SPDX-License-Identifier: Apache-2.0
package quality

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UiValueRulesTest {
    @TempDir lateinit var root: File

    @Test fun `literal colors, sizes and text are counted per file`() {
        fixture(
            "app",
            "main",
            """
            fun Screen(name: String) {
                Color(0xFF6750A4)
                Color(1f, 0f, 0f)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                Modifier.padding(8.dp).size(0.dp).offset(spacing.dp)
                Text("Save project")
                Text(text = "Hello ${'$'}name")
                Text(name)
                Text("${'$'}name")
                Icon(icon, contentDescription = "Undo")
                Icon(icon, contentDescription = null)
            }
            """.trimIndent(),
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("app"))
        assertEquals(2, findings["colors|app/src/main/kotlin/Screen.kt"])
        assertEquals(1, findings["dp|app/src/main/kotlin/Screen.kt"])
        assertEquals(3, findings["text|app/src/main/kotlin/Screen.kt"])
    }

    @Test fun `design system, tests and previews may use literal values`() {
        val source = """
            fun Swatch() { Color(0xFF000000); Text("Sample"); Modifier.padding(4.dp) }
        """.trimIndent()
        fixture("designsystem", "main", source)
        fixture("app", "test", source)
        fixture(
            "app",
            "main",
            "@PreviewLightDark @Composable fun Swatch() { Text(\"Sample\"); Modifier.padding(4.dp) }",
        )
        assertEquals(emptyMap<String, Int>(), KotlinBoundaryScanner.scan(root, listOf("app", "designsystem")))
    }

    private fun fixture(module: String, sourceSet: String, source: String) {
        val file = File(root, "$module/src/$sourceSet/kotlin/Screen.kt")
        file.parentFile.mkdirs()
        file.writeText(source)
    }
}
