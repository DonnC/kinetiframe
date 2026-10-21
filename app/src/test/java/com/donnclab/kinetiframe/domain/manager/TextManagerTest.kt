package com.donnclab.kinetiframe.domain.manager

import androidx.compose.ui.geometry.Rect
import com.donnclab.kinetiframe.models.TextNode
import com.donnclab.kinetiframe.ui.SelectedContent
import com.donnclab.kinetiframe.ui.SelectionState
import com.donnclab.kinetiframe.ui.TimelineState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextManagerTest {

    @Test
    fun `editing a selected text node updates the vector selection copy`() {
        val original = TextNode(id = "title", text = "Before", fontSizeSp = 36f)
        var state = TimelineState(
            selection = SelectionState(
                active = true,
                selectedContent = SelectedContent(
                    nodes = mapOf(original.id to original),
                    bounds = Rect(0f, 0f, 100f, 48f),
                ),
            ),
        )
        val manager = TextManager { reducer -> state = reducer(state) }

        manager.updateText(original.id, "Line one\nLine two")

        val selected = state.selection.selectedContent?.nodes?.get(original.id) as TextNode
        assertEquals("Line one\nLine two", selected.text)
        assertFalse(state.scene.nodes.containsKey(original.id))
    }

    @Test
    fun `style updates keep a selected text node editable until selection commits`() {
        val original = TextNode(id = "caption", fontFileName = "inter_regular.ttf", fontSizeSp = 24f)
        var state = TimelineState(
            selection = SelectionState(
                active = true,
                selectedContent = SelectedContent(nodes = mapOf(original.id to original)),
            ),
        )
        val manager = TextManager { reducer -> state = reducer(state) }

        manager.updateTextNode(original.copy(fontFileName = "bangers_regular.ttf", fontSizeSp = 72f))

        val selected = state.selection.selectedContent?.nodes?.get(original.id) as TextNode
        assertEquals("bangers_regular.ttf", selected.fontFileName)
        assertEquals(72f, selected.fontSizeSp)
        assertTrue(state.selection.active)
    }
}
