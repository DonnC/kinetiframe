// SPDX-License-Identifier: Apache-2.0
package quality

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.util.Properties

interface AuditParameters : WorkParameters {
    val repository: DirectoryProperty
    val modules: ListProperty<String>
    val reportOnly: Property<Boolean>
}

/** Compiler PSI is process-isolated, never added to the Kotlin Gradle plugin build classpath. */
abstract class ArchitectureAuditWork : WorkAction<AuditParameters> {
    override fun execute() {
        val root = parameters.repository.get().asFile
        val findings = KotlinBoundaryScanner.scan(root, parameters.modules.get())
        val kinds = findings.entries.groupBy { it.key.substringBefore('|') }
        val uiValues = UI_VALUE_KINDS.flatMap { kinds[it].orEmpty() }
        val layering = LAYERING_KINDS.flatMap { kinds[it].orEmpty() }
        val structure = findings.entries.filter { it.key.substringBefore('|') !in UI_VALUE_KINDS + LAYERING_KINDS }
        if (parameters.reportOnly.get()) {
            writeCandidate(
                File(root, "build/architecture/candidate-baseline.properties"),
                structure.filter { it.key.contains("|app/") },
                "Candidate only: reviewed legacy app debt, never automatic approval",
            )
            writeCandidate(
                File(root, "build/architecture/candidate-ui-values-baseline.properties"),
                uiValues,
                "Candidate only: inline colors, sizes and text per file",
            )
            writeCandidate(
                File(root, "build/architecture/candidate-layering-baseline.properties"),
                layering,
                "Candidate only: upward feature references, Compose outside UI, and UI reading storage",
            )
            println("Candidate baselines in ${File(root, "build/architecture").path}; ${findings.size} total findings")
            return
        }
        val baseline = Properties().apply {
            BASELINES.map { File(root, it) }.filter { it.isFile }.forEach { file -> file.reader().use { load(it) } }
        }
        val allowances = ArchitectureBaseline.allowances(baseline)
        val regressions = findings.filter { (key, count) -> count > (allowances[key] ?: 0) }
        if (regressions.isNotEmpty()) {
            throw GradleException(
                regressions.entries.joinToString("\n", "Architecture violations:\n") {
                    "${it.key}: ${it.value}"
                },
            )
        }
        println(
            "Architecture checks passed; ${structure.size} reviewed legacy violations, " +
                "${layering.sumOf { it.value }} layering violations and " +
                "${uiValues.sumOf { it.value }} inline UI values remain",
        )
    }

    private fun writeCandidate(file: File, entries: List<Map.Entry<String, Int>>, comment: String) {
        file.parentFile.mkdirs()
        val properties = Properties().apply { entries.forEach { (key, count) -> setProperty(key, count.toString()) } }
        file.writer().use { properties.store(it, comment) }
    }

    private companion object {
        val UI_VALUE_KINDS = setOf(UiValueRules.COLORS, UiValueRules.DP, UiValueRules.TEXT)

        val LAYERING_KINDS = setOf("layering", UiLayerRules.COMPOSE, UiLayerRules.UI_DATA)

        /** Structural debt, upward feature references and inline UI values. Every list only shrinks. */
        val BASELINES = listOf(
            ".architecture/legacy-baseline.properties",
            ".architecture/layering-baseline.properties",
            ".architecture/ui-values-baseline.properties",
        )
    }
}
