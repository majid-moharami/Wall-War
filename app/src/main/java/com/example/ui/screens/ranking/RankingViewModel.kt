package com.example.ui.screens.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.GameRepository
import com.example.data.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    val leaderboard: StateFlow<List<LeaderboardPlayer>> = combine(
        authRepository.userProfile,
        gameRepository.playerWins
    ) { user: UserProfile, winsCount: Int ->
        val simulatedOthers = listOf(
            LeaderboardPlayer(1, "Apex Cybermaster", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde", false, 3200, 48, 88, "Apex Master", 15),
            LeaderboardPlayer(2, "WallTactician_99", "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61", false, 2850, 39, 82, "Neon Legend", 12),
            LeaderboardPlayer(3, "GridRunner_Pro", "https://images.unsplash.com/photo-1580489944761-15a19d654956", false, 2450, 31, 78, "Grandmaster", 10),
            LeaderboardPlayer(5, "QuantumPawn", "https://images.unsplash.com/photo-1527980965255-d3b416303d12", false, 1950, 22, 65, "Cyber Knight", 6),
            LeaderboardPlayer(6, "Wall_Blocker_X", "https://images.unsplash.com/photo-1628157582853-a796fa650a6a", false, 1620, 18, 58, "Strategist", 5)
        )

        val userWins = winsCount.coerceAtLeast(user.wins)
        val userPlayer = LeaderboardPlayer(
            rank = 4,
            name = if (user.isLoggedIn) user.displayName else "You (Guest)",
            avatarUrl = user.photoUrl,
            isUser = true,
            trophies = user.trophies + userWins * 10,
            wins = userWins,
            winRate = if (user.totalMatches > 0) ((userWins.toFloat() / user.totalMatches) * 100).toInt() else 70,
            title = user.rankTitle,
            level = user.level
        )

        (simulatedOthers + userPlayer).sortedByDescending { it.trophies }.mapIndexed { index, p ->
            p.copy(rank = index + 1)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
