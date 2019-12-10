import java.io.File

data class Size(val width: Int, val height: Int) {
    fun bytes(): Int {
        return width * height
    }
}

data class Position(val x: Int, val y: Int) {
    fun index(size: Size): Int {
        return y * size.width + x
    }
}

data class Layer(val size: Size, val data: List<Int>) {
    init {
        if (!data.size.equals(size.bytes())) {
            throw Exception("Invalid layer size")
        }
    }

    fun countByte(byte: Int): Int {
        return data.count { it.equals(byte) }
    }

    fun pixel(position: Position): Int {
        return data[position.index(size)]
    }
}

data class SIF(val size: Size, val layers: List<Layer>) {
    companion object {
        fun fromRaw(size: Size, data: List<Int>): SIF {
            return SIF(size, data.chunked(size.bytes()).map { Layer(size, it) })
        }
    }

    fun pixel(position: Position): Int {
        return layers.fold(2) { _, layer ->
            val newPixel = layer.pixel(position)
            if (newPixel < 2) {
                return newPixel
            }
            newPixel
        }
    }

    override fun toString(): String {
        return (0 until size.height).map { row ->
            (0 until size.width).map { col ->
                when (pixel(Position(col, row))) {
                    0 -> "  "
                    1 -> "XX"
                    else -> "??"
                }
            }.joinToString("")
        }.joinToString("\n")
    }
}

// Setup

fun size(): Size {
    return Size(25, 6)
}

fun input(): List<Int> {
    return File("input.txt").readText().map { it.toString().toInt() }
}

fun partOne(image: SIF): Int {
    val targetLayer = image.layers.minBy { it.countByte(0) }!!
    return targetLayer.countByte(1) * targetLayer.countByte(2)
}

fun partTwo(image: SIF) {
    println(image)
}

// RUN

val image = SIF.fromRaw(size(), input())

//partOne(image)
partTwo(image)