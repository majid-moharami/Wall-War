package com.wallwar.data.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.wallwar.data.AuthRepository
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class AdNetwork(val displayName: String) {
    ADMOB("AdMob Network"),
    TAPSELL("Tapsell Network")
}

enum class AdType(val title: String) {
    REWARDED("Rewarded Video Ad"),
    INTERSTITIAL("Match Interstitial Ad")
}

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val nakamaRepository: NakamaRepository,
    private val adMobManager: AdMobManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val isRewardedAdLoading: StateFlow<Boolean> = adMobManager.isRewardedAdLoading
    val isRewardedAdReady: StateFlow<Boolean> = adMobManager.isRewardedAdReady

    val isInterstitialLoading: StateFlow<Boolean> = adMobManager.isInterstitialLoading
    val isInterstitialAdReady: StateFlow<Boolean> = adMobManager.isInterstitialAdReady
    val isInterstitialReady: StateFlow<Boolean> = adMobManager.isInterstitialReady

    private val _isAdPlaying = MutableStateFlow(false)
    val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

    private val _activeNetwork = MutableStateFlow(AdNetwork.ADMOB)
    val activeNetwork: StateFlow<AdNetwork> = _activeNetwork.asStateFlow()

    private val _currentAdType = MutableStateFlow<AdType?>(null)
    val currentAdType: StateFlow<AdType?> = _currentAdType.asStateFlow()

    private val _adCountdown = MutableStateFlow(5)
    val adCountdown: StateFlow<Int> = _adCountdown.asStateFlow()

    private val _rewardDescription = MutableStateFlow("Reward: +50 Coins 🪙")
    val rewardDescription: StateFlow<String> = _rewardDescription.asStateFlow()

    private val _completedMatchCount = MutableStateFlow(0)
    val completedMatchCount: StateFlow<Int> = _completedMatchCount.asStateFlow()

    private val _rewardToast = MutableStateFlow<String?>(null)
    val rewardToast: StateFlow<String?> = _rewardToast.asStateFlow()

    init {
        // Interstitials will be loaded on demand or when a game is in progress; rewarded ads only on user button click
    }

    fun preloadInterstitialAd() {
        adMobManager.loadInterstitialAd(context)
    }

    /**
     * Increments the completed match counter.
     * Returns true if an interstitial ad should be triggered (strictly after every 2 completed matches).
     */
    fun recordMatchCompleted(): Boolean {
        val newCount = _completedMatchCount.value + 1
        _completedMatchCount.value = newCount
        return newCount % 2 == 0
    }

    fun watchRewardedAd(
        activity: Activity? = null,
        rewardCoins: Int = 50,
        onSuccess: (coins: Int) -> Unit = {},
        onClosed: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        if (_isAdPlaying.value || isRewardedAdLoading.value) {
            Log.w("AdManager", "Ad is already loading or playing, ignoring click.")
            return
        }

        _rewardDescription.value = "Reward: +$rewardCoins Coins 🪙"

        if (activity != null) {
            adMobManager.loadAndShowRewardedAd(
                activity = activity,
                onRewardEarned = {
                    scope.launch {
                        nakamaRepository.rpcProcessCoinTransaction(rewardCoins, "rewarded_ad")
                        authRepository.addCoins(rewardCoins)
                        val successMsg = "🎉 +$rewardCoins Coins Earned from Watching Ad!"
                        _rewardToast.value = successMsg
                        onSuccess(rewardCoins)
                    }
                },
                onAdDismissed = {
                    onClosed()
                },
                onFailed = { errorMsg ->
                    scope.launch {
                        _rewardToast.value = errorMsg
                        onError?.invoke(errorMsg)
                        onClosed()
                    }
                }
            )
            return
        }

        // Fallback simulated ad overlay if activity is null
        scope.launch {
            _isAdPlaying.value = true
            _currentAdType.value = AdType.REWARDED
            _activeNetwork.value = AdNetwork.ADMOB
            _adCountdown.value = 5

            for (i in 5 downTo 1) {
                _adCountdown.value = i
                delay(1000)
            }

            _isAdPlaying.value = false
            _currentAdType.value = null

            // Award coins via Nakama Server RPC & AuthRepository local state
            nakamaRepository.rpcProcessCoinTransaction(rewardCoins, "rewarded_ad")
            authRepository.addCoins(rewardCoins)

            val successMsg = "🎉 +$rewardCoins Coins Earned from Watching Ad!"
            _rewardToast.value = successMsg
            onSuccess(rewardCoins)
            onClosed()
        }
    }

    fun watchRewardedAdForFreeEntry(
        activity: Activity? = null,
        onSuccess: () -> Unit = {},
        onClosed: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        if (_isAdPlaying.value || isRewardedAdLoading.value) {
            Log.w("AdManager", "Ad is already loading or playing, ignoring click.")
            return
        }

        _rewardDescription.value = "Reward: Free Match Entry 🎮"

        if (activity != null) {
            adMobManager.loadAndShowRewardedAd(
                activity = activity,
                onRewardEarned = {
                    scope.launch {
                        val successMsg = "🎉 Free Entry Unlocked! Starting Match..."
                        _rewardToast.value = successMsg
                        onSuccess()
                    }
                },
                onAdDismissed = {
                    onClosed()
                },
                onFailed = { errorMsg ->
                    scope.launch {
                        _rewardToast.value = errorMsg
                        onError?.invoke(errorMsg)
                        onClosed()
                    }
                }
            )
            return
        }

        // Fallback simulated ad overlay if activity is null
        scope.launch {
            _isAdPlaying.value = true
            _currentAdType.value = AdType.REWARDED
            _activeNetwork.value = AdNetwork.ADMOB
            _adCountdown.value = 5

            for (i in 5 downTo 1) {
                _adCountdown.value = i
                delay(1000)
            }

            _isAdPlaying.value = false
            _currentAdType.value = null

            val successMsg = "🎉 Free Entry Unlocked! Starting Match..."
            _rewardToast.value = successMsg
            onSuccess()
            onClosed()
        }
    }

    fun showInterstitialIfTriggered(
        activity: Activity? = null,
        onAdClosed: () -> Unit
    ) {
        val shouldShow = _completedMatchCount.value > 0 && _completedMatchCount.value % 2 == 0
        if (!shouldShow) {
            onAdClosed()
            return
        }

        if (_isAdPlaying.value) {
            onAdClosed()
            return
        }

        if (activity != null) {
            adMobManager.loadAndShowInterstitialAd(
                activity = activity,
                onAdDismissed = onAdClosed,
                onFailed = {
                    onAdClosed()
                }
            )
            return
        }

        scope.launch {
            _isAdPlaying.value = true
            _currentAdType.value = AdType.INTERSTITIAL
            _activeNetwork.value = AdNetwork.ADMOB
            _adCountdown.value = 3

            for (i in 3 downTo 1) {
                _adCountdown.value = i
                delay(1000)
            }

            _isAdPlaying.value = false
            _currentAdType.value = null

            onAdClosed()

            adMobManager.loadInterstitialAd(context)
        }
    }

    fun clearRewardToast() {
        _rewardToast.value = null
    }
}
