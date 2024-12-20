package days

import days.Action.TURN
import days.Action.WALK
import days.ActionResult.LOOP
import days.Direction.DOWN
import days.Direction.LEFT
import days.Direction.RIGHT
import days.Direction.UP
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import kotlin.time.measureTime

val game = File("src/main/resources/inputs/day06.txt").createBoard()

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun main() = coroutineScope {
    // Task 1
    game.toGameState().apply {
        playThrough()
        guard.getUniquePlaces().println()
    }

    // Task 2
    game.generateGameStateVariations()
        .map { async { it.playThrough() } }
        .awaitAll()
        .count { result -> result == LOOP }
        .println()
}

private fun Pair<Board, Guard>.generateGameStateVariations(): List<GameState> {
    val startBoard = this.first.deepCopy()
    val startGuard = this.second.copy()
    val jobQueue = mutableListOf<GameState>()
    repeat(startBoard.height) { y ->
        repeat(startBoard.width) { x ->
            if (startBoard.getObstacle(x, y) == null)
                jobQueue.add((startBoard to startGuard).toGameState(Obstacle(x, y)))
        }
    }
    return jobQueue
}

private fun GameState.playThrough(): ActionResult {
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

    fun getObstacle(x: Int, y: Int): Obstacle? {
        additionalObstacle?.let {
            if (it.x == x && it.y == y)
                return it
        }
        return obstacle.find { obstacle -> obstacle.x == x && obstacle.y == y }
    }

    fun isOutOfBound(x: Int, y: Int) = (x < 0 || y < 0 || x >= width || y >= height)

    fun deepCopy(adObstacle: Obstacle? = null): Board {
        val c = this.copy()
        c.obstacle = obstacle.map { it.copy() }
        c.additionalObstacle = adObstacle?.copy()
        return c
    }
}

data class GameState(
    val board: Board,
    val guard: Guard,
)

fun Pair<Board, Guard>.toGameState(additionalObstacle: Obstacle? = null): GameState {
    return GameState(board = first.deepCopy(additionalObstacle), guard = second.copy())
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
        do {
            var (action, nextDirection) = when (direction) {
                UP -> board.getObstacle(x, y - 1).let { if (it == null) WALK to UP else TURN to RIGHT }
                DOWN -> board.getObstacle(x, y + 1).let { if (it == null) WALK to DOWN else TURN to LEFT }
                LEFT -> board.getObstacle(x - 1, y).let { if (it == null) WALK to LEFT else TURN to UP }
                RIGHT -> board.getObstacle(x + 1, y).let { if (it == null) WALK to RIGHT else TURN to DOWN }
            }
            direction = nextDirection
        } while (action == TURN)

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
            .also { result -> if (result == ActionResult.OUT_OF_BOUND) history.size }
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
    OUT_OF_BOUND, LOOP, CONTINUE
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
