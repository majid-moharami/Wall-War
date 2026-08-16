package com.wallwar.model

enum class GameMode(val displayName: String, val cols: Int, val rows: Int, val defaultWalls: Int) {
    DUEL("Classic Duel", 9, 11, 10),
    RACE("Wall Race", 9, 11, 15),
    QUICK_5V5("Quick 5v5", 9, 9, 5),
    SUDDEN_DEATH("Sudden Death", 9, 11, 3)
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

enum class TilePattern {
    MATTE_DARK,
    METALLIC_GRID,
    CARBON_FIBER,
    MATRIX_CIRCUIT,
    VOLCANIC_ROCK,
    DARK_CRYSTAL,
    OBSIDIAN_GOLD
}

enum class RadarType {
    NONE,
    SIMPLE_CROSSHAIR,
    METALLIC_RADAR,
    CYBER_CROSSHAIR,
    MATRIX_GRID,
    VOLCANIC_CORE,
    CRYSTAL_ORB,
    ROYAL_COSMIC_RING
}

enum class BoardTheme(
    val id: String,
    val title: String,
    val primaryColor: Long,
    val gridBg: Long,
    val cellBg: Long,
    val wallColor: Long,
    val topGlowColor: Long,
    val bottomGlowColor: Long,
    val outerBgTop: Long,
    val outerBgBottom: Long,
    val gridBorderColor: Long,
    val tilePattern: TilePattern,
    val radarType: RadarType,
    val centerRingColor: Long
) {
    STARTER(
        id = "starter",
        title = "Classic Neon",
        primaryColor = 0xFF3B82F6,
        gridBg = 0xFF0A0E1A,
        cellBg = 0xFF121B30,
        wallColor = 0xFF3B82F6,
        topGlowColor = 0xFF3B82F6,
        bottomGlowColor = 0xFFEF4444,
        outerBgTop = 0xFF151D33,
        outerBgBottom = 0xFF101628,
        gridBorderColor = 0xFF1D2B4A,
        tilePattern = TilePattern.MATTE_DARK,
        radarType = RadarType.SIMPLE_CROSSHAIR,
        centerRingColor = 0x333B82F6
    ),
    NOVICE(
        id = "novice",
        title = "Subtle Metallic",
        primaryColor = 0xFF00E5FF,
        gridBg = 0xFF08121E,
        cellBg = 0xFF0F2238,
        wallColor = 0xFF00E5FF,
        topGlowColor = 0xFF00E5FF,
        bottomGlowColor = 0xFFFF9100,
        outerBgTop = 0xFF10263E,
        outerBgBottom = 0xFF081628,
        gridBorderColor = 0xFF1A3B5C,
        tilePattern = TilePattern.METALLIC_GRID,
        radarType = RadarType.METALLIC_RADAR,
        centerRingColor = 0x4400E5FF
    ),
    AMATEUR(
        id = "amateur",
        title = "Carbon Fiber",
        primaryColor = 0xFFE056FD,
        gridBg = 0xFF120818,
        cellBg = 0xFF22122E,
        wallColor = 0xFFE056FD,
        topGlowColor = 0xFFFF007A,
        bottomGlowColor = 0xFF7C5CFF,
        outerBgTop = 0xFF2B143A,
        outerBgBottom = 0xFF180A22,
        gridBorderColor = 0xFF422158,
        tilePattern = TilePattern.CARBON_FIBER,
        radarType = RadarType.CYBER_CROSSHAIR,
        centerRingColor = 0x44FF007A
    ),
    PRO(
        id = "pro",
        title = "Matrix Circuit",
        primaryColor = 0xFF00FF87,
        gridBg = 0xFF051810,
        cellBg = 0xFF0B2E1E,
        wallColor = 0xFF00FF87,
        topGlowColor = 0xFF00FF87,
        bottomGlowColor = 0xFF10B981,
        outerBgTop = 0xFF0F3B29,
        outerBgBottom = 0xFF062016,
        gridBorderColor = 0xFF17573D,
        tilePattern = TilePattern.MATRIX_CIRCUIT,
        radarType = RadarType.MATRIX_GRID,
        centerRingColor = 0x4400FF87
    ),
    HIGH_ROLLER(
        id = "highroller",
        title = "Volcanic Seams",
        primaryColor = 0xFFFF5500,
        gridBg = 0xFF1A0A05,
        cellBg = 0xFF33150A,
        wallColor = 0xFFFF5500,
        topGlowColor = 0xFFFF5500,
        bottomGlowColor = 0xFFEF4444,
        outerBgTop = 0xFF3D1B0F,
        outerBgBottom = 0xFF210E07,
        gridBorderColor = 0xFF5C2916,
        tilePattern = TilePattern.VOLCANIC_ROCK,
        radarType = RadarType.VOLCANIC_CORE,
        centerRingColor = 0x55FF5500
    ),
    MASTER(
        id = "master",
        title = "Dark Crystal",
        primaryColor = 0xFF9D4EDD,
        gridBg = 0xFF0F071A,
        cellBg = 0xFF211038,
        wallColor = 0xFF9D4EDD,
        topGlowColor = 0xFF3A0CA3,
        bottomGlowColor = 0xFFC77DFF,
        outerBgTop = 0xFF2A1545,
        outerBgBottom = 0xFF140A24,
        gridBorderColor = 0xFF482375,
        tilePattern = TilePattern.DARK_CRYSTAL,
        radarType = RadarType.CRYSTAL_ORB,
        centerRingColor = 0x55C77DFF
    ),
    GRAND_CHAMPION(
        id = "grandchampion",
        title = "Obsidian Gold",
        primaryColor = 0xFFFFD700,
        gridBg = 0xFF141005,
        cellBg = 0xFF2B220B,
        wallColor = 0xFFFFD700,
        topGlowColor = 0xFFFFD700,
        bottomGlowColor = 0xFFFFFFFF,
        outerBgTop = 0xFF3B2F0F,
        outerBgBottom = 0xFF1C1607,
        gridBorderColor = 0xFF6B551C,
        tilePattern = TilePattern.OBSIDIAN_GOLD,
        radarType = RadarType.ROYAL_COSMIC_RING,
        centerRingColor = 0x66FFD700
    );

    companion object {
        // Legacy aliases to preserve backward compatibility
        val ELEGANT_DARK = STARTER
        val MODERN_VIOLET = AMATEUR
        val CLASSIC_WOOD = HIGH_ROLLER
        val CYBER_NEON = PRO

        fun fromId(id: String): BoardTheme {
            return entries.find { it.id == id } ?: PRO
        }
    }
}
