package com.wallwar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_records")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeName: String, // "Classic Duel" or "Wall Race"
    val opponentName: String, // "AI (Normal)", "Pass & Play", etc.
    val winnerPlayer: Int, // 0 = Player 1 / Human, 1 = Player 2 / AI
    val totalMoves: Int,
    val totalWallsPlaced: Int,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)
