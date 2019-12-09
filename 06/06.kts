import java.io.File

data class MassiveBody(val name: String) {
    val children = mutableListOf<MassiveBody>()
    var parent: MassiveBody? = null

    companion object {
        var massiveBodies = mutableMapOf<String, MassiveBody>()

        fun get(name: String, default: MassiveBody = MassiveBody(name)): MassiveBody {
            return massiveBodies.getOrPut(name) { default }
        }

        fun com(): MassiveBody { return get("COM") }
        fun you(): MassiveBody { return get("YOU") }
        fun san(): MassiveBody { return get("SAN") }
    }

    override fun equals(other: Any?): Boolean {
        if (other is MassiveBody) {
            return name.equals(other.name)
        }

        return false
    }

    fun addChild(child: MassiveBody): MassiveBody {
        children.add(child)
        child.parent = this

        return this
    }

    fun countOrbits(depth: Int = 0): Int {
        return depth + children.sumBy { it.countOrbits(depth + 1) }
    }

    fun isTerminal(): Boolean {
        return children.isEmpty()
    }

    fun hasChild(target: MassiveBody): Boolean {
        return children.contains(target)
    }

    fun pathToSanta(pathFromYou: MutableList<MassiveBody> = mutableListOf<MassiveBody>()): List<MassiveBody>? {
        val directPath = directPathTo(san(), pathFromYou.toMutableList())
        if (directPath != null) {
            return directPath
        }
        if (parent == null) {
            return null // SAN probably doesn't exist in this world...
        }
        pathFromYou.add(this)
        return parent?.pathToSanta(pathFromYou)
    }

    fun directPathTo(target: MassiveBody, pathFromYou: MutableList<MassiveBody> = mutableListOf<MassiveBody>()): List<MassiveBody>? {
        pathFromYou.add(this)
        if (this == target) {
            return pathFromYou
        }
        if (isTerminal()) {
            return null
        }
        return children.map { it.directPathTo(target, pathFromYou.toMutableList()) }.filterNot { it == null || it.isEmpty() }.firstOrNull()
    }

    fun printOut(indent: String = "") {
        println(String.format("%s+- %s", indent, name))
        val newIndent = String.format("%s|  ", indent)
        children.forEach { it.printOut(newIndent) }
    }
}

data class Orbit(val primaryBody: MassiveBody, val secondaryBody: MassiveBody) {}

fun loadSystem() {
    inputLines().forEach { line ->
        val bodies = line.split(")").map { MassiveBody.get(it) }
        bodies[0].addChild(bodies[1])
    }
}

fun inputLines(): Sequence<String> {
    return File("input.txt").bufferedReader().lineSequence()
//    return "COM)B\nB)C\nC)D\nD)E\nE)F\nB)G\nG)H\nD)I\nE)J\nJ)K\nK)L\nK)YOU\nI)SAN".lines().asSequence()
}

loadSystem()

//MassiveBody.com().printOut()
//MassiveBody.com().countOrbits()
val path = MassiveBody.you().pathToSanta()!!
println(path.map { it.name })
println(path.size - 3)