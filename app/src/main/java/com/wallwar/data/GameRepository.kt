package com.wallwar.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(private val matchDao: MatchDao) {
    val allMatches: Flow<List<MatchRecord>> = matchDao.getAllMatches()
    val playerWins: Flow<Int> = matchDao.getPlayerWinsCount()
    val totalMatches: Flow<Int> = matchDao.getTotalMatchesCount()

    suspend fun recordMatch(match: MatchRecord) {
        matchDao.insertMatch(match)
    }

    suspend fun restoreMatches(matches: List<MatchRecord>) {
        if (matches.isNotEmpty()) {
            matchDao.insertMatches(matches)
        }
    }

    suspend fun clearAllHistory() {
        matchDao.clearHistory()
    }
}
