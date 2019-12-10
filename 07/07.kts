import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.math.pow
import kotlin.system.exitProcess

class Program(val rawSequence: List<Int>) {
    companion object {
        fun fromString(rawString: String): Program {
            return Program(rawString.split(",").map { it.toInt() })
        }
    }

    fun atTimeOfAlarm(): Program {
        return at(12, 2)
    }

    fun at(noun: Int, verb: Int): Program {
        var newSequence = rawSequence.toMutableList()
        newSequence[1] = noun
        newSequence[2] = verb
        return Program(newSequence.toList())
    }

    fun input(): Int {
        return rawSequence[1] * 100 + rawSequence[2]
    }

    fun execute(input: ProgramInput = StandardProgramInput(), output: ProgramOutput = StandardProgramOutput()): Program {
        var execution = Execution(this, 0, input, output)

        while(execution.isIncomplete()) { execution = execution.step() }

        return execution.program
    }

    fun blockableExecutor(input: ProgramInput = StandardProgramInput(), output: ProgramOutput = StandardProgramOutput()): BlockableExecutor {
        return BlockableExecutor(Execution(this, 0, input, output))
    }

    fun instruction(): Int {
        return get(0)
    }

    operator fun get(index: Int): Int {
        return rawSequence[index]
    }

    operator fun set(index: Int, value: Int): Program {
        var newSequence = rawSequence.toMutableList()
        newSequence[index] = value
        return Program(newSequence.toList())
    }
}

data class BlockableExecutor(var currentExecution: Execution) {
    fun executeUntilBlock(): ProgramOutput {
        while(currentExecution.isIncomplete() && currentExecution.nonBlocking()) { currentExecution = currentExecution.step() }
        return currentExecution.output
    }

    fun executeUntilBlock(input: ProgramInput): ProgramOutput {
        currentExecution = currentExecution.withInput(input)
        return executeUntilBlock()
    }

    fun halted(): Boolean {
        return currentExecution.halted()
    }
}

data class Execution(
        val program: Program,
        val instructionPointer: Int = 0,
        val input: ProgramInput = StandardProgramInput(),
        val output: ProgramOutput = StandardProgramOutput()
) {
    val instruction = program[instructionPointer]
    val opcode = instruction % 100
    val args = (instruction - opcode) / 100

    fun nextExecution(nextProgram: Program, nextInsructionPointer: Int): Execution {
        return Execution(nextProgram, nextInsructionPointer, input, output)
    }

    fun withInput(newInput: ProgramInput) : Execution {
        return Execution(program, instructionPointer, newInput, output)
    }

    fun nonBlocking(): Boolean {
        return when(opcode) {
            3 -> input.ready()
            else -> true
        }
    }

    fun step(): Execution {
//        println(this)
        return when(opcode) {
            1 -> add()
            2 -> multiply()
            3 -> readInput()
            4 -> writeOutput()
            5 -> jumpIfTrue()
            6 -> jumpIfFalse()
            7 -> isLessThan()
            8 -> isEqual()
            99 -> this
            else -> throw Exception(String.format("Invalid opcode: %d", opcode))
        }
    }

    fun argMode(argIndex: Int): Int {
        return ((args % (10.0).pow(argIndex + 1).toInt()) - (args % (10.0).pow(argIndex).toInt())) / (10.0).pow(argIndex).toInt()
    }

    fun argValue(argIndex: Int, mode: Int = argMode(argIndex)): Int {
        val rawArg = program[instructionPointer + argIndex + 1]
//        println(String.format("Argmode %d for index %d", argMode(argIndex), argIndex))
        return when (mode) {
            0 -> program[rawArg]
            1 -> rawArg
            else -> throw Exception(String.format("Invalid arg mode %d for arg index %d", argMode(argIndex), argIndex))
        }
    }

    fun targetValue(argIndex: Int): Int {
        return argValue(argIndex, 1)
    }

    // Operations

    // Opcode XXX01
    fun add(): Execution {
        return nextExecution(program.set(targetValue(2), argValue(0) + argValue(1)), instructionPointer + 4)
    }

    // Opcode XXX02
    fun multiply(): Execution {
        return nextExecution(program.set(targetValue(2), argValue(0) * argValue(1)), instructionPointer + 4)
    }

    // Opcode XXX03
    fun readInput(): Execution {
        return nextExecution(program.set(targetValue(0), input.readInt()), instructionPointer + 2)
    }

    // Opcode XXX04
    fun writeOutput(): Execution {
        output.printInt(argValue(0))
        return nextExecution(program, instructionPointer + 2)
    }

    // Opcode XXX05
    fun jumpIfTrue(): Execution {
        if (argValue(0).equals(0)) {
            return nextExecution(program, instructionPointer + 3)
        }
        else {
            return nextExecution(program, argValue(1))
        }
    }

    // Opcode XXX06
    fun jumpIfFalse(): Execution {
        if (argValue(0).equals(0)) {
            return nextExecution(program, argValue(1))
        }
        else {
            return nextExecution(program, instructionPointer + 3)
        }
    }

    // Opcode XXX07
    fun isLessThan(): Execution {
        return nextExecution(program.set(targetValue(2), if (argValue(0) < argValue(1)) 1 else 0), instructionPointer + 4)
    }

    // Opcode XXX08
    fun isEqual(): Execution {
        return nextExecution(program.set(targetValue(2), if (argValue(0).equals(argValue(1))) 1 else 0), instructionPointer + 4)
    }

    fun halted(): Boolean {
        return opcode.equals(99)
    }

    fun isIncomplete(): Boolean {
        return !halted()
    }

    // Other

    override fun toString(): String {
        var newSequence = program.rawSequence.map { it.toString() }.toMutableList()
        newSequence[instructionPointer] = String.format("[%d]", instruction)
        return newSequence.toString()
    }
}

interface ProgramInput {
    fun readInt(): Int;
    fun ready(): Boolean;
}

interface ProgramOutput {
    fun printInt(value: Int);
}

data class StandardProgramInput(val displayPrompt: Boolean = true) : ProgramInput {
    override fun readInt(): Int {
        if (displayPrompt) {
            print("?: ")
            System.out.flush()
        }
        return readLine()!!.toInt()
    }

    override fun ready(): Boolean {
        return System.`in`.available() > 0
    }
}

data class CustomProgramInput(val inputList: List<Int>) : ProgramInput {
    val inputIterator = inputList.listIterator()

    override fun readInt(): Int {
        return inputIterator.next()
    }

    override fun ready(): Boolean {
        return inputIterator.hasNext()
    }
}

class StandardProgramOutput : ProgramOutput {
    override fun printInt(value: Int) {
        println(value)
    }
}

data class CapturedProgramOutput(val outputList: MutableList<Int> = mutableListOf<Int>()) : ProgramOutput {
    override fun printInt(value: Int) {
        outputList.add(value)
    }
}

data class Amplifier(val program: Program, val phaseSetting: Int) {
    var output = CapturedProgramOutput(mutableListOf<Int>())
    var executor = program.blockableExecutor(CustomProgramInput(listOf<Int>(phaseSetting)), output)
    init {
        executor.executeUntilBlock()
    }

    fun consumeInput(input: Int): Int {
        executor.executeUntilBlock(CustomProgramInput(listOf(input)))
        if (output.outputList.size.equals(1)) {
            return output.outputList.removeAt(0)
        }
        else if (output.outputList.isEmpty()) {
            throw Exception("No output")
        }
        else {
            throw Exception("Too much output")
        }
    }

    fun halted(): Boolean {
        return executor.halted()
    }
}

// problem specific functions

fun program(): Program {
    // "Real" program
    return Program.fromString(File("input.txt").readText())

    // Part 1 examples:

//    return Program.fromString("3,15,3,16,1002,16,10,16,1,16,15,15,4,15,99,0,0")
//    return Program.fromString("3,23,3,24,1002,24,10,24,1002,23,-1,23,101,5,23,23,1,24,23,23,4,23,99,0,0")
//    return Program.fromString("3,31,3,32,1002,32,10,32,1001,31,-2,31,1007,31,0,33,1002,33,7,33,1,33,31,31,1,32,31,31,4,31,99,0,0,0")

    // Part 2 examples:
//    return Program.fromString("3,26,1001,26,-4,26,3,27,1002,27,2,27,1,27,26,27,4,27,1001,28,-1,28,1005,28,6,99,0,0,5")
//    return Program.fromString("3,52,1001,52,-5,52,3,53,1,52,56,54,1007,54,5,55,1005,55,26,1001,54,-5,54,1105,1,12,1,53,54,53,1008,54,0,55,1001,55,1,55,2,53,55,53,4,53,1001,56,-1,56,1005,56,6,99,0,0,0,0,10")
}

fun permutations(possibilities: List<Int>, prefix: List<Int> = mutableListOf<Int>()): List<List<Int>> {
    if (possibilities.isEmpty()) {
        return listOf(prefix)
    }
    return possibilities.flatMap { possibleValue ->
        val newPrefix = prefix.toMutableList()
        newPrefix.add(possibleValue)
        val newPossibilities = possibilities.minus(possibleValue)
        permutations(newPossibilities, newPrefix)
    }
}

fun powerOutput(program: Program, phaseSetting: Int, powerInput: Int = 0): Int {
    val input = CustomProgramInput(listOf(phaseSetting, powerInput))
    val output = CapturedProgramOutput(mutableListOf<Int>())
    program.execute(input, output)
//    println(String.format("%d -> [%d] -> %d", powerInput, phaseSetting, output.outputList.first()))
    return output.outputList.first()
}

fun partOne(program: Program): Int {
    return permutations(listOf(0, 1, 2, 3, 4)).map { phaseSettings ->
        //    println(phaseSettings)
        val finalPower = phaseSettings.fold(0) { powerInput, phaseSetting -> powerOutput(program, phaseSetting, powerInput) }
//    println(String.format("%s: %d", phaseSettings.toString(), finalPower))
        finalPower
    }.max()!!
}

fun partTwo(program: Program): Int {
    return permutations(listOf(5, 6, 7, 8, 9)).map { phaseSettings ->
        val amplifiers = phaseSettings.map { Amplifier(program, it) }
        var output = 0
        while (amplifiers.none { it.halted() }) {
            output = amplifiers.fold(output) { currentInput, amplifier -> amplifier.consumeInput(currentInput) }
        }
        output
    }.max()!!
}

// RUN!

val program = program()

//partOne(program)
partTwo(program)