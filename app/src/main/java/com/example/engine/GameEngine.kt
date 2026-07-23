package com.example.engine

import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.Move
import com.example.model.Position
import com.example.model.Wall
import java.util.ArrayDeque

object GameEngine {

    fun createInitialState(mode: GameMode = GameMode.DUEL, customWalls: Int? = null): GameState {
        val wallsCount = customWalls ?: mode.defaultWalls
        return if (mode == GameMode.RACE) {
            GameState(
                mode = GameMode.RACE,
                cols = mode.cols,
                rows = mode.rows,
                pawns = listOf(
                    Position(mode.rows - 1, 2),
                    Position(mode.rows - 1, mode.cols - 3)
                ),
                walls = emptyList(),
                leftWalls = intArrayOf(wallsCount, wallsCount),
                turn = 0,
                winner = null
            )
        } else {
            GameState(
                mode = GameMode.DUEL,
                cols = mode.cols,
                rows = mode.rows,
                pawns = listOf(
                    Position(8, 4),
                    Position(0, 4)
                ),
                walls = emptyList(),
                leftWalls = intArrayOf(wallsCount, wallsCount),
                turn = 0,
                winner = null
            )
        }
    }

    fun goalRow(p: Int, state: GameState): Int {
        if (state.mode == GameMode.RACE) return 0
        return if (p == 0) 0 else state.rows - 1
    }

    fun isBlocked(walls: List<Wall>, r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        if (r1 == r2) {
            // Horizontal step: crossing vertical boundary between c and c+1
            val minC = minOf(c1, c2)
            for (w in walls) {
                if (!w.isHorizontal && w.c == minC && (w.r == r1 || w.r == r1 - 1)) {
                    return true
                }
            }
        } else {
            // Vertical step: crossing horizontal boundary between r and r+1
            val minR = minOf(r1, r2)
            for (w in walls) {
                if (w.isHorizontal && w.r == minR && (w.c == c1 || w.c == c1 - 1)) {
                    return true
                }
            }
        }
        return false
    }

    private val DIRS = arrayOf(
        intArrayOf(-1, 0),
        intArrayOf(1, 0),
        intArrayOf(0, -1),
        intArrayOf(0, 1)
    )

    fun pawnMoves(state: GameState, p: Int): List<Position> {
        val cols = state.cols
        val rows = state.rows
        fun inBounds(r: Int, c: Int) = r in 0 until rows && c in 0 until cols

        val me = state.pawns[p]
        val opp = state.pawns[1 - p]
        val moves = mutableListOf<Position>()

        for (dir in DIRS) {
            val dr = dir[0]
            val dc = dir[1]
            val r1 = me.r + dr
            val c1 = me.c + dc

            if (!inBounds(r1, c1) || isBlocked(state.walls, me.r, me.c, r1, c1)) continue

            if (r1 != opp.r || c1 != opp.c) {
                moves.add(Position(r1, c1))
                continue
            }

            // Opponent adjacent: try straight jump
            val r2 = r1 + dr
            val c2 = c1 + dc
            if (inBounds(r2, c2) && !isBlocked(state.walls, r1, c1, r2, c2)) {
                moves.add(Position(r2, c2))
            } else {
                // Straight jump blocked: try diagonal sidesteps
                val perps = if (dr == 0) {
                    arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0))
                } else {
                    arrayOf(intArrayOf(0, -1), intArrayOf(0, 1))
                }
                for (perp in perps) {
                    val r3 = r1 + perp[0]
                    val c3 = c1 + perp[1]
                    if (!inBounds(r3, c3)) continue
                    if (isBlocked(state.walls, r1, c1, r3, c3)) continue
                    if (r3 == me.r && c3 == me.c) continue
                    moves.add(Position(r3, c3))
                }
            }
        }
        return moves
    }

    private fun wallsConflict(w1: Wall, w2: Wall): Boolean {
        if (w1.isHorizontal == w2.isHorizontal) {
            return if (w1.isHorizontal) {
                w1.r == w2.r && kotlin.math.abs(w1.c - w2.c) <= 1
            } else {
                w1.c == w2.c && kotlin.math.abs(w1.r - w2.r) <= 1
            }
        }
        // Horizontal vs Vertical crossing at the exact same center junction
        return w1.r == w2.r && w1.c == w2.c
    }

    fun hasPath(walls: List<Wall>, pawn: Position, goal: Int, cols: Int, rows: Int): Boolean {
        val visited = BooleanArray(rows * cols)
        val queue = ArrayDeque<Int>()
        val startIdx = pawn.r * cols + pawn.c
        queue.add(startIdx)
        visited[startIdx] = true

        while (queue.isNotEmpty()) {
            val cur = queue.poll() ?: break
            val r = cur / cols
            val c = cur % cols

            if (r == goal) return true

            for (dir in DIRS) {
                val nr = r + dir[0]
                val nc = c + dir[1]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val nextIdx = nr * cols + nc
                if (visited[nextIdx]) continue
                if (isBlocked(walls, r, c, nr, nc)) continue

                visited[nextIdx] = true
                queue.add(nextIdx)
            }
        }
        return false
    }

    fun distToGoal(walls: List<Wall>, goal: Int, cols: Int, rows: Int): IntArray {
        val dist = IntArray(rows * cols) { -1 }
        val queue = ArrayDeque<Int>()

        for (c in 0 until cols) {
            val idx = goal * cols + c
            dist[idx] = 0
            queue.add(idx)
        }

        while (queue.isNotEmpty()) {
            val cur = queue.poll() ?: break
            val r = cur / cols
            val c = cur % cols

            for (dir in DIRS) {
                val nr = r + dir[0]
                val nc = c + dir[1]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val nextIdx = nr * cols + nc
                if (dist[nextIdx] != -1) continue
                if (isBlocked(walls, r, c, nr, nc)) continue

                dist[nextIdx] = dist[cur] + 1
                queue.add(nextIdx)
            }
        }
        return dist
    }

    fun canPlaceWall(state: GameState, p: Int, wall: Wall): Boolean {
        if (state.leftWalls[p] <= 0) return false
        if (wall.r !in 0..state.rows - 2 || wall.c !in 0..state.cols - 2) return false

        for (e in state.walls) {
            if (wallsConflict(e, wall)) return false
        }

        val testWalls = state.walls + wall
        val p0Goal = goalRow(0, state)
        val p1Goal = goalRow(1, state)

        return hasPath(testWalls, state.pawns[0], p0Goal, state.cols, state.rows) &&
                hasPath(testWalls, state.pawns[1], p1Goal, state.cols, state.rows)
    }

    fun applyMove(state: GameState, move: Move): GameState? {
        if (state.winner != null) return null
        val p = state.turn

        return when (move) {
            is Move.PawnStep -> {
                val legalMoves = pawnMoves(state, p)
                if (legalMoves.none { it.r == move.target.r && it.c == move.target.c }) return null

                val newPawns = state.pawns.toMutableList()
                newPawns[p] = move.target

                val goal = goalRow(p, state)
                val newWinner = if (move.target.r == goal) p else null

                state.copy(
                    pawns = newPawns,
                    turn = if (newWinner != null) p else 1 - p,
                    winner = newWinner,
                    moveHistory = state.moveHistory + move
                )
            }
            is Move.WallPlacement -> {
                if (!canPlaceWall(state, p, move.wall)) return null

                val newLeft = state.leftWalls.clone()
                newLeft[p]--

                state.copy(
                    walls = state.walls + move.wall,
                    leftWalls = newLeft,
                    turn = 1 - p,
                    moveHistory = state.moveHistory + move
                )
            }
        }
    }
}
