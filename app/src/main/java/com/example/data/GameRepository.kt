package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val matchDao: MatchDao) {
    val allMatches: Flow<List<MatchRecord>> = matchDao.getAllMatches()
    val playerWins: Flow<Int> = matchDao.getPlayerWinsCount()
    val totalMatches: Flow<Int> = matchDao.getTotalMatchesCount()

    suspend fun recordMatch(match: MatchRecord) {
        matchDao.insertMatch(match)
    }

    suspend fun clearAllHistory() {
        matchDao.clearHistory()
    }
}
