package days

import java.io.File

val rules = File("src/main/resources/inputs/day05.txt").readRules()
val printOrder = File("src/main/resources/inputs/day05.txt").readPrintOrder()
fun main() {
    printOrder.filter { order -> !order.hasViolations() }
        .sumOf { it.center().value }
        .println()

    printOrder.filter { order -> order.hasViolations() }
        .map { it.sorted() }
        .sumOf { it.center().value }

}

private fun List<OrderNumber>.center(): OrderNumber {
    return this[(this.size / 2)]
}

private fun List<OrderNumber>.hasViolations(): Boolean {
    val hasRuleViolation = this.withIndex().any { iv ->
        val printAfter = this.subList(iv.index, this.size)
        rules.getOrDefault(iv.value, emptyList()).intersect(printAfter).isNotEmpty()
    }
    return hasRuleViolation
}

fun File.readRules(): Map<OrderNumber, Set<OrderNumber>> {
    val rules = mutableMapOf<OrderNumber, Set<OrderNumber>>()
    this.readLines()
        .filter { it.contains("|") }
        .map {
            val el = it.split("|")
            val first = el[0].toOrderNumber()
            val second = el[1].toOrderNumber()
            val printBefore = rules.getOrDefault(second, mutableListOf<OrderNumber>())
            rules[second] = (printBefore + first).toSet()
        }
    return rules
}

fun File.readPrintOrder(): List<List<OrderNumber>> {
    return this.readLines()
        .filter { it.isNotBlank() && !it.contains("|") }
        .map { it.split(",").map { el -> el.toOrderNumber() } }
}

fun String.toOrderNumber(): OrderNumber {
    return OrderNumber(this.toInt())
}

@JvmInline
value class OrderNumber(val value: Int) : Comparable<OrderNumber> {

    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(other: OrderNumber): Int {
        return if (rules.getOrDefault(this, emptyList()).contains(other)) -1 else 0
    }
}