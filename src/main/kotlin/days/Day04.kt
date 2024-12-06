package days

import java.io.File
import kotlin.time.measureTime


val matrix = File("src/main/resources/inputs/day04.txt").toMatrix()
val maxX = matrix[0].size
val maxY = matrix.size
const val MATCH = "XMAS"
const val REVERSE = "SAMX"
const val MATCH_2 = "MAS"
const val REVERSE_2 = "SAM"

fun main() {
   val x = measureTime {
        repeat(100) {
            val sum1 = listOf(
                matrix.getRows(),
                matrix.getColumns(),
                matrix.getMainDiagonals(),
                matrix.getAntiDiagonals()
            ).flatten().sumOfSequence("XMAS")
            println(sum1)

            val mm = mutableListOf<Array<Array<Char>>>()
            (0..<maxY - 2).map { y ->
                (0..<maxX - 2).map { x ->

                    val lines = mutableListOf<String>()
                    (0..<3).map { wY ->
                        var line = ""
                        (0..<3).map { wX ->
                            line += matrix[y + wY][x + wX]
                        }
                        lines.add(line)
                    }
                    mm.add(lines.toMatrix())
                }
            }

            val gg = mm.count {
                val main = it.getMainDiagonals().find { d -> d.length == 3 }!!
                val anti = it.getAntiDiagonals().find { d -> d.length == 3 }!!
                (main.contains("MAS") || main.contains("SAM")) && (anti.contains("MAS") || anti.contains("SAM"))
            }
            println(gg)
        }
    }
    println(x / 100)
}

fun Array<Array<Char>>.getRows() = this.map { it.joinToString("") }.toList()
fun Array<Array<Char>>.getColumns() = (0..<maxX).map { x ->
    this.map { it[x] }.joinToString("")
}

fun Array<Array<Char>>.getMainDiagonals(): List<String> {
    val rows = this.size
    val cols = this[0].size
    val mainDiagonals = mutableListOf<String>()
    for (start in 0..<rows + cols - 1) {
        val diagonal = mutableListOf<Char>()
        for (i in 0..<rows) {
            val j = start - i
            if (j in 0..<cols) {
                diagonal.add(this[i][j])
            }
        }
        if (diagonal.isNotEmpty()) {
            mainDiagonals.add(diagonal.joinToString(""))
        }
    }
    return mainDiagonals
}

fun Array<Array<Char>>.asString(): String {
    return this.getRows().joinToString("\n")
}

fun Array<Array<Char>>.getAntiDiagonals(): List<String> {
    val rows = this.size
    val cols = this[0].size
    val antiDiagonal = mutableListOf<String>()
    for (start in 0..<rows + cols - 1) {
        val diagonal = mutableListOf<Char>()
        for (i in 0..<rows) {
            val j = start - (rows - 1 - i)
            if (j in 0..<cols) {
                diagonal.add(this[i][j])
            }
        }
        if (diagonal.isNotEmpty()) {
            antiDiagonal.add(diagonal.joinToString(""))
        }
    }
    return antiDiagonal
}

fun File.toMatrix(): Array<Array<Char>> {
    return this.readLines().filter { it.isNotBlank() }.toMatrix()
}

fun List<String>.toMatrix(): Array<Array<Char>> {
    return this.map { line ->
        line.map { it }.toTypedArray()
    }.toTypedArray()
}

fun List<String>.sumOfSequence(seq: String): Int {
    return this.sumOf { it.split(seq).size - 1 } + this.sumOf { it.split(seq.reversed()).size - 1 }
}