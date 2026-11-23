// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.figure

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Checks that a drawn figure only refers to bones, colours and curves that exist. */
object FigureValidation {
    const val MAX_PIECES = 96
    const val MAX_VARIANTS = 16
    const val MAX_SHAPES = 24
    const val MAX_CHAIN = 32

    fun validate(figure: DrawnFigure, graph: SceneGraph) {
        require(figure.palette.isNotEmpty() && figure.palette.size <= 64) { "Figure needs a palette" }
        figure.palette.values.forEach { require(COLOUR.matches(it)) { "Figure colour must be #RRGGBB" } }
        require(figure.ink in figure.palette) { "Figure ink colour is not in its palette" }
        require(figure.lineWidth in 0f..64f) { "Figure line width is out of range" }
        val ids = figure.tubes.map { it.id } + figure.parts.map { it.id } + figure.surfaces.map { it.id }
        require(ids.size <= MAX_PIECES) { "Figure has too many pieces" }
        require(ids.size == ids.toSet().size) { "Figure pieces must have unique IDs" }
        val partIds = figure.parts.map { it.id }.toSet()
        figure.tubes.forEach { tube ->
            require(tube.boneIds.size in 1..MAX_CHAIN) { "Figure tube needs a chain of bones" }
            tube.boneIds.forEach { require(it in graph.bones) { "Figure tube names an unknown bone" } }
            tube.boneIds.zipWithNext().forEach { (parent, child) ->
                require(graph.bones.getValue(child).parentId == parent) { "Figure tube bones must form a chain" }
            }
            require(tube.width > 0f && tube.width <= 1_000f) { "Figure tube width is out of range" }
            require(tube.endWidth > 0f && tube.endWidth <= 1_000f) { "Figure tube width is out of range" }
            require(tube.fill in figure.palette) { "Figure tube colour is not in its palette" }
            require(tube.rootPartId == null || tube.rootPartId in partIds) { "Figure tube names an unknown part" }
        }
        figure.parts.forEach { part ->
            require(part.boneId in graph.bones) { "Figure part names an unknown bone" }
            require(part.variants.isNotEmpty() && part.variants.size <= MAX_VARIANTS) { "Figure part needs drawings" }
            part.variants.values.forEach { shapes(figure, it) }
        }
        figure.surfaces.forEach { surface ->
            require(surface.bones.isNotEmpty() && surface.bones.size <= MAX_CHAIN) { "Figure surface needs bones" }
            surface.bones.forEach { bound ->
                require(bound.boneId in graph.bones) { "Figure surface names an unknown bone" }
                require(bound.length > 0f) { "Figure surface bone length must be positive" }
                bound.bindWorld.inverse()
            }
            require(surface.falloff in 1f..16f) { "Figure surface falloff is out of range" }
            shapes(figure, surface.shapes)
        }
    }

    private fun shapes(figure: DrawnFigure, shapes: List<FigureShape>) {
        require(shapes.isNotEmpty() && shapes.size <= MAX_SHAPES) { "Figure piece needs between 1 and 24 shapes" }
        shapes.forEach { shape ->
            FigurePath.parse(shape.path)
            require(shape.fill == null || shape.fill in figure.palette) { "Figure shape colour is not in its palette" }
            require(shape.line in 0f..8f) { "Figure shape line is out of range" }
        }
    }

    private val COLOUR = Regex("#[0-9A-Fa-f]{6}")
}
