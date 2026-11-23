// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.figure

/** A parsed drawing command. Coordinates are absolute, in the owning bone's space. */
sealed interface PathCommand {
    data class MoveTo(val x: Float, val y: Float) : PathCommand
    data class LineTo(val x: Float, val y: Float) : PathCommand
    data class QuadTo(val cx: Float, val cy: Float, val x: Float, val y: Float) : PathCommand
    data class CubicTo(val c1x: Float, val c1y: Float, val c2x: Float, val c2y: Float, val x: Float, val y: Float) :
        PathCommand
    data object Close : PathCommand
}

/** Reads the SVG path subset figures use: absolute M, L, Q, C and Z, with repeated arguments. */
object FigurePath {
    const val MAX_LENGTH = 4_000

    fun parse(data: String): List<PathCommand> {
        require(data.length <= MAX_LENGTH) { "Figure path is too long" }
        val tokens = TOKEN.findAll(data).map { it.value }.toList()
        val commands = mutableListOf<PathCommand>()
        var index = 0
        var command: Char? = null
        fun number(): Float {
            val token = tokens.getOrNull(index++) ?: throw IllegalArgumentException("Figure path ends early")
            return requireNotNull(token.toFloatOrNull()?.takeIf { it.isFinite() }) { "Bad number in figure path" }
        }
        while (index < tokens.size) {
            val token = tokens[index]
            if (token.length == 1 && token[0].isLetter()) {
                command = token[0]
                index++
                if (command == 'Z' || command == 'z') {
                    commands += PathCommand.Close
                    command = null
                }
                continue
            }
            commands += when (command) {
                'M' -> PathCommand.MoveTo(number(), number()).also { command = 'L' }
                'L' -> PathCommand.LineTo(number(), number())
                'Q' -> PathCommand.QuadTo(number(), number(), number(), number())
                'C' -> PathCommand.CubicTo(number(), number(), number(), number(), number(), number())
                else -> throw IllegalArgumentException("Unsupported figure path command")
            }
        }
        require(commands.firstOrNull() is PathCommand.MoveTo) { "Figure path must start with M" }
        return commands
    }

    private val TOKEN = Regex("[MLQCZmlqcz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")
}
