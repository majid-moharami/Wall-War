package com.wallwar.data.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.wallwar.analytics.AnalyticsManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class AdNetwork(val displayName: String) {
    ADMOB("Google AdMob Network"),
    ADIVERY("Adivery Ads (Iran / Persian)")
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
    private val adMobManager: AdMobManager,
    private val adiveryManager: AdiveryManager,
    private val geoLocationDetector: GeoLocationDetector,
    private val analyticsManager: AnalyticsManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val isIranUser: StateFlow<Boolean> = geoLocationDetector.isIranUser

    val activeNetwork: StateFlow<AdNetwork> = geoLocationDetector.isIranUser
        .map { isIran -> if (isIran) AdNetwork.ADIVERY else AdNetwork.ADMOB }
        .stateIn(scope, SharingStarted.Eagerly, if (geoLocationDetector.isIranUser.value) AdNetwork.ADIVERY else AdNetwork.ADMOB)

    val isRewardedAdLoading: StateFlow<Boolean> = combine(
        activeNetwork,
        adMobManager.isRewardedAdLoading,
        adiveryManager.isRewardedAdLoading
    ) { network, admobLoading, adiveryLoading ->
        if (network == AdNetwork.ADIVERY) adiveryLoading else admobLoading
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val isRewardedAdReady: StateFlow<Boolean> = combine(
        activeNetwork,
        adMobManager.isRewardedAdReady,
        adiveryManager.isRewardedAdReady
    ) { network, admobReady, adiveryReady ->
        if (network == AdNetwork.ADIVERY) adiveryReady else admobReady
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val isInterstitialLoading: StateFlow<Boolean> = combine(
        activeNetwork,
        adMobManager.isInterstitialLoading,
        adiveryManager.isInterstitialLoading
    ) { network, admobLoading, adiveryLoading ->
        if (network == AdNetwork.ADIVERY) adiveryLoading else admobLoading
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val isInterstitialAdReady: StateFlow<Boolean> = combine(
        activeNetwork,
        adMobManager.isInterstitialAdReady,
        adiveryManager.isInterstitialAdReady
    ) { network, admobReady, adiveryReady ->
        if (network == AdNetwork.ADIVERY) adiveryReady else admobReady
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val isInterstitialReady: StateFlow<Boolean> get() = isInterstitialAdReady

    private val _isAdPlaying = MutableStateFlow(false)
    val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

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
        scope.launch {
            geoLocationDetector.isIranUser.collect { isIran ->
                Log.d("AdManager", "Active ad routing configured: isIranUser=$isIran -> Network=${if (isIran) "Adivery (Persian Ads)" else "Google AdMob"}")
                if (isIran) {
                    adiveryManager.prepareInterstitialAd()
                    adiveryManager.prepareRewardedAd()
                } else {
                    adMobManager.loadInterstitialAd(context)
                }
            }
        }
    }

    fun preloadInterstitialAd() {
        if (geoLocationDetector.isIranUser.value) {
            adiveryManager.prepareInterstitialAd()
        } else {
            adMobManager.loadInterstitialAd(context)
        }
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

        val isIran = geoLocationDetector.isIranUser.value
        val country = geoLocationDetector.getCountryCode()
        val networkName = if (isIran) "adivery" else "admob"

        analyticsManager.logAdRequested(
            adType = "rewarded",
            adNetwork = networkName,
            country = country,
            isIranIp = isIran,
            triggerLocation = "coin_shop"
        )

        if (activity != null) {
            Log.d("AdManager", "Serving Rewarded Ad: isIran=$isIran, country=$country, selectedNetwork=$networkName")

            if (isIran) {
                // Iran IP: Strictly Adivery ads
                analyticsManager.logAdShowing(
                    adType = "rewarded",
                    adNetwork = "adivery",
                    country = country,
                    isIranIp = true,
                    triggerLocation = "coin_shop"
                )
                adiveryManager.loadAndShowRewardedAd(
                    activity = activity,
                    onRewardEarned = {
                        scope.launch {
                            analyticsManager.logRewardedAdWatchedForCoins(
                                adNetwork = "adivery",
                                country = country,
                                isIranIp = true,
                                rewardCoins = rewardCoins,
                                triggerLocation = "coin_shop"
                            )
                            nakamaRepository.rpcProcessCoinTransaction(rewardCoins, "rewarded_ad_adivery")
                            authRepository.addCoins(rewardCoins)
                            val successMsg = "🎉 +$rewardCoins Coins Earned from Watching Persian Ad (Adivery)!"
                            _rewardToast.value = successMsg
                            onSuccess(rewardCoins)
                        }
                    },
                    onAdDismissed = {
                        analyticsManager.logAdDismissed(
                            adType = "rewarded",
                            adNetwork = "adivery",
                            country = country,
                            isIranIp = true
                        )
                        onClosed()
                    },
                    onFailed = { adiveryError ->
                        Log.w("AdManager", "Adivery ad failed for Iran user: $adiveryError")
                        analyticsManager.logAdFailed(
                            adType = "rewarded",
                            adNetwork = "adivery",
                            country = country,
                            isIranIp = true,
                            errorMessage = adiveryError,
                            triggerLocation = "coin_shop"
                        )
                        scope.launch {
                            val finalError = "Ad could not be loaded: $adiveryError"
                            _rewardToast.value = finalError
                            onError?.invoke(finalError)
                            onClosed()
                        }
                    }
                )
            } else {
                // Non-Iran IP (All other countries): Strictly Google AdMob ads
                analyticsManager.logAdShowing(
                    adType = "rewarded",
                    adNetwork = "admob",
                    country = country,
                    isIranIp = false,
                    triggerLocation = "coin_shop"
                )
                adMobManager.loadAndShowRewardedAd(
                    activity = activity,
                    onRewardEarned = {
                        scope.launch {
                            analyticsManager.logRewardedAdWatchedForCoins(
                                adNetwork = "admob",
                                country = country,
                                isIranIp = false,
                                rewardCoins = rewardCoins,
                                triggerLocation = "coin_shop"
                            )
                            nakamaRepository.rpcProcessCoinTransaction(rewardCoins, "rewarded_ad")
                            authRepository.addCoins(rewardCoins)
                            val successMsg = "🎉 +$rewardCoins Coins Earned from Watching Ad!"
                            _rewardToast.value = successMsg
                            onSuccess(rewardCoins)
                        }
                    },
                    onAdDismissed = {
                        analyticsManager.logAdDismissed(
                            adType = "rewarded",
                            adNetwork = "admob",
                            country = country,
                            isIranIp = false
                        )
                        onClosed()
                    },
                    onFailed = { errorMsg ->
                        Log.w("AdManager", "AdMob failed for Global user ($country): $errorMsg")
                        analyticsManager.logAdFailed(
                            adType = "rewarded",
                            adNetwork = "admob",
                            country = country,
                            isIranIp = false,
                            errorMessage = errorMsg,
                            triggerLocation = "coin_shop"
                        )
                        scope.launch {
                            val msg = "Ad failed to load: $errorMsg"
                            _rewardToast.value = msg
                            onError?.invoke(msg)
                            onClosed()
                        }
                    }
                )
            }
            return
        }

        // Fallback simulated ad overlay if activity is null
        scope.launch {
            _isAdPlaying.value = true
            _currentAdType.value = AdType.REWARDED
            _adCountdown.value = 5

            for (i in 5 downTo 1) {
                _adCountdown.value = i
                delay(1000)
            }

            _isAdPlaying.value = false
            _currentAdType.value = null

            analyticsManager.logRewardedAdWatchedForCoins(
                adNetwork = "simulated",
                country = country,
                isIranIp = isIran,
                rewardCoins = rewardCoins,
                triggerLocation = "coin_shop_simulated"
            )

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

        val isIran = geoLocationDetector.isIranUser.value
        val country = geoLocationDetector.getCountryCode()
        val networkName = if (isIran) "adivery" else "admob"

        analyticsManager.logAdRequested(
            adType = "rewarded",
            adNetwork = networkName,
            country = country,
            isIranIp = isIran,
            triggerLocation = "free_match_entry"
        )

        if (activity != null) {
            if (isIran) {
                // Iran IP: Strictly Adivery ads
                analyticsManager.logAdShowing(
                    adType = "rewarded",
                    adNetwork = "adivery",
                    country = country,
                    isIranIp = true,
                    triggerLocation = "free_match_entry"
                )
                adiveryManager.loadAndShowRewardedAd(
                    activity = activity,
                    onRewardEarned = {
                        scope.launch {
                            analyticsManager.logRewardedAdWatchedForFreeEntry(
                                adNetwork = "adivery",
                                country = country,
                                isIranIp = true,
                                triggerLocation = "offline_entry"
                            )
                            val successMsg = "🎉 Free Entry Unlocked via Persian Ad! Starting Match..."
                            _rewardToast.value = successMsg
                            onSuccess()
                        }
                    },
                    onAdDismissed = {
                        analyticsManager.logAdDismissed(
                            adType = "rewarded",
                            adNetwork = "adivery",
                            country = country,
                            isIranIp = true
                        )
                        onClosed()
                    },
                    onFailed = { adiveryError ->
                        Log.w("AdManager", "Adivery free entry ad failed: $adiveryError")
                        analyticsManager.logAdFailed(
                            adType = "rewarded",
                            adNetwork = "adivery",
                            country = country,
                            isIranIp = true,
                            errorMessage = adiveryError,
                            triggerLocation = "free_match_entry"
                        )
                        scope.launch {
                            val errorMsg = "Unable to load ad: $adiveryError"
                            _rewardToast.value = errorMsg
                            onError?.invoke(errorMsg)
                            onClosed()
                        }
                    }
                )
            } else {
                // Non-Iran IP (All other countries): Strictly Google AdMob ads
                analyticsManager.logAdShowing(
                    adType = "rewarded",
                    adNetwork = "admob",
                    country = country,
                    isIranIp = false,
                    triggerLocation = "free_match_entry"
                )
                adMobManager.loadAndShowRewardedAd(
                    activity = activity,
                    onRewardEarned = {
                        scope.launch {
                            analyticsManager.logRewardedAdWatchedForFreeEntry(
                                adNetwork = "admob",
                                country = country,
                                isIranIp = false,
                                triggerLocation = "offline_entry"
                            )
                            val successMsg = "🎉 Free Entry Unlocked! Starting Match..."
                            _rewardToast.value = successMsg
                            onSuccess()
                        }
                    },
                    onAdDismissed = {
                        analyticsManager.logAdDismissed(
                            adType = "rewarded",
                            adNetwork = "admob",
                            country = country,
                            isIranIp = false
                        )
                        onClosed()
                    },
                    onFailed = { errorMsg ->
                        Log.w("AdManager", "AdMob free entry failed for Global user ($country): $errorMsg")
                        analyticsManager.logAdFailed(
                            adType = "rewarded",
                            adNetwork = "admob",
                            country = country,
                            isIranIp = false,
                            errorMessage = errorMsg,
                            triggerLocation = "free_match_entry"
                        )
                        scope.launch {
                            val msg = "Ad failed to load: $errorMsg"
                            _rewardToast.value = msg
                            onError?.invoke(msg)
                            onClosed()
                        }
                    }
                )
            }
            return
        }

        // Fallback simulated ad overlay if activity is null
        scope.launch {
            _isAdPlaying.value = true
            _currentAdType.value = AdType.REWARDED
            _adCountdown.value = 5

            for (i in 5 downTo 1) {
                _adCountdown.value = i
                delay(1000)
            }

            _isAdPlaying.value = false
            _currentAdType.value = null

            analyticsManager.logRewardedAdWatchedForFreeEntry(
                adNetwork = "simulated",
                country = country,
                isIranIp = isIran,
                triggerLocation = "offline_entry_simulated"
            )

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
        val count = _completedMatchCount.value
        val shouldShow = count > 0 && count % 2 == 0
        if (!shouldShow) {
            onAdClosed()
            return
        }

        if (_isAdPlaying.value) {
            onAdClosed()
            return
        }

        val isIran = geoLocationDetector.isIranUser.value
        val country = geoLocationDetector.getCountryCode()
        val network = if (isIran) "adivery" else "admob"

        analyticsManager.logAdRequested(
            adType = "interstitial",
            adNetwork = network,
            country = country,
            isIranIp = isIran,
            triggerLocation = "post_match"
        )

        if (activity != null) {
            if (isIran) {
                // Iran IP: Strictly Adivery ads
                analyticsManager.logAdShowing(
                    adType = "interstitial",
                    adNetwork = "adivery",
                    country = country,
                    isIranIp = true,
                    triggerLocation = "post_match"
                )
                adiveryManager.loadAndShowInterstitialAd(
                    activity = activity,
                    onAdDismissed = {
                        analyticsManager.logInterstitialAdShownAfterGame(
                            adNetwork = "adivery",
                            country = country,
                            isIranIp = true,
                            completedMatchCount = count
                        )
                        analyticsManager.logAdDismissed(
                            adType = "interstitial",
                            adNetwork = "adivery",
                            country = country,
                            isIranIp = true
                        )
                        onAdClosed()
                    },
                    onFailed = {
                        analyticsManager.logAdFailed(
                            adType = "interstitial",
                            adNetwork = "adivery",
                            country = country,
                            isIranIp = true,
                            errorMessage = "Adivery interstitial skipped",
                            triggerLocation = "post_match"
                        )
                        onAdClosed()
                    }
                )
            } else {
                // Non-Iran IP (All other countries): Strictly Google AdMob ads
                analyticsManager.logAdShowing(
                    adType = "interstitial",
                    adNetwork = "admob",
                    country = country,
                    isIranIp = false,
                    triggerLocation = "post_match"
                )
                adMobManager.loadAndShowInterstitialAd(
                    activity = activity,
                    onAdDismissed = {
                        analyticsManager.logInterstitialAdShownAfterGame(
                            adNetwork = "admob",
                            country = country,
                            isIranIp = false,
                            completedMatchCount = count
                        )
                        analyticsManager.logAdDismissed(
                            adType = "interstitial",
                            adNetwork = "admob",
                            country = country,
                            isIranIp = false
                        )
                        onAdClosed()
                    },
                    onFailed = {
                        analyticsManager.logAdFailed(
                            adType = "interstitial",
                            adNetwork = "admob",
                            country = country,
                            isIranIp = false,
                            errorMessage = "AdMob interstitial skipped",
                            triggerLocation = "post_match"
                        )
                        onAdClosed()
                    }
                )
            }
            return
        }

        scope.launch {
            _isAdPlaying.value = true
            _currentAdType.value = AdType.INTERSTITIAL
            _adCountdown.value = 3

            for (i in 3 downTo 1) {
                _adCountdown.value = i
                delay(1000)
            }

            _isAdPlaying.value = false
            _currentAdType.value = null

            analyticsManager.logInterstitialAdShownAfterGame(
                adNetwork = "simulated",
                country = country,
                isIranIp = isIran,
                completedMatchCount = count
            )

            onAdClosed()

            preloadInterstitialAd()
        }
    }

    fun clearRewardToast() {
        _rewardToast.value = null
    }
}

