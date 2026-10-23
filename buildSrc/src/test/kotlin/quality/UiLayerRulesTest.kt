// SPDX-License-Identifier: Apache-2.0
package quality

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UiLayerRulesTest {
    private val root = "com.donnclab.kinetiframe"

    @Test fun `compose belongs in ui code, not in product rules or storage`() {
        assertNull(UiLayerRules.violation("$root.studio.ui.canvas", "androidx.compose.ui.geometry.Offset"))
        assertNull(UiLayerRules.violation("$root.home", "androidx.compose.runtime.Composable"))
        assertEquals(
            UiLayerRules.COMPOSE,
            UiLayerRules.violation("$root.studio.domain", "androidx.compose.ui.geometry.Offset"),
        )
        assertEquals(
            UiLayerRules.COMPOSE,
            UiLayerRules.violation("$root.actors.rig.math", "androidx.compose.ui.graphics.Color"),
        )
    }

    @Test fun `ui code reaches storage only through its view model`() {
        assertEquals(
            UiLayerRules.UI_DATA,
            UiLayerRules.violation("$root.library.ui", "$root.library.data.LibraryRepository"),
        )
        assertEquals(
            UiLayerRules.UI_DATA,
            UiLayerRules.violation("$root.studio.editor", "$root.project.data.ProjectRepository"),
        )
        assertNull(UiLayerRules.violation("$root.library.ui", "$root.library.ui.LibraryIntent"))
        assertNull(UiLayerRules.violation("$root.studio.domain", "$root.project.data.ProjectRepository"))
        assertNull(UiLayerRules.violation("$root.library.ui", "$root.core.document.ProjectDocument"))
    }
}
