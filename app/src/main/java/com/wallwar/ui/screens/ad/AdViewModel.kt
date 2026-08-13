package com.wallwar.ui.screens.ad

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.wallwar.data.ad.AdManager
import com.wallwar.data.ad.AdNetwork
import com.wallwar.data.ad.AdType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AdViewModel @Inject constructor(
    private val adManager: AdManager
) : ViewModel() {

    val isRewardedAdLoading: StateFlow<Boolean> = adManager.isRewardedAdLoading
    val isRewardedAdReady: StateFlow<Boolean> = adManager.isRewardedAdReady
    val isInterstitialLoading: StateFlow<Boolean> = adManager.isInterstitialLoading
    val isInterstitialReady: StateFlow<Boolean> = adManager.isInterstitialReady
    val isAdPlaying: StateFlow<Boolean> = adManager.isAdPlaying
    val activeNetwork: StateFlow<AdNetwork> = adManager.activeNetwork
    val currentAdType: StateFlow<AdType?> = adManager.currentAdType
    val adCountdown: StateFlow<Int> = adManager.adCountdown
    val completedMatchCount: StateFlow<Int> = adManager.completedMatchCount
    val rewardToast: StateFlow<String?> = adManager.rewardToast

    fun watchRewardedAd(activity: Activity? = null, rewardCoins: Int = 50, onSuccess: (Int) -> Unit = {}) {
        adManager.watchRewardedAd(activity, rewardCoins, onSuccess)
    }

    fun showInterstitialIfTriggered(activity: Activity? = null, onClosed: () -> Unit) {
        adManager.showInterstitialIfTriggered(activity, onClosed)
    }

    fun recordMatchCompleted(): Boolean {
        return adManager.recordMatchCompleted()
    }

    fun clearRewardToast() {
        adManager.clearRewardToast()
    }
}
