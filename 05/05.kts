import java.io.File
import kotlin.math.pow

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

    fun execute(): Program {
        var execution = Execution(this)
        println(execution)

        while(execution.isIncomplete()) {
            execution = execution.step()
            println(execution)
        }

        return execution.program
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

data class Execution(val program: Program, val instructionPointer: Int = 0) {
    val instruction = program[instructionPointer]
    val opcode = instruction % 100
    val args = (instruction - opcode) / 100

    fun step(): Execution {
        return when(opcode) {
            1 -> add()
            2 -> multiply()
            3 -> input()
            4 -> output()
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
        return Execution(program.set(targetValue(2), argValue(0) + argValue(1)), instructionPointer + 4)
    }

    // Opcode XXX02
    fun multiply(): Execution {
        return Execution(program.set(targetValue(2), argValue(0) * argValue(1)), instructionPointer + 4)
    }

    // Opcode XXX03
    fun input(): Execution {
        print("?: ")
        System.out.flush()
        return Execution(program.set(targetValue(0), readLine()!!.toInt()), instructionPointer + 2)
    }

    // Opcode XXX04
    fun output(): Execution {
        println(argValue(0))
        return Execution(program, instructionPointer + 2)
    }

    // Opcode XXX05
    fun jumpIfTrue(): Execution {
        if (argValue(0).equals(0)) {
            return Execution(program, instructionPointer + 3)
        }
        else {
            return Execution(program, argValue(1))
        }
    }

    // Opcode XXX06
    fun jumpIfFalse(): Execution {
        if (argValue(0).equals(0)) {
            return Execution(program, argValue(1))
        }
        else {
            return Execution(program, instructionPointer + 3)
        }
    }

    // Opcode XXX07
    fun isLessThan(): Execution {
        return Execution(program.set(targetValue(2), if (argValue(0) < argValue(1)) 1 else 0), instructionPointer + 4)
    }

    // Opcode XXX08
    fun isEqual(): Execution {
        return Execution(program.set(targetValue(2), if (argValue(0).equals(argValue(1))) 1 else 0), instructionPointer + 4)
    }

    // Opcode XXX99
    fun isIncomplete(): Boolean {
        return !opcode.equals(99)
    }

    // Other

    override fun toString(): String {
        var newSequence = program.rawSequence.map { it.toString() }.toMutableList()
        newSequence[instructionPointer] = String.format("[%d]", instruction)
        return newSequence.toString()
    }
}

fun originalProgram(): Program {
    return Program.fromString(File("input.txt").readText())
}
//Program.fromString("3,21,1008,21,8,20,1005,20,22,107,8,21,20,1006,20,31,1106,0,36,98,0,0,1002,21,125,20,4,20,1105,1,46,104,999,1105,1,46,1101,1000,1,20,4,20,1105,1,46,98,99").execute()
originalProgram().execute()