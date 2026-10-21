package com.donnclab.kinetiframe.domain.manager

import androidx.compose.ui.graphics.toArgb
import com.donnclab.kinetiframe.models.TextNode
import com.donnclab.kinetiframe.ui.TimelineState

/**
 * Manager for text operations.
 */
class TextManager(private val updateState: ((TimelineState) -> TimelineState) -> Unit) {
    fun addText(text: String, x: Float, y: Float) {
        updateState { state ->
            val options = state.tool.textToolOptions
            val id = "text_${System.currentTimeMillis()}"
            val node = TextNode(
                id = id,
                text = text,
                fontFileName = options.fontFileName,
                fontSizeSp = options.fontSizeSp,
                style = com.donnclab.kinetiframe.models.NodeStyle(
                    mainColorHex = options.textColor.toArgb(),
                ),
                alignment = options.alignment,
                x = x,
                y = y,
            )
            state.copy(
                scene = state.scene.copy(
                    nodes = state.scene.nodes + (id to node),
                    activeNodeId = id,
                ),
            )
        }
    }

    fun updateText(id: String, newText: String) {
        updateState { state ->
            val node = findTextNode(state, id) ?: return@updateState state
            replaceTextNode(state, node.copy(text = newText))
        }
    }

    fun selectTextNode(id: String?) {
        updateState { state ->
            val updatedNodes = state.scene.nodes.mapValues { entry ->
                val node = entry.value
                if (node is TextNode) {
                    node.copy(isSelected = entry.key == id)
                } else {
                    node
                }
            }
            state.copy(
                scene = state.scene.copy(
                    nodes = updatedNodes,
                    activeNodeId = id,
                ),
            )
        }
    }

    fun deleteTextNode(id: String) {
        updateState { state ->
            state.copy(
                scene = state.scene.copy(
                    nodes = state.scene.nodes.filterKeys { it != id },
                    activeNodeId = if (state.scene.activeNodeId == id) null else state.scene.activeNodeId,
                ),
            )
        }
    }

    fun updateTextNode(node: TextNode) {
        updateState { state ->
            replaceTextNode(state, node)
        }
    }

    private fun findTextNode(state: TimelineState, id: String): TextNode? =
        (state.scene.nodes[id] ?: state.selection.selectedContent?.nodes?.get(id)) as? TextNode

    /**
     * A selected node is temporarily detached from the scene while the selection
     * transform is active. Updates must target that live selection copy so changing a
     * font, colour, or text value never makes the node disappear or revert on commit.
     */
    private fun replaceTextNode(state: TimelineState, node: TextNode): TimelineState {
        val selectedContent = state.selection.selectedContent
        return if (selectedContent?.nodes?.containsKey(node.id) == true) {
            state.copy(
                selection = state.selection.copy(
                    selectedContent = selectedContent.copy(nodes = selectedContent.nodes + (node.id to node)),
                ),
            )
        } else {
            state.copy(scene = state.scene.copy(nodes = state.scene.nodes + (node.id to node)))
        }
    }
}
