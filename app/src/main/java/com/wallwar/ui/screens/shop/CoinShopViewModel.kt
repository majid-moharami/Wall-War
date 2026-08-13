package com.wallwar.ui.screens.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.data.AuthRepository
import com.wallwar.data.UserProfile
import com.wallwar.data.ad.AdManager
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoinPack(
    val id: String,
    val nameEn: String,
    val coins: Int,
    val priceUsd: String,
    val popularTag: String? = null
)

@HiltViewModel
class CoinShopViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val nakamaRepository: NakamaRepository,
    private val adManager: AdManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    val isRewardedAdLoading: StateFlow<Boolean> = adManager.isRewardedAdLoading
    val isRewardedAdReady: StateFlow<Boolean> = adManager.isRewardedAdReady
    val isAdPlaying: StateFlow<Boolean> = adManager.isAdPlaying
    val rewardToast: StateFlow<String?> = adManager.rewardToast

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    private val defaultCoinPacks = listOf(
        CoinPack("micro", "Micro Pack", 100, "$0.99"),
        CoinPack("starter", "Starter Pack", 300, "$2.49"),
        CoinPack("gamer", "Gamer Pack", 600, "$4.99"),
        CoinPack("pro", "Pro Pack", 1300, "$8.99", popularTag = "POPULAR"),
        CoinPack("master", "Master Pack", 3000, "$17.99", popularTag = "GREAT VALUE"),
        CoinPack("champion", "Champion Vault", 7500, "$39.99", popularTag = "BEST VALUE")
    )

    private val _coinPacks = MutableStateFlow<List<CoinPack>>(defaultCoinPacks)
    val coinPacks: StateFlow<List<CoinPack>> = _coinPacks.asStateFlow()

    init {
        loadShopPackagesFromNakama()
    }

    fun loadShopPackagesFromNakama() {
        viewModelScope.launch {
            val remotePacks = nakamaRepository.fetchShopPacksFromNakama()
            if (!remotePacks.isNullOrEmpty()) {
                _coinPacks.value = remotePacks
            }
        }
    }

    fun buyCoinPack(pack: CoinPack) {
        authRepository.addCoins(pack.coins)
        _purchaseMessage.value = "Successfully purchased ${pack.nameEn}! +${pack.coins} Coins added."
    }

    fun watchRewardedAdForCoins(activity: Activity? = null) {
        adManager.watchRewardedAd(
            activity = activity,
            rewardCoins = 50,
            onSuccess = { coins ->
                _purchaseMessage.value = "🎉 +$coins Coins Earned from Watching Rewarded Ad!"
            }
        )
    }

    fun clearPurchaseMessage() {
        _purchaseMessage.value = null
        adManager.clearRewardToast()
    }
}
