// SPDX-License-Identifier: Apache-2.0
package quality

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Counts values that belong in the theme, design tokens or string resources rather than inline:
 *
 * - `colors`: `Color(...)` built from literals, such as `Color(0xFF6750A4)`. Theme colors and
 *   `copy(alpha = ...)` variants of them are fine.
 * - `dp`: numeric `.dp` literals other than zero.
 * - `text`: string literals passed to `Text(...)` or as a `contentDescription`.
 *
 * The designsystem module defines the tokens, so it is not scanned. Tests and `@Preview`
 * functions are skipped too: their sample values never reach the animator.
 */
internal object UiValueRules {
    const val COLORS = "colors"
    const val DP = "dp"
    const val TEXT = "text"

    fun applies(module: String, path: String): Boolean =
        module != "designsystem" && "/src/test/" !in path && "/src/androidTest/" !in path

    fun count(file: KtFile): Map<String, Int> {
        val counts = sortedMapOf<String, Int>()
        fun add(kind: String) {
            counts[kind] = (counts[kind] ?: 0) + 1
        }
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                if (function.annotationEntries.none { it.shortName?.asString()?.startsWith("Preview") == true }) {
                    super.visitNamedFunction(function)
                }
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                val arguments = expression.valueArguments
                when (expression.calleeExpression?.text) {
                    "Color" -> if (arguments.isNotEmpty() && arguments.all { it.getArgumentExpression().isNumber() }) {
                        add(COLORS)
                    }

                    "Text" -> {
                        val text = arguments.firstOrNull { it.getArgumentName()?.asName?.asString() == "text" }
                            ?: arguments.firstOrNull()?.takeIf { it.getArgumentName() == null }
                        if (text.isWords()) add(TEXT)
                    }
                }
                super.visitCallExpression(expression)
            }

            override fun visitArgument(argument: KtValueArgument) {
                if (argument.getArgumentName()?.asName?.asString() == "contentDescription" && argument.isWords()) {
                    add(TEXT)
                }
                super.visitArgument(argument)
            }

            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val receiver = expression.receiverExpression
                if (expression.selectorExpression?.text == "dp" && receiver.isNumber() && !receiver.isZero()) {
                    add(DP)
                }
                super.visitDotQualifiedExpression(expression)
            }
        })
        return counts
    }

    private fun KtExpression?.isNumber(): Boolean = when (this) {
        is KtConstantExpression -> text.first().isDigit()
        is KtPrefixExpression -> baseExpression.isNumber()
        else -> false
    }

    private fun KtElement.isZero(): Boolean = text.trimEnd('f', 'F').toDoubleOrNull() == 0.0

    private fun KtValueArgument?.isWords(): Boolean {
        val literal = this?.getArgumentExpression() as? KtStringTemplateExpression ?: return false
        return literal.entries.any { entry -> entry.text.any(Char::isLetter) && entry.expression == null }
    }
}
