package com.donnclab.kinetiframe.engine.drawing

/** Fill colour independently of ink. Ink alpha is a barrier, not the replacement colour.
 * Every pixel enters the queue once, bounding work/memory even for an entirely open sketch. */
object SketchColourFill {
    fun apply(ink: IntArray, colours: IntArray, width: Int, height: Int, x: Int, y: Int, colour: Int): IntArray {
        require(
            width > 0 && height > 0 && width.toLong() * height <= 4_194_304 && ink.size == width * height &&
                colours.size == ink.size,
        )
        val result = colours.copyOf()
        if (x !in 0 until width || y !in 0 until height) return result
        val start = y * width + x
        val old = colours[start]
        if (old == colour || ink[start] ushr 24 >= 24) return result
        val queue = IntArray(ink.size)
        val visited = BooleanArray(ink.size)
        var head = 0
        var tail = 0
        fun offer(index: Int) {
            if (!visited[index]) {
                visited[index] = true
                if (ink[index] ushr 24 < 24 && colours[index] == old) queue[tail++] = index
            }
        }
        offer(start)
        while (head < tail) {
            val index = queue[head++]
            result[index] = colour
            val column = index % width
            if (column > 0) offer(index - 1)
            if (column + 1 < width) offer(index + 1)
            if (index >= width) offer(index - width)
            if (index + width < ink.size) offer(index + width)
        }
        return result
    }
}
