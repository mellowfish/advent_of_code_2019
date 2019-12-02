import java.io.File

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
//        println(execution)

        while(execution.isIncomplete()) {
            execution = execution.step()
//            println(execution)
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
    fun step(): Execution {
        return when(instruction()) {
            1 -> add()
            2 -> multiply()
            99 -> this
            else -> throw Exception(String.format("Invalid Instruction: %d", instruction()))
        }
    }

    fun arg0(): Int {
        return program[program[instructionPointer + 1]]
    }

    fun arg1(): Int {
        return program[program[instructionPointer + 2]]
    }

    fun targetIndex(): Int {
        return program[instructionPointer + 3]
    }

    fun add(): Execution {
        return Execution(program.set(targetIndex(), arg0() + arg1()), instructionPointer + 4)
    }

    fun multiply(): Execution {
        return Execution(program.set(targetIndex(), arg0() * arg1()), instructionPointer + 4)
    }

    fun instruction(): Int {
        return program[instructionPointer]
    }

    fun isIncomplete(): Boolean {
        return !instruction().equals(99)
    }

    override fun toString(): String {
        var newSequence = program.rawSequence.map { it.toString() }.toMutableList()
        newSequence[instructionPointer] = String.format("[%d]", instruction())
        return newSequence.toString()
    }
}

fun originalProgram(): Program {
    return Program.fromString(File("input.txt").readText())
}

fun findInput(output: Int): Int {
    var program: Program

    for (noun in 0..99) {
        for (verb in 0..99) {
            program = originalProgram().at(noun, verb).execute()
            if (program.instruction().equals(output)) {
                return program.input()
            }
        }
    }

    return -1
}

println(String.format("Ouput of 1202: %d", originalProgram().atTimeOfAlarm().execute().instruction()))
println(String.format("Input for 19690720: %d", findInput(19690720)))