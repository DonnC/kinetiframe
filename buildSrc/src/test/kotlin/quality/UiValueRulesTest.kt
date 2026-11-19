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

    @Test fun `text in branches, positional icon descriptions and semantics is counted`() {
        fixture(
            "app",
            "main",
            """
            fun Screen(playing: Boolean, mode: Int, label: String) {
                Text(if (playing) "Pause" else "Play")
                Icon(icon, if (playing) "Pause audio" else "Play audio")
                Icon(icon, label)
                Icon(icon, null)
                Text(when (mode) { 0 -> "Off"; else -> label })
                Modifier.semantics { contentDescription = "Audio position" }
                Modifier.semantics { stateDescription = if (playing) "On" else "Off" }
                Modifier.semantics { contentDescription = label }
                Text(if (playing) label else name)
            }
            """.trimIndent(),
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("app"))
        assertEquals(5, findings["text|app/src/main/kotlin/Screen.kt"])
    }

    @Test fun `labels passed to the app's components and named text arguments are counted`() {
        fixture(
            "app",
            "main",
            """
            fun Menu(name: String) {
                ContextIconAction(Icons.Copy, "Duplicate", {})
                ContextIconAction(Icons.Copy, label = "Duplicate", onClick = {})
                ContextIconAction(Icons.Copy, name, {})
                ContextSliderButton(Icons.Volume, "Volume", 1f, {})
                StudioButton(title = "Paste", subtitle = name)
                TextField(value, {}, placeholder = "Search", label = stringResource(R.string.search))
                Clip(id = "voice", label = name)
                Image(bitmap, "Frame ${'$'}number preview")
                Image(bitmap, null)
            }
            """.trimIndent(),
        )
        val findings = KotlinBoundaryScanner.scan(root, listOf("app"))
        assertEquals(6, findings["text|app/src/main/kotlin/Screen.kt"])
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
            "@PreviewLightDark @Composable fun Swatch() { Text(\"Sample\"); Modifier.padding(4.dp) }\n" +
                "private fun previewSample() = Summary(title = \"Walk cycle\")",
        )
        assertEquals(emptyMap<String, Int>(), KotlinBoundaryScanner.scan(root, listOf("app", "designsystem")))
    }

    private fun fixture(module: String, sourceSet: String, source: String) {
        val file = File(root, "$module/src/$sourceSet/kotlin/Screen.kt")
        file.parentFile.mkdirs()
        file.writeText("package com.donnclab.kinetiframe\n$source")
    }
}
