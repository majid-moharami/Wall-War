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
        val userWins = winsCount.coerceAtLeast(user.wins)
        val userPlayer = LeaderboardPlayer(
            rank = 1,
            name = if (user.displayName.isNotBlank()) user.displayName else "Duelist",
            avatarUrl = user.photoUrl,
            isUser = true,
            trophies = user.trophies,
            wins = userWins,
            winRate = if (user.totalMatches > 0) ((userWins.toFloat() / user.totalMatches) * 100).toInt() else 0,
            title = user.rankTitle,
            level = user.level
        )

        if (nakamaList.isEmpty()) {
            listOf(userPlayer)
        } else {
            val hasUserInList = nakamaList.any { it.isUser }
            val mergedList = if (hasUserInList) {
                nakamaList.map { player ->
                    if (player.isUser) {
                        player.copy(
                            name = if (user.displayName.isNotBlank()) user.displayName else player.name,
                            avatarUrl = user.photoUrl ?: player.avatarUrl,
                            trophies = maxOf(player.trophies, user.trophies),
                            wins = maxOf(player.wins, userWins),
                            level = maxOf(player.level, user.level)
                        )
                    } else {
                        player
                    }
                }
            } else {
                nakamaList + userPlayer
            }

            // Always sort in global ranking order: Trophies (primary), Wins (secondary)
            mergedList
                .sortedWith(
                    compareByDescending<LeaderboardPlayer> { it.trophies }
                        .thenByDescending { it.wins }
                )
                .mapIndexed { index, player ->
                    player.copy(rank = index + 1)
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshLeaderboard()
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            val user = userProfile.value
            try {
                if (user.isLoggedIn || !user.displayName.isNullOrBlank()) {
                    nakamaRepository.syncUserProfileToNakama(user)
                }
            } catch (e: Exception) {
                android.util.Log.w("RankingViewModel", "Error syncing profile before leaderboard fetch: ${e.message}")
            }

            val entries = nakamaRepository.fetchGlobalLeaderboard()
            if (entries.isNotEmpty()) {
                val mapped = entries.map { entry ->
                    val isCurr = (!user.nakamaUserId.isNullOrBlank() && entry.userId == user.nakamaUserId) ||
                            (user.displayName.isNotBlank() && entry.displayName.equals(user.displayName, ignoreCase = true)) ||
                            (user.displayName.isNotBlank() && entry.username.equals(user.displayName, ignoreCase = true)) ||
                            (!user.email.isNullOrBlank() && entry.username.equals(user.email, ignoreCase = true))

                    val trophies = if (isCurr) maxOf(entry.trophies, user.trophies) else entry.trophies
                    val wins = if (isCurr) maxOf(entry.wins, user.wins) else entry.wins
                    val winRate = if (wins > 0) 75 else 0

                    LeaderboardPlayer(
                        rank = entry.rank,
                        name = if (isCurr && user.displayName.isNotBlank()) user.displayName else entry.displayName,
                        avatarUrl = if (isCurr) (user.photoUrl ?: entry.avatarUrl) else entry.avatarUrl,
                        isUser = isCurr,
                        trophies = trophies,
                        wins = wins,
                        winRate = winRate,
                        title = when {
                            trophies >= 1000 -> "Apex Cybermaster"
                            trophies >= 500 -> "Neon Grandmaster"
                            trophies >= 200 -> "Neon Knight"
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
