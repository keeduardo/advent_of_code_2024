package days

import days.Action.TURN
import days.Action.WALK
import days.ActionResult.LOOP
import days.Direction.DOWN
import days.Direction.LEFT
import days.Direction.RIGHT
import days.Direction.UP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.random.Random
import kotlin.time.measureTime

val game = File("src/main/resources/inputs/day06.txt").createBoard()

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun main() = coroutineScope {
    val (startBoard, startGuard) = game

    measureTime {
        val jobList = createJobList(startBoard, startGuard)
        jobList.withIndex()
            .map { job -> async { runRound(job.value.board, job.value.guard) } }
            .awaitAll()
            .count { result -> result == LOOP }
            .println()
    }.println()

}

private fun createJobList(startBoard: Board, startGuard: Guard): List<BoardJob> {
    val l = mutableListOf<BoardJob>()
    repeat(startBoard.height) { y ->
        repeat(startBoard.width) { x ->
            val (board, guard) = startBoard.copy() to startGuard.copy()
            board.obstacle = board.obstacle.map { it.copy() }
            board.addObstacle(x, y)
            l.add(BoardJob(board = board, guard = guard))
        }
    }
    return l
}

private fun runRound(board: Board, guard: Guard): ActionResult {
    var next = guard.next(board)
    while (next == ActionResult.CONTINUE) {
        next = guard.next(board)
    }
    return next
}

data class Board(
    val width: Int,
    val height: Int,
    var obstacle: List<Obstacle>,
) {
    var additionalObstacle: Obstacle? = null

    fun get(x: Int, y: Int): Obstacle? {
        additionalObstacle?.let {
            if (it.x == x && it.y == y)
                return it
        }
        return obstacle.find { obstacle -> obstacle.x == x && obstacle.y == y }
    }

    fun addObstacle(x: Int, y: Int) {
        additionalObstacle = Obstacle(x = x, y = y)
    }

    fun isOutOfBound(x: Int, y: Int) = (x < 0 || y < 0 || x >= width || y >= height)
}

data class BoardJob(
    val board: Board,
    val guard: Guard,
) {

}

data class Obstacle(
    val x: Int,
    val y: Int,
)

data class Guard(
    var x: Int = 0,
    var y: Int = 0,
    var direction: Direction = DOWN,
) {
    val history = mutableListOf<GuardHistory>()

    init {
        pushHistory()
    }

    fun next(board: Board): ActionResult {
        var turns = 0
        do {
            var (action, nextDirection) = when (direction) {
                UP -> board.get(x, y - 1).let { if (it == null) WALK to UP else TURN to RIGHT }
                DOWN -> board.get(x, y + 1).let { if (it == null) WALK to DOWN else TURN to LEFT }
                LEFT -> board.get(x - 1, y).let { if (it == null) WALK to LEFT else TURN to UP }
                RIGHT -> board.get(x + 1, y).let { if (it == null) WALK to RIGHT else TURN to DOWN }
            }
            direction = nextDirection
            if (action == TURN)
                turns++
        } while (action == TURN && turns <= 4)
        if (turns == 4)
            return ActionResult.LOOP_2

        when (direction) {
            UP -> y -= 1
            DOWN -> y += 1
            LEFT -> x -= 1
            RIGHT -> x += 1
        }

        if (isHistoryRepeating()) {
            return LOOP
        }

        return board.isOutOfBound(x, y)
            .also { oob -> if (!oob) pushHistory() }
            .let { oob -> if (oob) ActionResult.OUT_OF_BOUND else ActionResult.CONTINUE }
    }

    fun getUniquePlaces(): Int {
        return history.map { "${it.x},${it.y}" }.toSet().count()
    }

    private fun pushHistory() {
        history.add(this.toHistory())
    }

    private fun isHistoryRepeating(): Boolean {
        val repeating = history.any { history ->
            history.x == this.x && history.y == this.y && history.direction == this.direction
        }
        return repeating
    }
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

enum class Action {
    TURN, WALK
}

enum class ActionResult {
    OUT_OF_BOUND, LOOP, CONTINUE, LOOP_2
}

data class GuardHistory(
    val x: Int = 0,
    val y: Int = 0,
    val direction: Direction = DOWN,
)

fun Guard.toHistory(): GuardHistory {
    return GuardHistory(x = x, y = y, direction = direction)
}

fun File.createBoard(): Pair<Board, Guard> {
    var width = 0
    val lines = this.readLines()

    val obstacles = mutableListOf<Obstacle>()
    var guard: Guard? = null
    lines.withIndex().forEach { li ->
        width = li.value.length
        val y = li.index
        li.value.withIndex().map { vi ->
            when (vi.value) {
                '#' -> obstacles.add(Obstacle(x = vi.index, y = y))
                else -> vi.value.toDirection()?.also { guard = Guard(x = vi.index, y = y, direction = it) }
            }
        }
    }
    guard = guard ?: throw IllegalStateException("no guard")
    return Board(
        width = width,
        height = lines.size,
        obstacle = obstacles,
    ) to guard
}

fun Char.toDirection(): Direction? = when (this) {
    '>' -> RIGHT
    'v' -> DOWN
    '<' -> LEFT
    '^' -> UP
    else -> null
}
