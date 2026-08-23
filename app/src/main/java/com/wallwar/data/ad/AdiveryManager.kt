package com.wallwar.data.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class AdiveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isRewardedAdLoading = MutableStateFlow(false)
    val isRewardedAdLoading: StateFlow<Boolean> = _isRewardedAdLoading.asStateFlow()

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    private val _isInterstitialLoading = MutableStateFlow(false)
    val isInterstitialLoading: StateFlow<Boolean> = _isInterstitialLoading.asStateFlow()

    private val _isInterstitialAdReady = MutableStateFlow(false)
    val isInterstitialAdReady: StateFlow<Boolean> = _isInterstitialAdReady.asStateFlow()
    val isInterstitialReady: StateFlow<Boolean> get() = isInterstitialAdReady

    private val _isAdShowing = MutableStateFlow(false)
    val isAdShowing: StateFlow<Boolean> = _isAdShowing.asStateFlow()

    private val interstitialRetryAttempt = AtomicInteger(0)

    private var onRewardEarnedCallback: (() -> Unit)? = null
    private var onRewardedDismissedCallback: (() -> Unit)? = null
    private var onRewardedFailedCallback: ((String) -> Unit)? = null

    private var onInterstitialDismissedCallback: (() -> Unit)? = null

    init {
        Adivery.addGlobalListener(object : AdiveryListener() {
            override fun onRewardedAdLoaded(placementId: String) {
                if (placementId == AdiveryConstants.REWARDED_PLACEMENT_ID) {
                    Log.d("AdiveryManager", "Adivery Rewarded Ad Loaded successfully for: $placementId")
                    _isRewardedAdReady.value = true
                    _isRewardedAdLoading.value = false
                }
            }

            override fun onRewardedAdShown(placementId: String) {
                if (placementId == AdiveryConstants.REWARDED_PLACEMENT_ID) {
                    Log.d("AdiveryManager", "Adivery Rewarded Ad Shown: $placementId")
                    _isAdShowing.value = true
                }
            }

            override fun onRewardedAdClosed(placementId: String, isRewarded: Boolean) {
                if (placementId == AdiveryConstants.REWARDED_PLACEMENT_ID) {
                    Log.d("AdiveryManager", "Adivery Rewarded Ad Closed: $placementId, isRewarded=$isRewarded")
                    _isAdShowing.value = false
                    _isRewardedAdReady.value = false
                    _isRewardedAdLoading.value = false
                    if (isRewarded) {
                        onRewardEarnedCallback?.invoke()
                    }
                    onRewardedDismissedCallback?.invoke()
                    onRewardEarnedCallback = null
                    onRewardedDismissedCallback = null
                    onRewardedFailedCallback = null
                    // Pre-prepare next ad
                    prepareRewardedAd()
                }
            }

            override fun onInterstitialAdLoaded(placementId: String) {
                if (placementId == AdiveryConstants.INTERSTITIAL_PLACEMENT_ID) {
                    Log.d("AdiveryManager", "Adivery Interstitial Ad Loaded: $placementId")
                    _isInterstitialAdReady.value = true
                    _isInterstitialLoading.value = false
                    interstitialRetryAttempt.set(0)
                }
            }

            override fun onInterstitialAdShown(placementId: String) {
                if (placementId == AdiveryConstants.INTERSTITIAL_PLACEMENT_ID) {
                    Log.d("AdiveryManager", "Adivery Interstitial Ad Shown: $placementId")
                    _isAdShowing.value = true
                }
            }

            override fun onInterstitialAdClosed(placementId: String) {
                if (placementId == AdiveryConstants.INTERSTITIAL_PLACEMENT_ID) {
                    Log.d("AdiveryManager", "Adivery Interstitial Ad Closed: $placementId")
                    _isAdShowing.value = false
                    _isInterstitialAdReady.value = false
                    _isInterstitialLoading.value = false
                    onInterstitialDismissedCallback?.invoke()
                    onInterstitialDismissedCallback = null
                    // Pre-prepare next ad
                    prepareInterstitialAd()
                }
            }
        })
    }

    fun prepareRewardedAd() {
        if (_isRewardedAdLoading.value || isRewardedLoaded()) return
        _isRewardedAdLoading.value = true
        Adivery.prepareRewardedAd(context, AdiveryConstants.REWARDED_PLACEMENT_ID)
    }

    fun prepareInterstitialAd() {
        if (_isInterstitialLoading.value || isInterstitialLoaded()) return
        _isInterstitialLoading.value = true
        Adivery.prepareInterstitialAd(context, AdiveryConstants.INTERSTITIAL_PLACEMENT_ID)
    }

    fun isRewardedLoaded(): Boolean {
        return Adivery.isLoaded(AdiveryConstants.REWARDED_PLACEMENT_ID)
    }

    fun isInterstitialLoaded(): Boolean {
        return Adivery.isLoaded(AdiveryConstants.INTERSTITIAL_PLACEMENT_ID)
    }

    fun loadAndShowRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit,
        onFailed: (String) -> Unit = {}
    ) {
        if (_isAdShowing.value) {
            Log.w("AdiveryManager", "Adivery Rewarded ad is already showing.")
            return
        }

        this.onRewardEarnedCallback = onRewardEarned
        this.onRewardedDismissedCallback = onAdDismissed
        this.onRewardedFailedCallback = onFailed

        if (isRewardedLoaded()) {
            Log.d("AdiveryManager", "Adivery Rewarded Ad is in memory, showing directly.")
            _isAdShowing.value = true
            Adivery.showAd(AdiveryConstants.REWARDED_PLACEMENT_ID)
        } else {
            Log.d("AdiveryManager", "Adivery Rewarded Ad not loaded yet. Preparing and waiting to show...")
            _isRewardedAdLoading.value = true
            prepareRewardedAd()

            scope.launch {
                // Poll briefly up to 3 seconds for ad to load
                var waitCount = 0
                while (waitCount < 6 && !isRewardedLoaded()) {
                    delay(500)
                    waitCount++
                }

                if (isRewardedLoaded()) {
                    Log.d("AdiveryManager", "Adivery Rewarded Ad ready after wait, showing now.")
                    _isAdShowing.value = true
                    Adivery.showAd(AdiveryConstants.REWARDED_PLACEMENT_ID)
                } else {
                    _isRewardedAdLoading.value = false
                    val errorMsg = "Unable to load Persian ad from Adivery."
                    Log.w("AdiveryManager", errorMsg)
                    onFailed(errorMsg)
                    onAdDismissed()
                    onRewardEarnedCallback = null
                    onRewardedDismissedCallback = null
                    onRewardedFailedCallback = null
                }
            }
        }
    }

    fun loadAndShowInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onFailed: (String) -> Unit = {}
    ) {
        this.onInterstitialDismissedCallback = onAdDismissed
        if (isInterstitialLoaded() && !_isAdShowing.value) {
            Log.d("AdiveryManager", "Showing Adivery Interstitial Ad.")
            _isAdShowing.value = true
            Adivery.showAd(AdiveryConstants.INTERSTITIAL_PLACEMENT_ID)
        } else {
            Log.d("AdiveryManager", "Adivery Interstitial Ad not loaded yet. Preparing...")
            prepareInterstitialAd()
            scope.launch {
                delay(1200)
                if (isInterstitialLoaded() && !_isAdShowing.value) {
                    _isAdShowing.value = true
                    Adivery.showAd(AdiveryConstants.INTERSTITIAL_PLACEMENT_ID)
                } else {
                    onFailed("Adivery interstitial ad skipped.")
                    onAdDismissed()
                }
            }
        }
    }
}
