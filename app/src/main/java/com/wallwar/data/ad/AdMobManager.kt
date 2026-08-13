package com.wallwar.data.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    private val rewardedRetryAttempt = AtomicInteger(0)
    private val interstitialRetryAttempt = AtomicInteger(0)

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

    init {
        loadRewardedAd(context)
        loadInterstitialAd(context)
    }

    fun loadRewardedAd(context: Context) {
        if (_isRewardedAdLoading.value || rewardedAd != null) {
            Log.d("AdMobManager", "Skipping loadRewardedAd - loading: ${_isRewardedAdLoading.value}, adExists: ${rewardedAd != null}")
            return
        }
        _isRewardedAdLoading.value = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdMobConstants.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("AdMobManager", "Rewarded Ad Loaded successfully.")
                    rewardedAd = ad
                    rewardedRetryAttempt.set(0)
                    _isRewardedAdReady.value = true
                    _isRewardedAdLoading.value = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMobManager", "Rewarded Ad Failed to Load (${loadAdError.code}): ${loadAdError.message}")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    _isRewardedAdLoading.value = false

                    // Exponential Backoff Retry Policy
                    val attempt = rewardedRetryAttempt.getAndIncrement()
                    val delaySeconds = (2.0.pow(attempt.toDouble()).toLong() * 5L).coerceIn(5L, 30L)
                    Log.d("AdMobManager", "Scheduling Rewarded Ad retry attempt #$attempt in ${delaySeconds}s...")
                    scope.launch {
                        delay(delaySeconds * 1000L)
                        loadRewardedAd(context)
                    }
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null && !_isAdShowing.value) {
            _isAdShowing.value = true
            var rewardGranted = false

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdMobManager", "Rewarded Ad Dismissed. Resetting and auto-reloading next ad...")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    _isAdShowing.value = false
                    rewardedRetryAttempt.set(0)
                    if (rewardGranted) {
                        onRewardEarned()
                    }
                    onAdDismissed()
                    loadRewardedAd(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("AdMobManager", "Rewarded Ad Failed to Show: ${adError.message}")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    _isAdShowing.value = false
                    rewardedRetryAttempt.set(0)
                    onAdDismissed()
                    loadRewardedAd(activity.applicationContext)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("AdMobManager", "Rewarded Ad Showing.")
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d("AdMobManager", "User Earned Reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardGranted = true
            }
        } else {
            Log.w("AdMobManager", "Rewarded Ad is not ready or currently showing. Invoking fallback reward flow.")
            onRewardEarned()
            onAdDismissed()
            loadRewardedAd(activity.applicationContext)
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (_isInterstitialLoading.value || interstitialAd != null) {
            Log.d("AdMobManager", "Skipping loadInterstitialAd - loading: ${_isInterstitialLoading.value}, adExists: ${interstitialAd != null}")
            return
        }
        _isInterstitialLoading.value = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdMobConstants.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdMobManager", "Interstitial Ad Loaded successfully.")
                    interstitialAd = ad
                    interstitialRetryAttempt.set(0)
                    _isInterstitialAdReady.value = true
                    _isInterstitialLoading.value = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMobManager", "Interstitial Ad Failed to Load (${loadAdError.code}): ${loadAdError.message}")
                    interstitialAd = null
                    _isInterstitialAdReady.value = false
                    _isInterstitialLoading.value = false

                    // Exponential Backoff Retry Policy
                    val attempt = interstitialRetryAttempt.getAndIncrement()
                    val delaySeconds = (2.0.pow(attempt.toDouble()).toLong() * 5L).coerceIn(5L, 30L)
                    Log.d("AdMobManager", "Scheduling Interstitial Ad retry attempt #$attempt in ${delaySeconds}s...")
                    scope.launch {
                        delay(delaySeconds * 1000L)
                        loadInterstitialAd(context)
                    }
                }
            }
        )
    }

    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        val ad = interstitialAd
        if (ad != null && !_isAdShowing.value) {
            _isAdShowing.value = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdMobManager", "Interstitial Ad Dismissed. Resetting and auto-reloading next ad...")
                    interstitialAd = null
                    _isInterstitialAdReady.value = false
                    _isAdShowing.value = false
                    interstitialRetryAttempt.set(0)
                    onAdDismissed()
                    loadInterstitialAd(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e("AdMobManager", "Interstitial Ad Failed to Show: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialAdReady.value = false
                    _isAdShowing.value = false
                    interstitialRetryAttempt.set(0)
                    onAdDismissed()
                    loadInterstitialAd(activity.applicationContext)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("AdMobManager", "Interstitial Ad Showing.")
                }
            }
            ad.show(activity)
        } else {
            Log.w("AdMobManager", "Interstitial Ad not ready or showing. Gracefully skipping.")
            onAdDismissed()
            loadInterstitialAd(activity.applicationContext)
        }
    }
}
