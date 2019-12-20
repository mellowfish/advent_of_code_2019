import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.PI

data class Point(val x: Int, val y: Int) {
    fun angleTo(other: Point): Double {
//        println(String.format("%s -> %s: %s (%s)", toString(), other.toString(), vectorTo(other).toString(), vectorTo(other).angle().toString()))
        return vectorTo(other).angle()
    }

    fun vectorTo(other: Point): Point {
        return Point(other.x - x, other.y - y)
    }

    fun distanceTo(other: Point): Double {

        return sqrt((maxOf(x, other.x) - minOf(x, other.x)).toDouble().pow(2.toDouble()) + (maxOf(y, other.y) - minOf(y, other.y)).toDouble().pow(2.toDouble()))
    }

    fun angle(): Double {
        val rawAngle = atan2(y.toDouble() * -1.0, x.toDouble())
        if (rawAngle < 0)
            return (2 * PI) + rawAngle
        else
            return rawAngle
    }
}

data class Asteroid(val point: Point, val detectionVectors: MutableMap<Double, MutableList<Asteroid>> = mutableMapOf()) {
    override fun toString(): String {
        return point.toString()
    }

    fun detect(other: Asteroid) {
        if (this == other) {
            return
        }
        val angle = angleTo(other)
        var lineup = detectionVectors.getOrDefault(angle, mutableListOf<Asteroid>())
        lineup.add(other)
        detectionVectors.put(angle, lineup)
    }

    fun angleTo(other: Asteroid): Double {
        return point.angleTo(other.point)
    }

    fun distanceTo(other: Asteroid): Double {
        return point.distanceTo(other.point)
    }

    fun totalDetectedAsteroids(): Int {
        return detectionVectors.size
    }

    fun sortDetectionVectors() {
        detectionVectors.forEach { entry -> entry.value.sortBy { other -> distanceTo(other) } }
    }

    fun asteroidsToDestroy(): List<Asteroid> {
        return copy().destroyAsteroids()
    }

    private fun destroyAsteroids(): List<Asteroid> {
        val vaporizedAsteroids = mutableListOf<Asteroid>()
        val angles = detectionVectors.keys.sorted()
        val totalTargets = detectionVectors.values.map { it.size }.sum()
        val totalAngles = angles.size

        var startAngle = PI / 2.0

        var angleIndex = angles.indexOfFirst { angle -> angle >= startAngle }

        while (vaporizedAsteroids.size < totalTargets) {
            val angle = angles[angleIndex]
//            println(angle * (180.0/PI))
            var possibleAsteroids = detectionVectors[angle]!!
            if (possibleAsteroids.any()) {
//                println(possibleAsteroids.get(0))
                vaporizedAsteroids.add(possibleAsteroids.removeAt(0))
            }

            if (angleIndex == 0) {
                angleIndex = totalAngles
            }
            angleIndex = angleIndex - 1
        }
        return vaporizedAsteroids
    }

    fun toInt(): Int {
        return (point.x * 100) + point.y
    }
}

data class AsteroidMap(val asteroids: List<Asteroid>) {
    init {
        detectAsteroids()
    }
    val xs = asteroids.map { it.point.x }
    val ys = asteroids.map { it.point.y }
    val left = xs.min()!!
    val right = xs.max()!!
    val top = ys.min()!!
    val bottom = ys.max()!!
    val map = plotAsteroids()
    val maxAsteroidsDetected = topAsteroid().totalDetectedAsteroids()
    val maxAsteroidsDetectedDigits = maxAsteroidsDetected.toString().length + 1

    companion object {
        fun fromLines(lines: List<String>): AsteroidMap {
            val asteroids = mutableListOf<Asteroid>()
            lines.forEachIndexed { y, row ->
                row.toCharArray().forEachIndexed { x, char ->
                    if (char.equals('#')) {
                        asteroids.add(Asteroid(Point(x, y)))
                    }
                }
            }
            return AsteroidMap(asteroids)
        }
    }

    fun topAsteroid(): Asteroid {
        return asteroids.maxBy { asteroid -> asteroid.totalDetectedAsteroids() }!!
    }

    override fun toString(): String {
        return (top until bottom + 1).map { y ->
            (left until right + 1).map { x -> if (map.contains(Point(x, y))) "#" else "." }.joinToString("")
        }.joinToString("\n")
    }

    fun toStringWithCounts(): String {
        return (top until bottom + 1).map { y ->
            (left until right + 1).map { x ->
                if (map.contains(Point(x, y)))
                    String.format("%" + maxAsteroidsDetectedDigits.toString() + "d", map[Point(x, y)]!!.totalDetectedAsteroids())
                else
                    String.format("%" + maxAsteroidsDetectedDigits.toString() + "s", ".")
            }.joinToString("")
        }.joinToString("\n")
    }

    fun get(point: Point): Asteroid? {
        return map.get(point)
    }

    private fun detectAsteroids() {
        asteroids.forEach { first ->
            asteroids.forEach { second ->
                first.detect(second)
            }
            first.sortDetectionVectors()
        }
    }

    private fun plotAsteroids(): Map<Point, Asteroid> {
        return asteroids.fold(mapOf<Point, Asteroid>()) { map, asteroid -> map.plus(Pair(asteroid.point, asteroid)) }
    }
}

fun partOne(asteroidMap: AsteroidMap): Int {
//    println(asteroidMap)
//    println(asteroidMap.toStringWithCounts())
    return asteroidMap.topAsteroid().totalDetectedAsteroids()
}

fun partTwo(asteroidMap: AsteroidMap): Int {
    return asteroidMap.topAsteroid().asteroidsToDestroy().get(199).toInt()
}

fun input(): String {
    return File("input.txt").readText()
//    return  ""
}

fun asteroidMap(): AsteroidMap {
    return AsteroidMap.fromLines(input().lines().toList())
}

val asteroidMap = asteroidMap()
//partOne(asteroidMap)
partTwo(asteroidMap)