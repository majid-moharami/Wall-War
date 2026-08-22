package com.wallwar.ui.screens.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.audio.SoundManager
import com.wallwar.data.Arena
import com.wallwar.data.ArenaConfig
import com.wallwar.data.AuthRepository
import com.wallwar.data.DailyMission
import com.wallwar.data.DailyMissionManager
import com.wallwar.data.DailySpinnerManager
import com.wallwar.data.DailyStreakManager
import com.wallwar.data.DailyStreakState
import com.wallwar.data.GameRepository
import com.wallwar.data.SpinOutcome
import com.wallwar.data.SpinRewardType
import com.wallwar.data.SpinnerState
import com.wallwar.data.UserProfile
import com.wallwar.data.ad.AdManager
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
    val soundManager: SoundManager,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val adManager: AdManager,
    private val dailyStreakManager: DailyStreakManager,
    private val dailyMissionManager: DailyMissionManager,
    private val dailySpinnerManager: DailySpinnerManager
) : ViewModel() {

    val boardTheme: StateFlow<BoardTheme> = settingsRepository.boardTheme

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile
    val abandonedMatchNotice: StateFlow<String?> = authRepository.abandonedMatchNotice

    val isAdPlaying: StateFlow<Boolean> = adManager.isAdPlaying
    val isRewardedAdReady: StateFlow<Boolean> = adManager.isRewardedAdReady
    val isRewardedAdLoading: StateFlow<Boolean> = adManager.isRewardedAdLoading

    // Daily Retention Systems
    val dailyStreakState: StateFlow<DailyStreakState> = dailyStreakManager.streakState
    val dailyMissions: StateFlow<List<DailyMission>> = dailyMissionManager.missions
    val dailySpinnerState: StateFlow<SpinnerState> = dailySpinnerManager.spinnerState
    val spinnerState: StateFlow<SpinnerState> = dailySpinnerState

    fun clearAbandonedMatchNotice() {
        authRepository.clearAbandonedMatchNotice()
    }

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
        val currentCoins = userProfile.value.coins
        if (currentCoins < arena.entryFee) {
            _arenaErrorMessage.value = "Not enough coins for ${arena.title}! Entry fee is ${arena.entryFee} Coins."
            return
        }

        // Do not deduct coins before successful matchmaking.
        // The entry fee will be deducted only once an opponent is found in GameViewModel.
        _arenaErrorMessage.value = null
        onSuccess(GameMode.DUEL, OpponentType.ONLINE, AiDifficulty.NORMAL, arena)
    }

    fun joinOfflineMatch(
        opponentType: OpponentType,
        difficulty: AiDifficulty,
        useAdForFreeEntry: Boolean,
        onSuccess: (GameMode, OpponentType, AiDifficulty, Arena) -> Unit
    ) {
        if (useAdForFreeEntry) {
            _arenaErrorMessage.value = null
            onSuccess(GameMode.DUEL, opponentType, difficulty, offlineArena)
        } else {
            val entryFee = offlineArena.entryFee
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

    fun joinOfflineMatchWithAd(
        activity: Activity?,
        opponentType: OpponentType,
        difficulty: AiDifficulty,
        onSuccess: (GameMode, OpponentType, AiDifficulty, Arena) -> Unit
    ) {
        _arenaErrorMessage.value = null
        adManager.watchRewardedAdForFreeEntry(
            activity = activity,
            onSuccess = {
                onSuccess(GameMode.DUEL, opponentType, difficulty, offlineArena)
            },
            onError = { error ->
                _arenaErrorMessage.value = error
            }
        )
    }

    fun claimDailyBonus() {
        val result = dailyStreakManager.claimDailyBonus()
        if (result.coinsAwarded > 0) {
            authRepository.addCoins(result.coinsAwarded)
            soundManager.playCoinSound()
            val resetNote = if (result.wasReset) " (Streak Reset)" else ""
            _bonusMessage.value = "Day ${result.newStreakDay} Streak Claimed! +${result.coinsAwarded} Coins awarded! 🪙$resetNote"
        } else {
            _bonusMessage.value = "You have already claimed today's daily bonus! Come back tomorrow."
        }
    }

    fun claimMissionReward(missionId: String) {
        val reward = dailyMissionManager.claimMissionReward(missionId)
        if (reward != null) {
            val (coins, xp) = reward
            authRepository.addCoins(coins)
            soundManager.playCoinSound()
            _bonusMessage.value = "Mission Complete! +$coins Coins & +$xp XP received! 🎯"
        }
    }

    fun spinWheel(isFree: Boolean = false): SpinOutcome {
        authRepository.deductCoins(DailySpinnerManager.SPIN_FEE_COINS)
        val outcome = dailySpinnerManager.performSpin(false)
        when (val r = outcome.winningSegment.reward) {
            is SpinRewardType.Coins -> {
                authRepository.addCoins(r.amount)
                soundManager.playCoinSound()
            }
            is SpinRewardType.Cosmetic -> {
                // Special rare skin fallback/reward coins added & synced to user profile
                authRepository.addCoins(r.fallbackCoins)
                soundManager.playCoinSound()
            }
        }
        return outcome
    }

    fun clearArenaErrorMessage() {
        _arenaErrorMessage.value = null
    }

    fun clearBonusMessage() {
        _bonusMessage.value = null
    }
}

