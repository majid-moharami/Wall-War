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
     * Generic event logger with safe try/catch and diagnostic logcat output
     */
    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics.logEvent(eventName, params)
            Log.d(TAG, "📊 [Analytics] Event: $eventName | Params: ${params?.let { bundleToString(it) }}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [Analytics] Failed to log event $eventName: ${e.message}")
        }
    }

    private fun bundleToString(bundle: Bundle): String {
        return bundle.keySet().joinToString(", ") { key -> "$key=${bundle.get(key)}" }
    }

    /**
     * Screen Navigation Tracking
     */
    fun logScreenView(screenName: String, screenClass: String = screenName) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    /**
     * User Authentication & Identity
     */
    fun logLogin(method: String, userId: String? = null, isGuest: Boolean = false, username: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
            putBoolean("is_guest", isGuest)
            userId?.let { putString("user_id", it) }
            username?.let { putString("user_name", it) }
        }
        logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
        logEvent("user_login", bundle)
        if (!userId.isNullOrBlank()) {
            setUserId(userId)
        }
        setUserProperty("login_method", method)
        setUserProperty("is_guest_user", isGuest.toString())
    }

    fun logSignUp(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    fun logLogout() {
        logEvent("user_logout")
    }

    fun setUserId(userId: String?) {
        try {
            firebaseAnalytics.setUserId(userId)
            Log.d(TAG, "📊 [Analytics] Set user ID: $userId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [Analytics] Failed to set user ID: ${e.message}")
        }
    }

    fun setUserProperty(name: String, value: String?) {
        try {
            firebaseAnalytics.setUserProperty(name, value)
            Log.d(TAG, "📊 [Analytics] User property $name = $value")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [Analytics] Failed to set user property $name: ${e.message}")
        }
    }

    fun logProfileUpdated(hasCustomName: Boolean, avatarId: String) {
        val bundle = Bundle().apply {
            putBoolean("has_custom_name", hasCustomName)
            putString("avatar_id", avatarId)
        }
        logEvent("profile_updated", bundle)
    }

    // ==========================================
    // AD MONETIZATION ANALYTICS
    // ==========================================

    fun logAdRequested(
        adType: String,
        adNetwork: String,
        placementId: String? = null,
        triggerLocation: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("ad_network", adNetwork)
            placementId?.let { putString("placement_id", it) }
            triggerLocation?.let { putString("trigger_location", it) }
        }
        logEvent("ad_requested", bundle)
    }

    fun logAdLoaded(
        adType: String,
        adNetwork: String,
        placementId: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("ad_network", adNetwork)
            placementId?.let { putString("placement_id", it) }
        }
        logEvent("ad_loaded", bundle)
    }

    fun logAdImpression(
        adType: String,
        adNetwork: String,
        placementId: String? = null,
        triggerLocation: String? = null
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_PLATFORM, adNetwork)
            putString(FirebaseAnalytics.Param.AD_SOURCE, adNetwork)
            putString(FirebaseAnalytics.Param.AD_FORMAT, adType)
            placementId?.let { putString(FirebaseAnalytics.Param.AD_UNIT_NAME, it) }
            triggerLocation?.let { putString("trigger_location", it) }
        }
        logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle)
    }

    fun logAdFailed(
        adType: String,
        adNetwork: String,
        errorMessage: String? = null,
        triggerLocation: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("ad_network", adNetwork)
            errorMessage?.let { putString("error_message", it) }
            triggerLocation?.let { putString("trigger_location", it) }
        }
        logEvent("ad_failed", bundle)
    }

    /**
     * Specifically tracking users who complete watching rewarded ads to get coins
     */
    fun logRewardedAdWatchedForCoins(
        adNetwork: String,
        rewardCoins: Int = 50,
        triggerLocation: String = "coin_shop"
    ) {
        val bundle = Bundle().apply {
            putString("ad_network", adNetwork)
            putInt("reward_coins", rewardCoins)
            putString("reward_type", "coins")
            putString("trigger_location", triggerLocation)
        }
        logEvent("ad_reward_coins_earned", bundle)
        logCoinsEarned(rewardCoins, "rewarded_ad_$adNetwork")
    }

    /**
     * Specifically tracking users who watch rewarded ads for free game match entry
     */
    fun logRewardedAdWatchedForFreeEntry(
        adNetwork: String,
        triggerLocation: String = "offline_entry"
    ) {
        val bundle = Bundle().apply {
            putString("ad_network", adNetwork)
            putString("reward_type", "free_match_entry")
            putString("trigger_location", triggerLocation)
        }
        logEvent("ad_reward_free_entry", bundle)
    }

    /**
     * Specifically tracking interstitial ads shown after games
     */
    fun logInterstitialAdShownAfterGame(
        adNetwork: String,
        completedMatchCount: Int,
        triggerLocation: String = "post_match"
    ) {
        val bundle = Bundle().apply {
            putString("ad_network", adNetwork)
            putInt("completed_matches_count", completedMatchCount)
            putString("trigger_location", triggerLocation)
        }
        logEvent("ad_interstitial_after_game", bundle)
    }

    fun logAdDismissed(
        adType: String,
        adNetwork: String,
        wasRewardEarned: Boolean = false
    ) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("ad_network", adNetwork)
            putBoolean("was_reward_earned", wasRewardEarned)
        }
        logEvent("ad_dismissed", bundle)
    }

    // ==========================================
    // IN-APP PURCHASE & STORE MONETIZATION
    // ==========================================

    fun logPurchaseInitiated(
        productId: String,
        expectedCoins: Int,
        priceString: String? = null
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, "Coin Pack ($expectedCoins Coins)")
            putInt("expected_coins", expectedCoins)
            priceString?.let { putString("price_formatted", it) }
        }
        logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, bundle)
    }

    /**
     * Log successful coin purchase via Google Play Billing
     */
    fun logPurchaseSuccess(
        productId: String,
        coinsAwarded: Int,
        orderId: String?,
        isSandbox: Boolean = false
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, "Coin Pack ($coinsAwarded Coins)")
            putInt("coins_awarded", coinsAwarded)
            orderId?.let { putString(FirebaseAnalytics.Param.TRANSACTION_ID, it) }
            putBoolean("is_sandbox", isSandbox)
            putString(FirebaseAnalytics.Param.CURRENCY, "USD")
        }
        logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
        logCoinsEarned(coinsAwarded, "in_app_purchase_$productId")
    }

    fun logPurchaseFailed(
        productId: String,
        responseCode: Int?,
        errorMessage: String
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
            responseCode?.let { putInt("response_code", it) }
            putString("error_message", errorMessage)
        }
        logEvent("iap_purchase_failed", bundle)
    }

    fun logPurchaseCancelled(productId: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
        }
        logEvent("iap_purchase_cancelled", bundle)
    }

    fun logPurchasePending(productId: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
        }
        logEvent("iap_purchase_pending", bundle)
    }

    // ==========================================
    // IN-GAME ECONOMY & COSMETICS
    // ==========================================

    fun logCoinsEarned(amount: Int, source: String) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.VALUE, amount)
            putInt("amount", amount)
            putString("source", source)
        }
        logEvent("coins_earned", bundle)
    }

    fun logCoinsSpent(amount: Int, item: String, category: String = "store") {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.VALUE, amount)
            putInt("amount", amount)
            putString(FirebaseAnalytics.Param.ITEM_NAME, item)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, category)
        }
        logEvent(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY, bundle)
    }

    fun logSkinPurchased(skinId: String, skinType: String, costCoins: Int) {
        val bundle = Bundle().apply {
            putString("skin_id", skinId)
            putString("skin_type", skinType)
            putInt("cost_coins", costCoins)
        }
        logEvent("skin_purchased", bundle)
        logCoinsSpent(costCoins, skinId, "skin_$skinType")
    }

    fun logSkinEquipped(skinId: String, skinType: String) {
        val bundle = Bundle().apply {
            putString("skin_id", skinId)
            putString("skin_type", skinType)
        }
        logEvent("skin_equipped", bundle)
    }

    fun logEmojiPurchased(emojiId: String, costCoins: Int) {
        val bundle = Bundle().apply {
            putString("emoji_id", emojiId)
            putInt("cost_coins", costCoins)
        }
        logEvent("emoji_purchased", bundle)
        logCoinsSpent(costCoins, emojiId, "emoji")
    }

    // ==========================================
    // DAILY RETENTION & QUESTS
    // ==========================================

    fun logDailyStreakClaimed(day: Int, coinsAwarded: Int, wasReset: Boolean = false, currentStreak: Int = day) {
        val bundle = Bundle().apply {
            putInt("streak_day", day)
            putInt("coins_awarded", coinsAwarded)
            putBoolean("was_reset", wasReset)
            putInt("current_streak", currentStreak)
        }
        logEvent("daily_streak_claimed", bundle)
        logEvent("daily_reward_claimed", bundle)
        logCoinsEarned(coinsAwarded, "daily_streak_day_$day")
    }

    fun logDailySpinnerSpun(rewardType: String, amount: Int, isFree: Boolean, rewardLabel: String = "") {
        val bundle = Bundle().apply {
            putString("reward_type", rewardType)
            putInt("reward_amount", amount)
            putBoolean("is_free_spin", isFree)
            if (rewardLabel.isNotBlank()) putString("reward_label", rewardLabel)
        }
        logEvent("daily_spinner_spun", bundle)
        logEvent("lucky_spin_claimed", bundle)
        if (rewardType == "coins" && amount > 0) {
            logCoinsEarned(amount, "daily_spinner")
        }
    }

    fun logDailyQuestClaimed(questId: String, questTitle: String, coinsAwarded: Int, xpAwarded: Int = 0) {
        val bundle = Bundle().apply {
            putString("quest_id", questId)
            putString("quest_title", questTitle)
            putInt("coins_awarded", coinsAwarded)
            putInt("xp_awarded", xpAwarded)
        }
        logEvent("daily_quest_claimed", bundle)
        logCoinsEarned(coinsAwarded, "quest_$questId")
    }

    // ==========================================
    // MATCHMAKING & GAMEPLAY
    // ==========================================

    fun logMatchmakingStarted(arenaId: String, arenaTitle: String, matchFee: Int) {
        val bundle = Bundle().apply {
            putString("arena_id", arenaId)
            putString("arena_title", arenaTitle)
            putInt("match_fee", matchFee)
        }
        logEvent("matchmaking_started", bundle)
    }

    fun logMatchmakingMatched(arenaId: String, opponentName: String, waitTimeSeconds: Long) {
        val bundle = Bundle().apply {
            putString("arena_id", arenaId)
            putString("opponent_name", opponentName)
            putLong("wait_time_seconds", waitTimeSeconds)
        }
        logEvent("matchmaking_matched", bundle)
    }

    fun logMatchmakingCancelled(arenaId: String, waitTimeSeconds: Long) {
        val bundle = Bundle().apply {
            putString("arena_id", arenaId)
            putLong("wait_time_seconds", waitTimeSeconds)
        }
        logEvent("matchmaking_cancelled", bundle)
    }

    fun logMatchStart(
        gameMode: String,
        difficulty: String? = null,
        isOnline: Boolean = false,
        arenaId: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
            difficulty?.let { putString("difficulty", it) }
            putBoolean("is_online", isOnline)
            arenaId?.let { putString("arena_id", it) }
        }
        logEvent(FirebaseAnalytics.Event.LEVEL_START, bundle)
        logEvent("match_start", bundle)
    }

    fun logMatchEnd(
        gameMode: String,
        isWin: Boolean,
        winnerName: String,
        durationSeconds: Long = 0,
        turnsCount: Int = 0,
        wallsPlaced: Int = 0,
        prizeCoins: Int = 0,
        arenaId: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
            putBoolean("is_win", isWin)
            putString("winner", winnerName)
            putLong("duration_seconds", durationSeconds)
            putInt("turns_count", turnsCount)
            putInt("walls_placed", wallsPlaced)
            putInt("prize_coins", prizeCoins)
            arenaId?.let { putString("arena_id", it) }
        }
        logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle)
        logEvent("match_end", bundle)

        if (isWin && prizeCoins > 0) {
            logCoinsEarned(prizeCoins, "match_win_${arenaId ?: gameMode}")
        }
    }

    fun logEmojiSent(emojiId: String, isOnline: Boolean) {
        val bundle = Bundle().apply {
            putString("emoji_id", emojiId)
            putBoolean("is_online", isOnline)
        }
        logEvent("emoji_sent", bundle)
    }

    fun logForfeit(gameMode: String, isOnline: Boolean, durationSeconds: Long) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
            putBoolean("is_online", isOnline)
            putLong("duration_seconds", durationSeconds)
        }
        logEvent("match_forfeited", bundle)
    }

    companion object {
        private const val TAG = "WallWarAnalytics"
    }
}

