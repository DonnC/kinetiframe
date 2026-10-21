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
        if (parameters.reportOnly.get()) {
            val candidate = File(root, "build/architecture/candidate-baseline.properties")
            candidate.parentFile.mkdirs()
            val existing = Properties().apply {
                findings.filterKeys {
                    it.contains("|app/")
                }.forEach { (key, count) -> setProperty(key, count.toString()) }
            }
            candidate.writer().use {
                existing.store(it, "Candidate only: reviewed legacy app debt, never automatic approval")
            }
            println("Candidate baseline: ${candidate.path}; ${findings.size} total findings")
            return
        }
        val file = File(root, ".architecture/legacy-baseline.properties")
        val baseline = Properties().apply { if (file.isFile) file.reader().use { load(it) } }
        val allowances = ArchitectureBaseline.allowances(baseline)
        val regressions = findings.filter { (key, count) -> count > (allowances[key] ?: 0) }
        if (regressions.isNotEmpty()) {
            throw GradleException(
                regressions.entries.joinToString("\n", "Architecture violations:\n") {
                    "${it.key}: ${it.value}"
                },
            )
        }
        println("Architecture checks passed; ${findings.size} reviewed legacy violations remain")
    }
}
