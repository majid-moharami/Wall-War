package com.wallwar.data

import android.content.Context
import android.content.SharedPreferences
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

data class DailyStreakState(
    val currentDay: Int = 0,
    val canClaim: Boolean = true,
    val todayReward: Int = 25,
    val longestStreak: Int = 0,
    val lastClaimDate: String? = null
)

data class DailyStreakResult(
    val coinsAwarded: Int,
    val newStreakDay: Int,
    val wasReset: Boolean
)

@Singleton
class DailyStreakManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nakamaRepository: NakamaRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_streak", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _streakState = MutableStateFlow(computeStreakState())
    val streakState: StateFlow<DailyStreakState> = _streakState.asStateFlow()

    companion object {
        private val STREAK_REWARDS = intArrayOf(25, 50, 75, 100, 150, 200, 500)

        fun rewardForDay(day: Int): Int {
            val idx = (day - 1).coerceIn(0, STREAK_REWARDS.lastIndex)
            return STREAK_REWARDS[idx]
        }
    }

    init {
        // Sync with Nakama Cloud on startup
        scope.launch {
            syncFromNakama()
        }
    }

    private fun todayDateUtc(): String {
        return LocalDate.now(ZoneOffset.UTC).toString() // yyyy-MM-dd
    }

    private fun computeStreakState(): DailyStreakState {
        val storedDate = prefs.getString("streak_last_claim_date", null)
        val storedDay = prefs.getInt("streak_current_day", 0)
        val longestStreak = prefs.getInt("streak_longest", 0)
        val today = todayDateUtc()

        if (storedDate == null || storedDay == 0) {
            return DailyStreakState(
                currentDay = 0,
                canClaim = true,
                todayReward = rewardForDay(1),
                longestStreak = longestStreak
            )
        }

        if (storedDate == today) {
            return DailyStreakState(
                currentDay = storedDay,
                canClaim = false,
                todayReward = rewardForDay(storedDay),
                longestStreak = longestStreak,
                lastClaimDate = storedDate
            )
        }

        val lastDate = try { LocalDate.parse(storedDate) } catch (_: Exception) { null }
        val todayDate = LocalDate.parse(today)

        return if (lastDate != null && todayDate.minusDays(1) == lastDate) {
            val nextDay = if (storedDay >= 7) 1 else storedDay + 1
            DailyStreakState(
                currentDay = storedDay,
                canClaim = true,
                todayReward = rewardForDay(nextDay),
                longestStreak = longestStreak,
                lastClaimDate = storedDate
            )
        } else {
            DailyStreakState(
                currentDay = 0,
                canClaim = true,
                todayReward = rewardForDay(1),
                longestStreak = longestStreak,
                lastClaimDate = storedDate
            )
        }
    }

    suspend fun syncFromNakama() {
        try {
            val nakamaStreak = nakamaRepository.fetchDailyStreakFromNakama()
            if (nakamaStreak != null) {
                val serverDate = nakamaStreak.optString("lastClaimDate", "")
                val serverDay = nakamaStreak.optInt("currentDay", 0)
                val serverLongest = nakamaStreak.optInt("longestStreak", 0)

                val localDate = prefs.getString("streak_last_claim_date", "") ?: ""

                // Prefer server data if valid and more recent
                if (serverDate.isNotBlank() && (localDate.isBlank() || serverDate >= localDate)) {
                    prefs.edit()
                        .putString("streak_last_claim_date", serverDate)
                        .putInt("streak_current_day", serverDay)
                        .putInt("streak_longest", maxOf(serverLongest, prefs.getInt("streak_longest", 0)))
                        .apply()
                }
            }
            _streakState.value = computeStreakState()
        } catch (_: Exception) { }
    }

    fun canClaimToday(): Boolean = _streakState.value.canClaim

    fun claimDailyBonus(): DailyStreakResult {
        val state = computeStreakState()
        if (!state.canClaim) {
            return DailyStreakResult(
                coinsAwarded = 0,
                newStreakDay = state.currentDay,
                wasReset = false
            )
        }

        val today = todayDateUtc()
        val storedDate = prefs.getString("streak_last_claim_date", null)
        val storedDay = prefs.getInt("streak_current_day", 0)
        val longestStreak = prefs.getInt("streak_longest", 0)

        val wasReset: Boolean
        val newDay: Int

        if (storedDate == null || storedDay == 0) {
            newDay = 1
            wasReset = false
        } else {
            val lastDate = try { LocalDate.parse(storedDate) } catch (_: Exception) { null }
            val todayDate = LocalDate.parse(today)

            if (lastDate != null && todayDate.minusDays(1) == lastDate) {
                newDay = if (storedDay >= 7) 1 else storedDay + 1
                wasReset = storedDay >= 7
            } else {
                newDay = 1
                wasReset = true
            }
        }

        val reward = rewardForDay(newDay)
        val newLongest = maxOf(longestStreak, newDay)

        prefs.edit()
            .putString("streak_last_claim_date", today)
            .putInt("streak_current_day", newDay)
            .putInt("streak_longest", newLongest)
            .apply()

        _streakState.value = computeStreakState()

        // Sync with Nakama Server (Storage + Coin Transaction RPC)
        scope.launch {
            try {
                nakamaRepository.syncDailyStreakToNakama(newDay, newLongest, today)
                nakamaRepository.rpcProcessCoinTransaction(reward, "daily_streak_day_$newDay")
            } catch (_: Exception) { }
        }

        return DailyStreakResult(
            coinsAwarded = reward,
            newStreakDay = newDay,
            wasReset = wasReset
        )
    }
}
