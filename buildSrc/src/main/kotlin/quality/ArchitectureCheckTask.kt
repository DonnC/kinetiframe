// SPDX-License-Identifier: Apache-2.0
package quality

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@DisableCachingByDefault(because = "Repository policy audit reads live declarations and reviewed legacy baseline")
abstract class ArchitectureCheckTask @Inject constructor(private val workers: WorkerExecutor) : DefaultTask() {
    @get:Internal abstract val repository: DirectoryProperty

    @get:Input abstract val modules: ListProperty<String>

    @get:Input abstract val reportOnly: Property<Boolean>

    @get:Classpath abstract val parserClasspath: ConfigurableFileCollection

    @TaskAction fun audit() {
        workers.processIsolation {
            classpath.from(parserClasspath)
            forkOptions.maxHeapSize = "768m"
        }.submit(ArchitectureAuditWork::class.java) {
            repository.set(this@ArchitectureCheckTask.repository)
            modules.set(this@ArchitectureCheckTask.modules)
            reportOnly.set(this@ArchitectureCheckTask.reportOnly)
        }
    }
}
