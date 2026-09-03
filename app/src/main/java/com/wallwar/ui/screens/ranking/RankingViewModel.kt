package com.wallwar.ui.screens.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.data.AuthRepository
import com.wallwar.data.GameRepository
import com.wallwar.data.UserProfile
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardPlayer(
    val rank: Int,
    val name: String,
    val avatarUrl: String?,
    val isUser: Boolean,
    val trophies: Int,
    val wins: Int,
    val winRate: Int,
    val title: String,
    val level: Int
) {
    val titleResId: Int
        @androidx.annotation.StringRes get() = when {
            trophies >= 1000 -> com.wallwar.R.string.rank_apex_duelist
            trophies >= 500 -> com.wallwar.R.string.rank_grand_champion
            trophies >= 200 -> com.wallwar.R.string.rank_neon_knight
            else -> com.wallwar.R.string.rank_novice_duelist
        }
}

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val nakamaRepository: NakamaRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    private val _nakamaLeaderboard = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())

    val leaderboard: StateFlow<List<LeaderboardPlayer>> = combine(
        authRepository.userProfile,
        gameRepository.playerWins,
        _nakamaLeaderboard
    ) { user: UserProfile, winsCount: Int, nakamaList: List<LeaderboardPlayer> ->
        if (nakamaList.isNotEmpty()) {
            nakamaList
        } else {
            val userWins = winsCount.coerceAtLeast(user.wins)
            val userPlayer = LeaderboardPlayer(
                rank = 1,
                name = if (user.isLoggedIn) user.displayName else "${user.displayName} (You)",
                avatarUrl = user.photoUrl,
                isUser = true,
                trophies = user.trophies,
                wins = userWins,
                winRate = if (user.totalMatches > 0) ((userWins.toFloat() / user.totalMatches) * 100).toInt() else 0,
                title = user.rankTitle,
                level = user.level
            )

            listOf(userPlayer)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshLeaderboard()
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            val entries = nakamaRepository.fetchGlobalLeaderboard()
            val user = userProfile.value
            if (entries.isNotEmpty()) {
                val mapped = entries.map { entry ->
                    LeaderboardPlayer(
                        rank = entry.rank,
                        name = entry.displayName,
                        avatarUrl = if (entry.username.equals(user.displayName, ignoreCase = true)) {
                            user.photoUrl ?: entry.avatarUrl
                        } else {
                            entry.avatarUrl
                        },
                        isUser = entry.username.equals(user.displayName, ignoreCase = true),
                        trophies = entry.trophies,
                        wins = entry.wins,
                        winRate = 75,
                        title = when {
                            entry.trophies >= 1000 -> "Apex Cybermaster"
                            entry.trophies >= 500 -> "Neon Grandmaster"
                            entry.trophies >= 200 -> "Neon Knight"
                            else -> "Novice Duelist"
                        },
                        level = entry.level
                    )
                }
                _nakamaLeaderboard.value = mapped
            }
        }
    }
}
