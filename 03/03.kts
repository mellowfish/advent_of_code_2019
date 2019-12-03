import java.io.File
import kotlin.math.abs

typealias Point = Pair<Int, Int>

data class Point(val x: Int, val y: Int) {
    companion object {
        val origin = Point(0, 0)
    }

    fun distance(toPoint: Point = Point.origin): Int {
//        println(String.format("%s is %d away", toString(), abs(x - toPoint.x) + abs(y - toPoint.y)))
        return abs(x - toPoint.x) + abs(y - toPoint.y)
    }
}

data class LineSegment(val startPoint: Point, val instruction: String) {
    val direction = instruction.slice(0..0)
    val length = instruction.slice(1 until instruction.length).toInt()
    val endPoint = calculateEndpoint()

    fun follow(nextInstruction: String): LineSegment {
        return LineSegment(endPoint, nextInstruction)
    }

    fun isAt(point: Point): Boolean {
        val comparablePoints = comparablePoints()
        return comparablePoints.first.x <= point.x && point.x <= comparablePoints.second.x &&
                comparablePoints.first.y <= point.y && point.y <= comparablePoints.second.y
    }

    fun charAt(point: Point): String {
        if (isAt(point)) {
            if (isVerticle()) {
                return "|"
            }
            return "-"
        }
        return "."
    }

    fun isVerticle(): Boolean {
        return startPoint.x.equals(endPoint.x)
    }

    fun comparablePoints(): Pair<Point, Point> {
        if (isVerticle()) {
            return Pair(
                Point(startPoint.x, minOf(startPoint.y, endPoint.y)),
                Point(startPoint.x, maxOf(startPoint.y, endPoint.y))
            )
        }
        else {
            return Pair(
                Point(minOf(startPoint.x, endPoint.x), startPoint.y),
                Point(maxOf(startPoint.x, endPoint.x), startPoint.y)
            )
        }
    }

    fun top(): Int {
        return maxOf(startPoint.y, endPoint.y)
    }

    fun bottom(): Int {
        return minOf(startPoint.y, endPoint.y)
    }

    fun left(): Int {
        return minOf(startPoint.x, endPoint.x)
    }

    fun right(): Int {
        return maxOf(startPoint.x, endPoint.x)
    }

    fun newPoints(): List<Point> {
        if (isVerticle()) {
            if (startPoint.y < endPoint.y) {
                return (startPoint.y + 1).rangeTo(endPoint.y).map { Point(startPoint.x, it) }
            }
            return (startPoint.y - 1).downTo(endPoint.y).map { Point(startPoint.x, it) }
        }
        if (startPoint.x < endPoint.x) {
            return (startPoint.x + 1).rangeTo(endPoint.x).map { Point(it, startPoint.y) }
        }
        return (startPoint.x - 1).downTo(endPoint.x).map { Point(it, startPoint.y) }
    }

    private fun calculateEndpoint(): Point {
        return when(direction) {
            "R" -> Point(startPoint.x + length, startPoint.y)
            "L" -> Point(startPoint.x - length, startPoint.y)
            "U" -> Point(startPoint.x, startPoint.y + length)
            "D" -> Point(startPoint.x, startPoint.y - length)
            else -> throw Exception(String.format("Invalid direction: '%s'", direction))
        }
    }
}

data class Line(val directions: List<String>) {
    val segments = calculateSegments()

    companion object {
        fun fromString(raw: String): Line {
            return Line(raw.split(","))
        }
    }

    fun isAt(point: Point): Boolean {
        return totalSegmentsAt(point) > 0
    }

    fun totalSegmentsAt(point: Point): Int {
        return segments.count { it.isAt(point) }
    }

    fun segmentsAt(point: Point): List<LineSegment> {
        return segments.filter { it.isAt(point) }
    }

    fun charAt(point: Point): String {
        val matchingSegments = segmentsAt(point)
        if (matchingSegments.isEmpty()) {
            return "."
        }
        if (matchingSegments.size > 1) {
            return "+"
        }
        return matchingSegments.first().charAt(point)
    }

    fun top(): Int {
        return segments.map { it.top() }.max()!!
    }

    fun bottom(): Int {
        return segments.map { it.bottom() }.min()!!
    }

    fun left(): Int {
        return segments.map { it.left() }.min()!!
    }

    fun right(): Int {
        return segments.map { it.right() }.max()!!
    }

    fun points(): List<Point> {
        return segments.flatMap { it.newPoints() }
    }

    fun distanceToPoint(point: Point): Int {
        return points().takeWhile { !it.equals(point) }.size + 1
    }

    private fun calculateSegments(): List<LineSegment> {
        val firstSegments = mutableListOf<LineSegment>(LineSegment(Point.origin, directions.first()))
        val remainingDirections = directions.subList(1, directions.size)

        return remainingDirections.fold(firstSegments) { acc, direction -> acc.add(acc.last().follow(direction)); acc }
    }
}

data class Network(val lines: List<Line>) {
    var pointMap = createPointMap()

    fun top(): Int {
        return lines.map { it.top() }.max()!! + 1
    }

    fun bottom(): Int {
        return lines.map { it.bottom() }.min()!! - 1
    }

    fun left(): Int {
        return lines.map { it.left() }.min()!! - 1
    }

    fun right(): Int {
        return lines.map { it.right() }.max()!! + 1
    }

    fun linesAt(point: Point): List<Line> {
        return pointMap.get(point)?.distinct() ?: emptyList<Line>()
    }

    fun rows(): IntProgression {
        return top().downTo(bottom())
    }

    fun columns(): IntProgression {
        return left().rangeTo(right())
    }

    fun charAt(point: Point): String {
        if (point.equals(Point.origin)) {
            return "O"
        }

        val lines = linesAt(point)
        if (lines.isEmpty()) {
            return "."
        }

        if (lines.size > 1) {
            return "X"
        }

        return lines.first().charAt(point)
    }

    override fun toString(): String {
        var builder = StringBuilder()
        rows().forEach { row ->
            columns().forEach { column ->
                builder.append(charAt(Point(column, row)))
            }
            builder.append("\n")
        }
        return builder.toString()
    }

    fun print() {
        var writer = File("network.txt").bufferedWriter()
        rows().forEach { row ->
            columns().forEach { column ->
                writer.append(charAt(Point(column, row)))
            }
            writer.append("\n")
        }
        writer.flush()
        writer.close()
    }

    fun eachEntry(callback: (entry: Map.Entry<Point, List<Line>>) -> Any) {
        pointMap.entries.forEach { callback(it) }
    }

    fun crossPoints(): List<Point> {
        val points = mutableListOf<Point>()
        eachEntry() {
            if (it.value.distinct().size > 1 && !it.key.equals(Point.origin)) {
                points.add(it.key)
            }
        }
        return points
    }

    fun closestCrossPoint(): Point {
        return  crossPoints().minBy { it.distance() }!!
    }

    fun bestCrossPoint(): Point {
        return crossPoints().minBy { lineDistanceToPoint(it) }!!
    }

    fun lineDistanceToPoint(point: Point): Int {
        return lines.map { line -> line.distanceToPoint(point) }.sum()
    }

    private fun createPointMap(): Map<Point, MutableList<Line>> {
        val map = mutableMapOf<Point, MutableList<Line>>()
        lines.forEach { line ->
            line.points().forEach { point ->
                var existingLines = map.get(point) ?: mutableListOf<Line>()
                existingLines.add(line)
                map.set(point, existingLines)
            }
        }
        return map
    }
}

fun input(): String {
//    return "R8,U5,L5,D3\nU7,R6,D4,L4"
//    return "R75,D30,R83,U83,L12,D49,R71,U7,L72\nU62,R66,U55,R34,D71,R55,D58,R83"
//    return "R98,U47,R26,D63,R33,U87,L62,D20,R33,U53,R51\nU98,R91,D20,R16,D67,R40,U7,R15,U6,R7"
    return File("input.txt").readText()
}

fun inputNetwork(): Network {
    return Network(input().split("\n").map { Line.fromString(it) })
}

val network = inputNetwork()
network.print()
try {
    println(String.format("Manhattan distance of closest cross point: %d", network.closestCrossPoint().distance()))
}
catch (e: NullPointerException) {
    e.printStackTrace()
}
println(String.format("Line distance of best cross point: %d", network.lineDistanceToPoint(network.bestCrossPoint())))
