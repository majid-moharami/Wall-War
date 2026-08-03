package com.wallwar.engine

import com.wallwar.model.AiDifficulty
import com.wallwar.model.GameState
import com.wallwar.model.Move
import com.wallwar.model.Position
import com.wallwar.model.Wall
import kotlin.random.Random

object AiEngine {

    fun computeBestMove(state: GameState, difficulty: AiDifficulty = AiDifficulty.NORMAL): Move {
        val p = state.turn
        val legalSteps = GameEngine.pawnMoves(state, p)
        val defaultPawnStep = legalSteps.firstOrNull() ?: state.pawns[p]

        if (state.winner != null || legalSteps.isEmpty()) {
            return Move.PawnStep(defaultPawnStep)
        }

        val rawMove = try {
            when (difficulty) {
                AiDifficulty.EASY -> computeEasyMove(state, p, legalSteps)
                AiDifficulty.NORMAL -> computeNormalMove(state, p, legalSteps)
                AiDifficulty.PRO -> computeProMove(state, p, legalSteps)
            }
        } catch (_: Exception) {
            Move.PawnStep(defaultPawnStep)
        }

        // Strict Move Validation Safety Guard
        return when (rawMove) {
            is Move.PawnStep -> {
                if (legalSteps.any { it.r == rawMove.target.r && it.c == rawMove.target.c }) {
                    rawMove
                } else {
                    Move.PawnStep(defaultPawnStep)
                }
            }
            is Move.WallPlacement -> {
                if (GameEngine.canPlaceWall(state, p, rawMove.wall)) {
                    rawMove
                } else {
                    Move.PawnStep(defaultPawnStep)
                }
            }
        }
    }

    private fun computeEasyMove(state: GameState, p: Int, moves: List<Position>): Move {
        val goal = GameEngine.goalRow(p, state)
        val distMap = GameEngine.distToGoal(state.walls, goal, state.cols, state.rows)
        val me = state.pawns[p]

        var minD = Int.MAX_VALUE
        for (m in moves) {
            val d = distMap[m.r * state.cols + m.c]
            if (d in 0 until minD) minD = d
        }

        val bestMoves = moves.filter { distMap[it.r * state.cols + it.c] == minD }

        // 20% chance to occasionally place a random valid wall if available
        if (Random.nextFloat() < 0.20f && state.leftWalls[p] > 0) {
            val opp = state.pawns[1 - p]
            for (dr in -1..0) {
                for (dc in -1..0) {
                    val r = opp.r + dr
                    val c = opp.c + dc
                    if (r in 0..state.rows - 2 && c in 0..state.cols - 2) {
                        val wH = Wall(r, c, isHorizontal = true, playerOwner = p)
                        if (GameEngine.canPlaceWall(state, p, wH)) return Move.WallPlacement(wH)
                        val wV = Wall(r, c, isHorizontal = false, playerOwner = p)
                        if (GameEngine.canPlaceWall(state, p, wV)) return Move.WallPlacement(wV)
                    }
                }
            }
        }

        val chosen = bestMoves.randomOrNull() ?: moves.randomOrNull() ?: moves.firstOrNull() ?: me
        return Move.PawnStep(chosen)
    }

    private fun computeNormalMove(state: GameState, p: Int, legalMoves: List<Position>): Move {
        val myGoal = GameEngine.goalRow(p, state)
        val oppGoal = GameEngine.goalRow(1 - p, state)

        val myDistMap = GameEngine.distToGoal(state.walls, myGoal, state.cols, state.rows)
        val oppDistMap = GameEngine.distToGoal(state.walls, oppGoal, state.cols, state.rows)

        val me = state.pawns[p]
        val opp = state.pawns[1 - p]

        val myDist = myDistMap[me.r * state.cols + me.c]
        val oppDist = oppDistMap[opp.r * state.cols + opp.c]

        // If opponent is ahead or equal, evaluate aggressive wall placement
        if (state.leftWalls[p] > 0 && myDist >= 0 && oppDist >= 0 && (oppDist <= myDist || Random.nextFloat() < 0.35f)) {
            var bestWall: Wall? = null
            var maxGain = 0

            val oppP = state.pawns[1 - p]
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = oppP.r + dr
                    val c = oppP.c + dc
                    if (r in 0..state.rows - 2 && c in 0..state.cols - 2) {
                        for (isHorizontal in listOf(true, false)) {
                            val candidateWall = Wall(r, c, isHorizontal, playerOwner = p)
                            if (GameEngine.canPlaceWall(state, p, candidateWall)) {
                                val testWalls = state.walls + candidateWall
                                val newOppDistMap = GameEngine.distToGoal(testWalls, oppGoal, state.cols, state.rows)
                                val newMyDistMap = GameEngine.distToGoal(testWalls, myGoal, state.cols, state.rows)

                                val newOppDist = newOppDistMap[opp.r * state.cols + opp.c]
                                val newMyDist = newMyDistMap[me.r * state.cols + me.c]

                                if (newOppDist >= 0 && newMyDist >= 0) {
                                    val oppGain = newOppDist - oppDist
                                    val myCost = newMyDist - myDist
                                    val netGain = oppGain - myCost
                                    if (netGain > maxGain) {
                                        maxGain = netGain
                                        bestWall = candidateWall
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (bestWall != null && maxGain >= 1) {
                return Move.WallPlacement(bestWall)
            }
        }

        // Step towards goal
        var minD = Int.MAX_VALUE
        for (m in legalMoves) {
            val d = myDistMap[m.r * state.cols + m.c]
            if (d in 0 until minD) minD = d
        }
        val bestPawnSteps = legalMoves.filter { myDistMap[it.r * state.cols + it.c] == minD }
        val chosenStep = bestPawnSteps.randomOrNull() ?: legalMoves.firstOrNull() ?: me
        return Move.PawnStep(chosenStep)
    }

    private fun computeProMove(state: GameState, p: Int, legalMoves: List<Position>): Move {
        val myGoal = GameEngine.goalRow(p, state)
        val oppGoal = GameEngine.goalRow(1 - p, state)

        val myDistMap = GameEngine.distToGoal(state.walls, myGoal, state.cols, state.rows)
        val oppDistMap = GameEngine.distToGoal(state.walls, oppGoal, state.cols, state.rows)

        val me = state.pawns[p]
        val opp = state.pawns[1 - p]

        val myDist = myDistMap[me.r * state.cols + me.c]
        val oppDist = oppDistMap[opp.r * state.cols + opp.c]

        // Check winning pawn step first
        for (m in legalMoves) {
            if (m.r == myGoal) return Move.PawnStep(m)
        }

        // Evaluate all candidate walls with highest impact
        var bestWall: Wall? = null
        var maxVal = -1000

        if (state.leftWalls[p] > 0 && myDist >= 0 && oppDist >= 0) {
            val candRadius = 2
            for (r in (opp.r - candRadius).coerceAtLeast(0)..(opp.r + candRadius).coerceAtMost(state.rows - 2)) {
                for (c in (opp.c - candRadius).coerceAtLeast(0)..(opp.c + candRadius).coerceAtMost(state.cols - 2)) {
                    for (isHorizontal in listOf(true, false)) {
                        val w = Wall(r, c, isHorizontal, playerOwner = p)
                        if (GameEngine.canPlaceWall(state, p, w)) {
                            val testWalls = state.walls + w
                            val testOppDist = GameEngine.distToGoal(testWalls, oppGoal, state.cols, state.rows)[opp.r * state.cols + opp.c]
                            val testMyDist = GameEngine.distToGoal(testWalls, myGoal, state.cols, state.rows)[me.r * state.cols + me.c]

                            if (testOppDist >= 0 && testMyDist >= 0) {
                                val value = (testOppDist - oppDist) * 3 - (testMyDist - myDist) * 2
                                if (value > maxVal) {
                                    maxVal = value
                                    bestWall = w
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bestWall != null && maxVal >= 2) {
            return Move.WallPlacement(bestWall)
        }

        // Best pawn step
        var minD = Int.MAX_VALUE
        for (m in legalMoves) {
            val d = myDistMap[m.r * state.cols + m.c]
            if (d in 0 until minD) minD = d
        }
        val bestSteps = legalMoves.filter { myDistMap[it.r * state.cols + it.c] == minD }
        val chosenStep = bestSteps.randomOrNull() ?: legalMoves.firstOrNull() ?: me
        return Move.PawnStep(chosenStep)
    }
}
