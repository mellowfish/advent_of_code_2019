import kotlin.math.truncate
import java.io.File

fun realTotalFuel(): Int {
    return moduleMasses().map { realFuelForMass(it) }.sum()
}

fun naiveTotalFuel(): Int {
    return moduleMasses().map { naiveFuelForMass(it) }.sum()
}

fun moduleMasses(): Sequence<Int> {
    return inputLines().map { it.toInt() }
}

fun inputLines(): Sequence<String> {
    return File("input.txt").bufferedReader().lineSequence()
}

fun realFuelForMass(mass: Int): Int {
    var fuel = naiveFuelForMass(mass)
    if (fuel.equals(0)) {
        return 0
    }
    else {
        return fuel + realFuelForMass(fuel)
    }
}

fun naiveFuelForMass(mass: Int): Int {
    if (mass < 7) {
        return 0
    }
    else {
        return truncate(mass.toDouble() / 3.0).toInt() - 2
    }
}

println(String.format("Naive Fuel Requirement: %d", naiveTotalFuel()))
println(String.format("Real Fuel Requirement: %d", realTotalFuel()))