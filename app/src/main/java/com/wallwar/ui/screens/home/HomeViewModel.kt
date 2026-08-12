package com.wallwar.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.data.Arena
import com.wallwar.data.ArenaConfig
import com.wallwar.data.AuthRepository
import com.wallwar.data.GameRepository
import com.wallwar.data.UserProfile
import com.wallwar.model.AiDifficulty
import com.wallwar.model.GameMode
import com.wallwar.model.OpponentType
import com.wallwar.data.SettingsRepository
import com.wallwar.model.BoardTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val boardTheme: StateFlow<BoardTheme> = settingsRepository.boardTheme

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    val totalWins: StateFlow<Int> = gameRepository.playerWins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMatches: StateFlow<Int> = gameRepository.totalMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val onlineArenas: List<Arena> = ArenaConfig.onlineArenas
    val offlineArena: Arena = ArenaConfig.offlineAiArena

    private val _arenaErrorMessage = MutableStateFlow<String?>(null)
    val arenaErrorMessage: StateFlow<String?> = _arenaErrorMessage.asStateFlow()

    private val _bonusMessage = MutableStateFlow<String?>(null)
    val bonusMessage: StateFlow<String?> = _bonusMessage.asStateFlow()

    fun joinOnlineArenaMatch(
        arena: Arena,
        onSuccess: (GameMode, OpponentType, AiDifficulty, Arena) -> Unit
    ) {
        if (!userProfile.value.isLoggedIn) {
            _arenaErrorMessage.value = "Sign in required to access Ranked Online Arenas!"
            return
        }
        val currentCoins = userProfile.value.coins
        if (currentCoins < arena.entryFee) {
            _arenaErrorMessage.value = "Not enough coins for ${arena.title}! Entry fee is ${arena.entryFee} Coins."
            return
        }

        val success = authRepository.deductCoins(arena.entryFee)
        if (success) {
            _arenaErrorMessage.value = null
            onSuccess(GameMode.DUEL, OpponentType.ONLINE, AiDifficulty.NORMAL, arena)
        } else {
            _arenaErrorMessage.value = "Failed to deduct coins. Please try again."
        }
    }

    fun joinOfflineMatch(
        opponentType: OpponentType,
        difficulty: AiDifficulty,
        useAdForFreeEntry: Boolean,
        onSuccess: (GameMode, OpponentType, AiDifficulty, Arena) -> Unit
    ) {
        if (useAdForFreeEntry) {
            // Rewarded Ad grants free entry (bypasses 50 coins entry fee) without adding +25 coins
            _arenaErrorMessage.value = null
            onSuccess(GameMode.DUEL, opponentType, difficulty, offlineArena)
        } else {
            val entryFee = offlineArena.entryFee // 50 coins
            val currentCoins = userProfile.value.coins
            if (currentCoins < entryFee) {
                _arenaErrorMessage.value = "Not enough coins! Offline entry fee is $entryFee Coins or watch a Rewarded Ad for free entry."
                return
            }
            val success = authRepository.deductCoins(entryFee)
            if (success) {
                _arenaErrorMessage.value = null
                onSuccess(GameMode.DUEL, opponentType, difficulty, offlineArena)
            } else {
                _arenaErrorMessage.value = "Failed to deduct coins. Please try again."
            }
        }
    }

    fun claimDailyBonus() {
        authRepository.addCoins(25)
        _bonusMessage.value = "Claimed +25 Bonus Coins! 🪙"
    }

    fun clearArenaErrorMessage() {
        _arenaErrorMessage.value = null
    }

    fun clearBonusMessage() {
        _bonusMessage.value = null
    }
}
