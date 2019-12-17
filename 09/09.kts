import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.math.BigInteger
import kotlin.math.pow
import kotlin.system.exitProcess

class Program(val rawSequence: List<BigInteger>) {
    companion object {
        fun fromString(rawString: String): Program {
            return Program(rawString.split(",").map { it.toBigInteger() })
        }
    }

    fun execute(input: ProgramInput = StandardProgramInput(), output: ProgramOutput = StandardProgramOutput()): Program {
        var execution = Execution(this, BigInteger.ZERO, BigInteger.ZERO, input, output)

        while(execution.isIncomplete()) { execution = execution.step() }

        return execution.program
    }

    fun blockableExecutor(input: ProgramInput = StandardProgramInput(), output: ProgramOutput = StandardProgramOutput()): BlockableExecutor {
        return BlockableExecutor(Execution(this, BigInteger.ZERO, BigInteger.ZERO, input, output))
    }

    fun instruction(): BigInteger {
        return get(BigInteger.ZERO)
    }

    operator fun get(index: BigInteger): BigInteger {
        ensureReasonableIndex(index)
        if (index >= rawSequence.size.toBigInteger()) {
            return BigInteger.ZERO
        }
        return rawSequence[index.toInt()]
    }

    operator fun set(index: BigInteger, value: BigInteger): Program {
        ensureReasonableIndex(index)
        var newSequence = ArrayList(rawSequence)
        if (index >= newSequence.size.toBigInteger()) {
            for (newIndex in newSequence.size until index.toInt() + 1) {
                newSequence.add(newIndex, BigInteger.ZERO)
            }
        }
        newSequence[index.toInt()] = value
        return Program(newSequence.toList())
    }

    fun ensureReasonableIndex(index: BigInteger) {
        if (index.compareTo(Int.MAX_VALUE.toBigInteger()) > 0) {
            throw Exception("Index out of bounds")
        }
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
        val instructionPointer: BigInteger = BigInteger.ZERO,
        val relativeBase: BigInteger = BigInteger.ZERO,
        val input: ProgramInput = StandardProgramInput(),
        val output: ProgramOutput = StandardProgramOutput()
) {
    val instruction = program[instructionPointer]
    val bigOpcode = instruction % 100.toBigInteger()
    val opcode = bigOpcode.toInt()
    val bigArgs = (instruction - bigOpcode) / 100.toBigInteger()
    val args = bigArgs.toInt()

    fun nextExecution(nextProgram: Program, nextInsructionPointer: BigInteger): Execution {
        return Execution(nextProgram, nextInsructionPointer, relativeBase, input, output)
    }

    fun withInput(newInput: ProgramInput) : Execution {
        return Execution(program, instructionPointer, relativeBase, newInput, output)
    }

    fun nonBlocking(): Boolean {
        return when(opcode) {
            3 -> input.ready()
            else -> true
        }
    }

    fun step(): Execution {
        // println(this) // DEBUG
        return when(opcode) {
            1 -> add()
            2 -> multiply()
            3 -> readInput()
            4 -> writeOutput()
            5 -> jumpIfTrue()
            6 -> jumpIfFalse()
            7 -> isLessThan()
            8 -> isEqual()
            9 -> adjustRelativeBase()
            99 -> this
            else -> throw Exception(String.format("Invalid opcode: %d", opcode))
        }
    }

    fun argMode(argIndex: Int): Int {
        return ((args % (10.0).pow(argIndex + 1).toInt()) - (args % (10.0).pow(argIndex).toInt())) / (10.0).pow(argIndex).toInt()
    }

    fun argValue(argIndex: Int, mode: Int = argMode(argIndex)): BigInteger {
        val rawArg = program[instructionPointer + argIndex.toBigInteger() + BigInteger.ONE]
//        println(String.format("Argmode %d for index %d", argMode(argIndex), argIndex))
        return when (mode) {
            0 -> program[rawArg]
            1 -> rawArg
            2 -> program[relativeBase + rawArg]
            else -> throw Exception(String.format("Invalid arg mode %d for arg index %d", argMode(argIndex), argIndex))
        }
    }

    fun argString(argIndex: Int, mode: Int = argMode(argIndex)): String {
        val rawArg = program[instructionPointer + argIndex.toBigInteger() + BigInteger.ONE]
        val value = argValue(argIndex, mode)
//        println(String.format("Argmode %d for index %d", argMode(argIndex), argIndex))
        return when (mode) {
            0 -> String.format("[%d](%d)", rawArg, value)
            1 -> String.format("!(%d)", value)
            2 -> String.format("[%d + %d](%d)", relativeBase, rawArg, value)
            else -> throw Exception(String.format("Invalid arg mode %d for arg index %d", argMode(argIndex), argIndex))
        }
    }

    fun targetValue(argIndex: Int, mode: Int = argMode(argIndex)): BigInteger {
        val rawArg = program[instructionPointer + argIndex.toBigInteger() + BigInteger.ONE]

        return when(mode) {
            0, 1 -> rawArg
            else -> relativeBase + rawArg
        }
    }

    // Operations

    // Opcode XXX01
    fun add(): Execution {
        return nextExecution(
                program.set(targetValue(2), argValue(0) + argValue(1)),
                instructionPointer + 4.toBigInteger()
        )
    }

    // Opcode XXX02
    fun multiply(): Execution {
        return nextExecution(
                program.set(targetValue(2), argValue(0) * argValue(1)),
                instructionPointer + 4.toBigInteger()
        )
    }

    // Opcode XXX03
    fun readInput(): Execution {
        return nextExecution(
                program.set(targetValue(0), input.readInt()),
                instructionPointer + 2.toBigInteger()
        )
    }

    // Opcode XXX04
    fun writeOutput(): Execution {
        output.printInt(argValue(0))
        return nextExecution(program, instructionPointer + 2.toBigInteger())
    }

    // Opcode XXX05
    fun jumpIfTrue(): Execution {
        if (argValue(0).equals(BigInteger.ZERO)) {
            return nextExecution(program, instructionPointer + 3.toBigInteger())
        }
        else {
            return nextExecution(program, argValue(1))
        }
    }

    // Opcode XXX06
    fun jumpIfFalse(): Execution {
        if (argValue(0).equals(BigInteger.ZERO)) {
            return nextExecution(program, argValue(1))
        }
        else {
            return nextExecution(program, instructionPointer + 3.toBigInteger())
        }
    }

    // Opcode XXX07
    fun isLessThan(): Execution {
        return nextExecution(
                program.set(
                        targetValue(2),
                        if (argValue(0) < argValue(1)) BigInteger.ONE else BigInteger.ZERO
                ),
                instructionPointer + 4.toBigInteger()
        )
    }

    // Opcode XXX08
    fun isEqual(): Execution {
        return nextExecution(
                program.set(
                        targetValue(2),
                        if (argValue(0).equals(argValue(1))) BigInteger.ONE else BigInteger.ZERO
                ),
                instructionPointer + 4.toBigInteger()
        )
    }
    // Opcode XXX09
    fun adjustRelativeBase(): Execution {
//        println(String.format("Adjusting relative base to %d", relativeBase + argValue(0)))
        return Execution(
                program,
                instructionPointer + 2.toBigInteger(),
                relativeBase + argValue(0),
                input,
                output
        )
    }

    fun halted(): Boolean {
        return opcode.equals(99)
    }

    fun isIncomplete(): Boolean {
        return !halted()
    }

    // Other

    override fun toString(): String {
        val instructionString = String.format("[%2d](%04d)", instructionPointer, instruction)
        val fullInstructionString =
                String.format(
                        "%s,%4d,%4d,%4d",
                        instructionString,
                        program[instructionPointer + 1.toBigInteger()],
                        program[instructionPointer + 2.toBigInteger()],
                        program[instructionPointer + 3.toBigInteger()]
                )
        return when(opcode) {
            1 -> String.format("%10s # %s + %s -> %s", fullInstructionString, argString(0), argString(1), argString(2))
            2 -> String.format("%10s # %s * %s -> %s", fullInstructionString, argString(0), argString(1), argString(2))
            3 -> String.format("%10s # input -> %s", fullInstructionString, argString(0))
            4 -> String.format("%10s # %s -> output", fullInstructionString, argString(0))
            5 -> String.format("%10s # if %s != 0 jump to %s", fullInstructionString, argString(0), argString(1))
            6 -> String.format("%10s # if %s == 0 jump to %s", fullInstructionString, argString(0), argString(1))
            7 -> String.format("%10s # %s < %s -> %s", fullInstructionString, argString(0), argString(1), argString(2))
            8 -> String.format("%10s # %s == %s -> %s", fullInstructionString, argString(0), argString(1), argString(2))
            9 -> String.format("%10s # !(%d) + %s -> relative", fullInstructionString, relativeBase, argString(0))
            99 -> String.format("%10s # halt", fullInstructionString)
            else -> throw Exception(String.format("Unknown opcode %s", fullInstructionString))
        }
//        var newSequence = program.rawSequence.map { it.toString() }.toMutableList()
//        program.ensureReasonableIndex(instructionPointer)
//        newSequence[instructionPointer.toInt()] = String.format("[%d]", instruction)
//        return newSequence.toString()
    }
}

interface ProgramInput {
    fun readInt(): BigInteger;
    fun ready(): Boolean;
}

interface ProgramOutput {
    fun printInt(value: BigInteger);
}

data class StandardProgramInput(val displayPrompt: Boolean = true) : ProgramInput {
    override fun readInt(): BigInteger {
        if (displayPrompt) {
            print("?: ")
            System.out.flush()
        }
        return readLine()!!.toBigInteger()
    }

    override fun ready(): Boolean {
        return System.`in`.available() > 0
    }
}

data class CustomProgramInput(val inputList: List<BigInteger>) : ProgramInput {
    val inputIterator = inputList.listIterator()

    override fun readInt(): BigInteger {
        val input = inputIterator.next()
//        println(String.format("Input: => %d", input))
        return input
    }

    override fun ready(): Boolean {
        return inputIterator.hasNext()
    }
}

class StandardProgramOutput : ProgramOutput {
    override fun printInt(value: BigInteger) {
        println(value)
    }
}

data class CapturedProgramOutput(val outputList: MutableList<BigInteger> = mutableListOf<BigInteger>()) : ProgramOutput {
    override fun printInt(value: BigInteger) {
        outputList.add(value)
    }
}

// problem specific functions

fun program(): Program {
    // "Real" program
    return Program.fromString(File("input.txt").readText())

    // Part 1 examples:

//    return Program.fromString("109,1,204,-1,1001,100,1,100,1008,100,16,101,1006,101,0,99")
//    return Program.fromString("1102,34915192,34915192,7,4,7,99,0")
//    return Program.fromString("104,1125899906842624,99")

    // Part 2 examples:
//    return Program.fromString("")
}

fun partOne(program: Program) {
    program.execute(CustomProgramInput(listOf(BigInteger.ONE)))
}

fun partTwo(program: Program) {
    program.execute(CustomProgramInput(listOf(2.toBigInteger())))
}

// RUN!

val program = program()
//partOne(program)
partTwo(program)