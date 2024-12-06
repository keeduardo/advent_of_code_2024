package days


import days.Mode.*
import java.io.File
import kotlin.math.abs

val lines = File("src/main/resources/inputs/day02.txt").readLines()
    .map { line -> line.split(" ").map { it.toInt() } }


fun main() {
    val countOfValidReports = lines.count { line ->
        val isReportValid = isReportValid(line)
        val isSomeReportVariationValid = line.withIndex().any { it ->
            val removeIndex = it.index
            val variation = line.filterIndexed { i, _ -> i != removeIndex }
            isReportValid(variation)
        }
        isReportValid || isSomeReportVariationValid
    }
    println(countOfValidReports) // 577
}

private fun isReportValid(line: List<Int>): Boolean {
    var mode: Mode = UNSET
    val isLineValid = line.windowed(2).all { el -> isLevelValid(el, mode) { mode = this } }
    return isLineValid
}

private fun isLevelValid(el: List<Int>, mode: Mode, block: Mode.() -> Unit): Boolean {
    val first = el[0]
    val second = el[1]
    val orderValid = when (mode) {
        INCREASING -> first < second
        DECREASING -> first > second
        UNSET -> true.also {
            (if (first > second) DECREASING else INCREASING).block()
        }
    }
    val deltaValid = abs(first - second) in 1..3
    return orderValid && deltaValid
}

enum class Mode {
    INCREASING, DECREASING, UNSET
}