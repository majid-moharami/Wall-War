package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecord)

    @Query("DELETE FROM match_records")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM match_records WHERE winnerPlayer = 0")
    fun getPlayerWinsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM match_records")
    fun getTotalMatchesCount(): Flow<Int>
}
