package com.donnclab.kinetiframe.core.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.models.StrokeNode")
data class StrokeNode(
    override val id: String,
    val command: DrawCommand,
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val rotation: Float = 0f,
    override val scaleX: Float = 1f,
    override val scaleY: Float = 1f,
    override val isSelected: Boolean = false,
    override val isVisible: Boolean = true,
    override val zIndex: Int = 0,
    override val style: NodeStyle = NodeStyle(
        mainColorHex = command.color,
        alpha = command.alpha,
    ),
) : BaseNode() {

    override fun copyWithTransform(x: Float, y: Float, rotation: Float, scaleX: Float, scaleY: Float): StrokeNode =
        copy(x = x, y = y, rotation = rotation, scaleX = scaleX, scaleY = scaleY)

    override fun copyWithSelection(isSelected: Boolean): StrokeNode = copy(isSelected = isSelected)

    override fun copyWithId(id: String): StrokeNode = copy(id = id)
}
