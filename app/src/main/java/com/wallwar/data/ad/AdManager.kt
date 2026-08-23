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
    private val geoLocationDetector: GeoLocationDetector
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

        if (activity != null) {
            val useAdivery = geoLocationDetector.isIranUser.value
            Log.d("AdManager", "Serving Rewarded Ad: useAdivery=$useAdivery")

            if (useAdivery) {
                adiveryManager.loadAndShowRewardedAd(
                    activity = activity,
                    onRewardEarned = {
                        scope.launch {
                            nakamaRepository.rpcProcessCoinTransaction(rewardCoins, "rewarded_ad_adivery")
                            authRepository.addCoins(rewardCoins)
                            val successMsg = "🎉 +$rewardCoins Coins Earned from Watching Persian Ad (Adivery)!"
                            _rewardToast.value = successMsg
                            onSuccess(rewardCoins)
                        }
                    },
                    onAdDismissed = {
                        onClosed()
                    },
                    onFailed = { adiveryError ->
                        Log.w("AdManager", "Adivery failed, falling back to AdMob: $adiveryError")
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
                            onFailed = { admobError ->
                                scope.launch {
                                    val finalError = "Ad could not be loaded ($adiveryError / $admobError)"
                                    _rewardToast.value = finalError
                                    onError?.invoke(finalError)
                                    onClosed()
                                }
                            }
                        )
                    }
                )
            } else {
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
                        Log.w("AdManager", "AdMob failed, falling back to Adivery: $errorMsg")
                        adiveryManager.loadAndShowRewardedAd(
                            activity = activity,
                            onRewardEarned = {
                                scope.launch {
                                    nakamaRepository.rpcProcessCoinTransaction(rewardCoins, "rewarded_ad_adivery")
                                    authRepository.addCoins(rewardCoins)
                                    val successMsg = "🎉 +$rewardCoins Coins Earned from Watching Persian Ad!"
                                    _rewardToast.value = successMsg
                                    onSuccess(rewardCoins)
                                }
                            },
                            onAdDismissed = {
                                onClosed()
                            },
                            onFailed = { secondaryError ->
                                scope.launch {
                                    val msg = "Ad failed to load ($errorMsg)"
                                    _rewardToast.value = msg
                                    onError?.invoke(msg)
                                    onClosed()
                                }
                            }
                        )
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
            val useAdivery = geoLocationDetector.isIranUser.value

            if (useAdivery) {
                adiveryManager.loadAndShowRewardedAd(
                    activity = activity,
                    onRewardEarned = {
                        scope.launch {
                            val successMsg = "🎉 Free Entry Unlocked via Persian Ad! Starting Match..."
                            _rewardToast.value = successMsg
                            onSuccess()
                        }
                    },
                    onAdDismissed = {
                        onClosed()
                    },
                    onFailed = { adiveryError ->
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
                            onFailed = { admobError ->
                                scope.launch {
                                    val errorMsg = "Unable to load ad: $admobError"
                                    _rewardToast.value = errorMsg
                                    onError?.invoke(errorMsg)
                                    onClosed()
                                }
                            }
                        )
                    }
                )
            } else {
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
                        adiveryManager.loadAndShowRewardedAd(
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
                            onFailed = {
                                scope.launch {
                                    _rewardToast.value = errorMsg
                                    onError?.invoke(errorMsg)
                                    onClosed()
                                }
                            }
                        )
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
            val useAdivery = geoLocationDetector.isIranUser.value
            if (useAdivery) {
                adiveryManager.loadAndShowInterstitialAd(
                    activity = activity,
                    onAdDismissed = onAdClosed,
                    onFailed = {
                        adMobManager.loadAndShowInterstitialAd(
                            activity = activity,
                            onAdDismissed = onAdClosed,
                            onFailed = { onAdClosed() }
                        )
                    }
                )
            } else {
                adMobManager.loadAndShowInterstitialAd(
                    activity = activity,
                    onAdDismissed = onAdClosed,
                    onFailed = {
                        adiveryManager.loadAndShowInterstitialAd(
                            activity = activity,
                            onAdDismissed = onAdClosed,
                            onFailed = { onAdClosed() }
                        )
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

            onAdClosed()

            preloadInterstitialAd()
        }
    }

    fun clearRewardToast() {
        _rewardToast.value = null
    }
}
