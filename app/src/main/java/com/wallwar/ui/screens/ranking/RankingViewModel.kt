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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardPlayer(
    val id: String = "",
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

data class RankingData(
    val displayedLeaderboard: List<LeaderboardPlayer> = emptyList(),
    val currentUserPlayer: LeaderboardPlayer? = null
)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val nakamaRepository: NakamaRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _nakamaLeaderboard = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())

    private fun isGuestName(name: String?): Boolean {
        val clean = name?.trim() ?: ""
        return clean.isBlank() ||
                clean.startsWith("Guest_", ignoreCase = true) ||
                clean.equals("Guest", ignoreCase = true) ||
                clean.startsWith("Guest Duelist", ignoreCase = true) ||
                clean.startsWith("Duelist", ignoreCase = true) ||
                clean.startsWith("player_", ignoreCase = true)
    }

    private val _rankingData: StateFlow<RankingData> = combine(
        authRepository.userProfile,
        gameRepository.playerWins,
        _nakamaLeaderboard
    ) { user: UserProfile, winsCount: Int, nakamaList: List<LeaderboardPlayer> ->
        val userWins = winsCount.coerceAtLeast(user.wins)
        val currentUserId = user.nakamaUserId ?: nakamaRepository.getNakamaUserId()
        val userDisplayName = user.displayName.trim()

        // 1. Filter out all guest accounts (guest accounts are not shown on leaderboard)
        val nonGuestList = nakamaList.filter { !isGuestName(it.name) }

        // 2. Identify any entry matching the current user across different devices/sessions
        fun isCurrentUser(player: LeaderboardPlayer): Boolean {
            if (player.isUser) return true
            if (!currentUserId.isNullOrBlank() && player.id == currentUserId) return true
            if (userDisplayName.isNotBlank() && !isGuestName(userDisplayName) && player.name.equals(userDisplayName, ignoreCase = true)) return true
            return false
        }

        val userMatches = nonGuestList.filter { isCurrentUser(it) }

        // Determine best trophies, wins, and level for the current user
        val bestUserTrophies = if (userMatches.isNotEmpty()) {
            maxOf(user.trophies, userMatches.maxOf { it.trophies })
        } else {
            user.trophies
        }
        val bestUserWins = if (userMatches.isNotEmpty()) {
            maxOf(userWins, userMatches.maxOf { it.wins })
        } else {
            userWins
        }
        val bestUserLevel = if (userMatches.isNotEmpty()) {
            maxOf(user.level, userMatches.maxOf { it.level })
        } else {
            user.level
        }
        val bestAvatar = user.photoUrl ?: userMatches.firstOrNull { !it.avatarUrl.isNullOrBlank() }?.avatarUrl

        val canonicalUserPlayer = LeaderboardPlayer(
            id = currentUserId ?: userMatches.firstOrNull()?.id ?: "local_user",
            rank = 1,
            name = if (userDisplayName.isNotBlank()) userDisplayName else (userMatches.firstOrNull()?.name ?: "Player"),
            avatarUrl = bestAvatar,
            isUser = true,
            trophies = bestUserTrophies,
            wins = bestUserWins,
            winRate = if (user.totalMatches > 0) ((bestUserWins.toFloat() / user.totalMatches) * 100).toInt() else if (bestUserWins > 0) 75 else 0,
            title = user.rankTitle,
            level = bestUserLevel
        )

        // 3. Deduplicate opponents by normalized name so no player appears twice (e.g. multi-phone logins)
        val deduplicatedMap = mutableMapOf<String, LeaderboardPlayer>()

        for (player in nonGuestList) {
            if (!isCurrentUser(player)) {
                val key = player.name.trim().lowercase()
                val existing = deduplicatedMap[key]
                if (existing == null || player.trophies > existing.trophies || (player.trophies == existing.trophies && player.wins > existing.wins)) {
                    deduplicatedMap[key] = player.copy(isUser = false)
                }
            }
        }

        // Add the single canonical current user if logged in or has a valid profile
        if (user.isLoggedIn || userDisplayName.isNotBlank()) {
            val userKey = canonicalUserPlayer.name.trim().lowercase()
            deduplicatedMap[userKey] = canonicalUserPlayer
        }

        // 4. Sort globally: Trophies (primary DESC), Wins (secondary DESC)
        val sortedGlobalList = deduplicatedMap.values
            .sortedWith(
                compareByDescending<LeaderboardPlayer> { it.trophies }
                    .thenByDescending { it.wins }
            )
            .mapIndexed { index, player ->
                player.copy(rank = index + 1)
            }

        // 5. Locate current user's entry with exact global rank
        val currentUserWithRank = sortedGlobalList.firstOrNull { it.isUser } ?: canonicalUserPlayer

        // 6. Limit displayed list to at most 100 players
        val displayedList = sortedGlobalList.take(100)

        RankingData(
            displayedLeaderboard = displayedList,
            currentUserPlayer = currentUserWithRank
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RankingData())

    val leaderboard: StateFlow<List<LeaderboardPlayer>> = _rankingData.map { it.displayedLeaderboard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserPlayer: StateFlow<LeaderboardPlayer?> = _rankingData.map { it.currentUserPlayer }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshLeaderboard()
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = userProfile.value
                val username = if (user.displayName.isNotBlank()) user.displayName else "Duelist"
                if (!nakamaRepository.hasValidSession()) {
                    nakamaRepository.ensureAuthenticatedGuest(username)
                }
                val currentNakamaUserId = user.nakamaUserId ?: nakamaRepository.getNakamaUserId()
                try {
                    if (user.isLoggedIn || !user.displayName.isNullOrBlank()) {
                        nakamaRepository.syncUserProfileToNakama(user)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("RankingViewModel", "Error syncing profile before leaderboard fetch: ${e.message}")
                }

                val entries = nakamaRepository.fetchGlobalLeaderboard()
                android.util.Log.d("RankingViewModel", "fetchGlobalLeaderboard returned ${entries.size} entries")
                if (entries.isNotEmpty()) {
                    val mapped = entries.map { entry ->
                        // Match current user strictly by Nakama user ID, or display name if logged in
                        val isCurr = (!currentNakamaUserId.isNullOrBlank() && entry.userId == currentNakamaUserId) ||
                                (user.displayName.isNotBlank() && !user.displayName.startsWith("Guest_") && entry.displayName.equals(user.displayName, ignoreCase = true))

                        val trophies = if (isCurr) maxOf(entry.trophies, user.trophies) else entry.trophies
                        val wins = if (isCurr) maxOf(entry.wins, user.wins) else entry.wins
                        val winRate = if (wins > 0) 75 else 0

                        LeaderboardPlayer(
                            id = entry.userId,
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
                    android.util.Log.d("RankingViewModel", "Updated _nakamaLeaderboard with ${mapped.size} mapped players")
                } else {
                    android.util.Log.w("RankingViewModel", "fetchGlobalLeaderboard returned empty entries list")
                }
            } catch (e: Exception) {
                android.util.Log.e("RankingViewModel", "Error in refreshLeaderboard: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
