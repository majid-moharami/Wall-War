package com.wallwar.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    /**
     * Generic event logger
     */
    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics.logEvent(eventName, params)
            Log.d(TAG, "Logged event: $eventName, params: $params")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log event $eventName: ${e.message}")
        }
    }

    /**
     * Log Screen View
     */
    fun logScreenView(screenName: String, screenClass: String = screenName) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    /**
     * Log User Authentication
     */
    fun logLogin(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    /**
     * Log User ID and User Properties
     */
    fun setUserId(userId: String?) {
        try {
            firebaseAnalytics.setUserId(userId)
            Log.d(TAG, "Set user ID: $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set user ID: ${e.message}")
        }
    }

    fun setUserProperty(name: String, value: String?) {
        try {
            firebaseAnalytics.setUserProperty(name, value)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set user property $name: ${e.message}")
        }
    }

    /**
     * Match / Game Lifecycle Analytics
     */
    fun logMatchStart(gameMode: String, difficulty: String? = null, isOnline: Boolean = false) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
            difficulty?.let { putString("difficulty", it) }
            putBoolean("is_online", isOnline)
        }
        logEvent("match_start", bundle)
    }

    fun logMatchEnd(
        gameMode: String,
        isWin: Boolean,
        winnerName: String,
        durationSeconds: Long = 0,
        turnsCount: Int = 0,
        wallsPlaced: Int = 0
    ) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
            putBoolean("is_win", isWin)
            putString("winner", winnerName)
            putLong("duration_seconds", durationSeconds)
            putInt("turns_count", turnsCount)
            putInt("walls_placed", wallsPlaced)
        }
        logEvent("match_end", bundle)
    }

    /**
     * In-game actions
     */
    fun logWallPlaced(orientation: String, isOnline: Boolean) {
        val bundle = Bundle().apply {
            putString("wall_orientation", orientation)
            putBoolean("is_online", isOnline)
        }
        logEvent("wall_placed", bundle)
    }

    fun logPawnMoved(isJump: Boolean, isOnline: Boolean) {
        val bundle = Bundle().apply {
            putBoolean("is_jump", isJump)
            putBoolean("is_online", isOnline)
        }
        logEvent("pawn_moved", bundle)
    }

    /**
     * Economy & Store
     */
    fun logCoinsEarned(amount: Int, source: String) {
        val bundle = Bundle().apply {
            putInt("amount", amount)
            putString("source", source)
        }
        logEvent("coins_earned", bundle)
    }

    fun logCoinsSpent(amount: Int, item: String) {
        val bundle = Bundle().apply {
            putInt("amount", amount)
            putString("item", item)
        }
        logEvent("coins_spent", bundle)
    }

    fun logRewardClaimed(rewardType: String, amount: Int) {
        val bundle = Bundle().apply {
            putString("reward_type", rewardType)
            putInt("amount", amount)
        }
        logEvent("reward_claimed", bundle)
    }

    fun logQuestCompleted(questTitle: String) {
        val bundle = Bundle().apply {
            putString("quest_title", questTitle)
        }
        logEvent("quest_completed", bundle)
    }

    companion object {
        private const val TAG = "AnalyticsManager"
    }
}
