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
import kotlin.random.Random

sealed class SpinRewardType {
    data class Coins(val amount: Int) : SpinRewardType()
    data class Cosmetic(val id: String, val name: String, val icon: String, val tier: String, val fallbackCoins: Int = 1000) : SpinRewardType()
}

data class WheelSegment(
    val index: Int,
    val label: String,
    val subLabel: String,
    val icon: String,
    val reward: SpinRewardType,
    val weight: Int, // Weight for probability calculation
    val colorHex: Long,
    val badge: String? = null
)

data class SpinnerState(
    val canSpinToday: Boolean = true,
    val lastSpinDate: String? = null,
    val totalSpins: Int = 0,
    val lastWonItem: String? = null,
    val spinFee: Int = DailySpinnerManager.SPIN_FEE_COINS
) {
    val hasFreeSpin: Boolean get() = canSpinToday
}

data class SpinOutcome(
    val winningSegment: WheelSegment,
    val targetAngleDegrees: Float,
    val isJackpot: Boolean,
    val isCosmetic: Boolean,
    val rewardSummary: String,
    val coinsAwarded: Int
)

@Singleton
class DailySpinnerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nakamaRepository: NakamaRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_spinner", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _spinnerState = MutableStateFlow(computeSpinnerState())
    val spinnerState: StateFlow<SpinnerState> = _spinnerState.asStateFlow()

    companion object {
        const val SPIN_FEE_COINS = 500

        val SEGMENTS = listOf(
            WheelSegment(
                index = 0,
                label = "500",
                subLabel = "Coins",
                icon = "🪙",
                reward = SpinRewardType.Coins(500),
                weight = 38,
                colorHex = 0xFF0D9488,
                badge = "500"
            ),
            WheelSegment(
                index = 1,
                label = "750",
                subLabel = "Coins",
                icon = "💰",
                reward = SpinRewardType.Coins(750),
                weight = 28,
                colorHex = 0xFF0284C7,
                badge = "750"
            ),
            WheelSegment(
                index = 2,
                label = "CYBER",
                subLabel = "Cyber Core",
                icon = "🔮",
                reward = SpinRewardType.Cosmetic("ball_cybernetic_core", "Cybernetic Core Ball", "🔮", "EPIC", 1000),
                weight = 2,
                colorHex = 0xFF7C3AED,
                badge = "EPIC"
            ),
            WheelSegment(
                index = 3,
                label = "1,000",
                subLabel = "Coins",
                icon = "💎",
                reward = SpinRewardType.Coins(1000),
                weight = 18,
                colorHex = 0xFF059669,
                badge = "1,000"
            ),
            WheelSegment(
                index = 4,
                label = "QUANTUM",
                subLabel = "Quantum Energy",
                icon = "⚡",
                reward = SpinRewardType.Cosmetic("ball_quantum_energy", "Quantum Energy Ball", "⚡", "LEGEND", 1500),
                weight = 2,
                colorHex = 0xFFDB2777,
                badge = "LEGEND"
            ),
            WheelSegment(
                index = 5,
                label = "1,500",
                subLabel = "Coins",
                icon = "🪙",
                reward = SpinRewardType.Coins(1500),
                weight = 10,
                colorHex = 0xFF2563EB,
                badge = "1,500"
            ),
            WheelSegment(
                index = 6,
                label = "BLACKHOLE",
                subLabel = "Micro Void",
                icon = "🕳️",
                reward = SpinRewardType.Cosmetic("ball_micro_blackhole", "Micro Blackhole Ball", "🕳️", "MYTHIC", 2000),
                weight = 1,
                colorHex = 0xFFC026D3,
                badge = "MYTHIC"
            ),
            WheelSegment(
                index = 7,
                label = "2,500",
                subLabel = "Jackpot",
                icon = "🏆",
                reward = SpinRewardType.Coins(2500),
                weight = 3,
                colorHex = 0xFFD97706,
                badge = "JACKPOT"
            )
        )
    }

    init {
        scope.launch {
            syncFromNakama()
        }
    }

    private fun todayDateUtc(): String {
        return LocalDate.now(ZoneOffset.UTC).toString()
    }

    private fun computeSpinnerState(): SpinnerState {
        val today = todayDateUtc()
        val lastDate = prefs.getString("spinner_last_date", null)
        val totalSpins = prefs.getInt("spinner_total_spins", 0)
        val lastWon = prefs.getString("spinner_last_won", null)

        val canSpinToday = lastDate != today
        return SpinnerState(
            canSpinToday = canSpinToday,
            lastSpinDate = lastDate,
            totalSpins = totalSpins,
            lastWonItem = lastWon,
            spinFee = SPIN_FEE_COINS
        )
    }

    suspend fun syncFromNakama() {
        try {
            val serverSpinner = nakamaRepository.fetchDailySpinnerFromNakama()
            if (serverSpinner != null) {
                val serverDate = serverSpinner.optString("lastSpinDate", "")
                val serverTotal = serverSpinner.optInt("totalSpins", 0)
                val localDate = prefs.getString("spinner_last_date", "") ?: ""

                if (serverDate.isNotBlank() && (localDate.isBlank() || serverDate >= localDate)) {
                    prefs.edit()
                        .putString("spinner_last_date", serverDate)
                        .putInt("spinner_total_spins", maxOf(serverTotal, prefs.getInt("spinner_total_spins", 0)))
                        .apply()
                }
            }
            _spinnerState.value = computeSpinnerState()
        } catch (_: Exception) { }
    }

    fun canSpinToday(): Boolean = _spinnerState.value.canSpinToday

    fun performSpin(isFreeSpin: Boolean = false): SpinOutcome {
        val totalWeight = SEGMENTS.sumOf { it.weight }
        val randomVal = Random.nextInt(totalWeight)

        var accumulated = 0
        var winningSegment = SEGMENTS[0]

        for (segment in SEGMENTS) {
            accumulated += segment.weight
            if (randomVal < accumulated) {
                winningSegment = segment
                break
            }
        }

        val segmentAngle = 360f / SEGMENTS.size
        // Indicator points at top (270 degrees or 0 index).
        // Each segment is 45 degrees.
        // If segment 0 is centered at -90 deg (or 0 deg), we calculate target angle for pointer.
        val targetCenterAngle = winningSegment.index * segmentAngle
        // Spin multiple full rotations (e.g. 5 full rounds = 1800 deg) plus the offset to stop under the top pointer
        val fullRotations = 5 * 360f
        val finalAngle = fullRotations + (360f - targetCenterAngle)

        val today = todayDateUtc()
        val totalSpins = prefs.getInt("spinner_total_spins", 0) + 1

        val editor = prefs.edit()
        editor.putString("spinner_last_date", today)
        editor.putInt("spinner_total_spins", totalSpins)
        editor.putString("spinner_last_won", winningSegment.label)
        editor.apply()

        _spinnerState.value = computeSpinnerState()

        val isJackpot = winningSegment.index == 7
        val isCosmetic = winningSegment.reward is SpinRewardType.Cosmetic
        val coinsAwarded = when (val r = winningSegment.reward) {
            is SpinRewardType.Coins -> r.amount
            is SpinRewardType.Cosmetic -> r.fallbackCoins
        }

        val rewardSummary = when (val r = winningSegment.reward) {
            is SpinRewardType.Coins -> "+${r.amount} Coins 🪙"
            is SpinRewardType.Cosmetic -> "Unlocked [${r.tier}] ${r.name} ${r.icon}!"
        }

        // Sync with Nakama Cloud
        scope.launch {
            try {
                if (winningSegment.reward is SpinRewardType.Coins) {
                    nakamaRepository.rpcProcessCoinTransaction(
                        winningSegment.reward.amount,
                        "lucky_spinner_${winningSegment.label.lowercase().replace(" ", "_")}"
                    )
                } else if (winningSegment.reward is SpinRewardType.Cosmetic) {
                    nakamaRepository.rpcProcessCoinTransaction(
                        winningSegment.reward.fallbackCoins,
                        "lucky_spinner_skin_${winningSegment.reward.id}"
                    )
                }
                nakamaRepository.syncDailySpinnerToNakama(today, totalSpins, winningSegment.label)
            } catch (_: Exception) { }
        }

        return SpinOutcome(
            winningSegment = winningSegment,
            targetAngleDegrees = finalAngle,
            isJackpot = isJackpot,
            isCosmetic = isCosmetic,
            rewardSummary = rewardSummary,
            coinsAwarded = coinsAwarded
        )
    }
}
