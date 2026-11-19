// SPDX-License-Identifier: Apache-2.0
package quality

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtWhenExpression

/**
 * Counts values that belong in the theme, design tokens or string resources rather than inline:
 *
 * - `colors`: `Color(...)` built from literals, such as `Color(0xFF6750A4)`. Theme colors and
 *   `copy(alpha = ...)` variants of them are fine.
 * - `dp`: numeric `.dp` literals other than zero.
 * - `text`: string literals passed to `Text(...)`, as an `Icon`'s or `Image`'s description, as the label of the
 *   app's `ContextIconAction` and `ContextSliderButton`, as a `contentDescription`, `label`,
 *   `title`, `subtitle`, `placeholder` or `supportingText` argument, or assigned to a semantics
 *   content or state description. A literal in a branch of an `if` or `when` counts too.
 *
 * The designsystem module defines the tokens, so it is not scanned. Tests, `@Preview` functions
 * and private preview sample builders (`previewSample()`) are skipped too: their sample values
 * never reach the animator.
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
                val preview = function.annotationEntries.any { it.shortName?.asString()?.startsWith("Preview") == true }
                // Private sample builders for previews, such as `previewSample()`.
                val sample =
                    function.hasModifier(KtTokens.PRIVATE_KEYWORD) && function.name?.startsWith("preview") == true
                if (!preview && !sample) super.visitNamedFunction(function)
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

                    // Icon(imageVector, contentDescription, ...), Image(bitmap, contentDescription,
                    // ...) and the app's icon menu items and sliders, ContextIconAction(icon,
                    // label, ...): the text may be positional.
                    in ICON_LABEL_CALLS -> {
                        val description = arguments.getOrNull(1)?.takeIf { it.getArgumentName() == null }
                        if (description.isWords()) add(TEXT)
                    }
                }
                super.visitCallExpression(expression)
            }

            override fun visitArgument(argument: KtValueArgument) {
                if (argument.getArgumentName()?.asName?.asString() in TEXT_ARGUMENTS && argument.isWords()) {
                    add(TEXT)
                }
                super.visitArgument(argument)
            }

            // `semantics { contentDescription = "..." }` and its state description.
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                if (expression.operationToken == KtTokens.EQ &&
                    expression.left?.text in DESCRIPTION_PROPERTIES &&
                    expression.right.isWords()
                ) {
                    add(TEXT)
                }
                super.visitBinaryExpression(expression)
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

    private val DESCRIPTION_PROPERTIES = setOf("contentDescription", "stateDescription")

    /** Named arguments that reach the screen, whichever component takes them. */
    private val TEXT_ARGUMENTS =
        setOf("contentDescription", "label", "title", "subtitle", "placeholder", "supportingText")

    /** Components whose second argument is the visible or spoken label. */
    private val ICON_LABEL_CALLS = setOf("Icon", "Image", "ContextIconAction", "ContextSliderButton")

    private fun KtValueArgument?.isWords(): Boolean = this?.getArgumentExpression().isWords()

    /** A string literal with words, or an `if` or `when` with such a literal in one of its branches. */
    private fun KtExpression?.isWords(): Boolean = when (this) {
        is KtStringTemplateExpression ->
            entries.any { entry -> entry.text.any(Char::isLetter) && entry.expression == null }

        is KtIfExpression -> then.isWords() || `else`.isWords()

        is KtWhenExpression -> entries.any { it.expression.isWords() }

        is KtParenthesizedExpression -> expression.isWords()

        else -> false
    }
}
