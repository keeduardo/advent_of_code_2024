package days

import java.io.File


val reg = "mul\\(([0-9]{1,3}),([0-9]{1,3})\\)".toRegex()
fun main() {
    val input = File("src/main/resources/inputs/day03-test.txt").readText()
    reg.findAll(input).filter { match ->
        val before = input.substring(0, match.range.first)
        before.lastIndexOf("do()") >= before.lastIndexOf("don't()")
    }
        .map { it.multiply() }
        .sum()
        .println()
}

fun MatchResult.multiply(): Int {
    return this.groups[1]!!.value.toInt() * this.groups[2]!!.value.toInt()
}

fun <T> T.println() = println(this.toString())