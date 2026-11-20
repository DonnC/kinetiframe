// SPDX-License-Identifier: Apache-2.0
package quality

import org.jetbrains.kotlin.cli.jvm.compiler.IdeaStandaloneExecutionSetup
import org.jetbrains.kotlin.com.intellij.core.CoreApplicationEnvironment
import org.jetbrains.kotlin.com.intellij.core.CoreProjectEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile

/**
 * Parses Kotlin sources into PSI trees without resolving them.
 *
 * It sets up only IntelliJ's core PSI environment and registers the Kotlin parser, instead of the
 * compiler's `KotlinCoreEnvironment`, which Kotlin 2.4 marks for rework and which loads far more
 * than the checker reads: no classpath, compiler plugins or analysis. Close it to release the PSI
 * environment.
 */
internal class KotlinSourceParser : AutoCloseable {
    private val disposable = Disposer.newDisposable("architecture checker parser")
    private val files: PsiFileFactory

    init {
        try {
            // Headless IntelliJ settings: no IDE home, config folder or plugins.
            IdeaStandaloneExecutionSetup.doSetup()
            val application = CoreApplicationEnvironment(disposable)
            application.registerFileType(KotlinFileType.INSTANCE, KotlinFileType.EXTENSION)
            application.registerParserDefinition(KotlinParserDefinition())
            files = PsiFileFactory.getInstance(CoreProjectEnvironment(disposable, application).project)
        } catch (failure: Throwable) {
            Disposer.dispose(disposable)
            throw failure
        }
    }

    /** Parses [text] as the file [name], with any line endings. */
    fun parse(name: String, text: String): KtFile = files.createFileFromText(
        name,
        KotlinLanguage.INSTANCE,
        text.replace("\r\n", "\n").replace('\r', '\n'),
    ) as KtFile

    override fun close() = Disposer.dispose(disposable)
}
