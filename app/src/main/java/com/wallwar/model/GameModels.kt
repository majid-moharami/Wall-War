package com.wallwar.model

enum class GameMode(val displayName: String, val cols: Int, val rows: Int, val defaultWalls: Int) {
    DUEL("Classic Duel", 9, 11, 10),
    RACE("Wall Race", 9, 11, 15)
}

enum class AiDifficulty(val displayName: String, val budgetMs: Long) {
    EASY("Easy", 100L),
    NORMAL("Normal", 300L),
    PRO("Master AI", 700L)
}

enum class OpponentType {
    AI,
    LOCAL_PASS_PLAY,
    ONLINE
}

data class Position(val r: Int, val c: Int)

data class Wall(val r: Int, val c: Int, val isHorizontal: Boolean, val playerOwner: Int = 0)

sealed class Move {
    data class PawnStep(val target: Position) : Move()
    data class WallPlacement(val wall: Wall) : Move()
}

data class GameState(
    val mode: GameMode = GameMode.DUEL,
    val cols: Int = mode.cols,
    val rows: Int = mode.rows,
    val pawns: List<Position> = listOf(Position(rows - 1, cols / 2), Position(0, cols / 2)),
    val walls: List<Wall> = emptyList(),
    val leftWalls: IntArray = intArrayOf(mode.defaultWalls, mode.defaultWalls),
    val turn: Int = 0,
    val winner: Int? = null,
    val moveHistory: List<Move> = emptyList(),
    val isAiMatch: Boolean = false,
    val aiDifficulty: AiDifficulty = AiDifficulty.NORMAL
) {
    fun isGameOver(): Boolean = winner != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameState
        return mode == other.mode &&
                cols == other.cols &&
                rows == other.rows &&
                pawns == other.pawns &&
                walls == other.walls &&
                leftWalls.contentEquals(other.leftWalls) &&
                turn == other.turn &&
                winner == other.winner
    }

    override fun hashCode(): Int {
        var result = mode.hashCode()
        result = 31 * result + cols
        result = 31 * result + rows
        result = 31 * result + pawns.hashCode()
        result = 31 * result + walls.hashCode()
        result = 31 * result + leftWalls.contentHashCode()
        result = 31 * result + turn
        result = 31 * result + (winner ?: -1)
        return result
    }
}

enum class BoardTheme(val title: String, val primaryColor: Long, val gridBg: Long, val cellBg: Long, val wallColor: Long) {
    ELEGANT_DARK("Elegant Dark", 0xFFD0BCFF, 0xFF111318, 0xFF1C1B1F, 0xFFD0BCFF),
    MODERN_VIOLET("Modern Violet", 0xFF7C5CFF, 0xFF1D1B26, 0xFF2B283A, 0xFF9F85FF),
    CLASSIC_WOOD("Classic Wood", 0xFF8B5A2B, 0xFF3D2314, 0xFF5C3A21, 0xFFE0A96D),
    CYBER_NEON("Cyber Neon", 0xFF00E5FF, 0xFF0A0E1A, 0xFF161F33, 0xFFFF007A)
}
