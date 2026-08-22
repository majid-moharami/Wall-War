package com.wallwar.ui.screens.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.UserProfile
import com.wallwar.data.ad.AdManager
import com.wallwar.data.billing.BillingConstants
import com.wallwar.data.billing.BillingManager
import com.wallwar.data.billing.BillingPurchaseResult
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoinPack(
    val id: String,
    val productId: String = id,
    val nameEn: String,
    val coins: Int,
    val priceUsd: String,
    val popularTag: String? = null
)

@HiltViewModel
class CoinShopViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val nakamaRepository: NakamaRepository,
    private val adManager: AdManager,
    private val billingManager: BillingManager,
    val soundManager: SoundManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    val isRewardedAdLoading: StateFlow<Boolean> = adManager.isRewardedAdLoading
    val isRewardedAdReady: StateFlow<Boolean> = adManager.isRewardedAdReady
    val isAdPlaying: StateFlow<Boolean> = adManager.isAdPlaying
    val rewardToast: StateFlow<String?> = adManager.rewardToast

    val isPurchasing: StateFlow<Boolean> = billingManager.isPurchasing

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    private val defaultCoinPacks = listOf(
        CoinPack(
            id = "micro",
            productId = BillingConstants.COINS_PACK_100,
            nameEn = "Micro Pack",
            coins = 100,
            priceUsd = "$0.99"
        ),
        CoinPack(
            id = "starter",
            productId = BillingConstants.COINS_PACK_300,
            nameEn = "Starter Pack",
            coins = 300,
            priceUsd = "$2.49"
        ),
        CoinPack(
            id = "gamer",
            productId = BillingConstants.COINS_PACK_600,
            nameEn = "Gamer Pack",
            coins = 600,
            priceUsd = "$4.99"
        ),
        CoinPack(
            id = "pro",
            productId = BillingConstants.COINS_PACK_1300,
            nameEn = "Pro Pack",
            coins = 1300,
            priceUsd = "$8.99",
            popularTag = "POPULAR"
        ),
        CoinPack(
            id = "master",
            productId = BillingConstants.COINS_PACK_3000,
            nameEn = "Master Pack",
            coins = 3000,
            priceUsd = "$17.99",
            popularTag = "GREAT VALUE"
        ),
        CoinPack(
            id = "champion",
            productId = BillingConstants.COINS_PACK_7500,
            nameEn = "Champion Vault",
            coins = 7500,
            priceUsd = "$39.99",
            popularTag = "BEST VALUE"
        )
    )

    private val _coinPacks = MutableStateFlow<List<CoinPack>>(defaultCoinPacks)
    val coinPacks: StateFlow<List<CoinPack>> = _coinPacks.asStateFlow()

    init {
        loadShopPackagesFromNakama()
        observeBillingUpdates()
    }

    private fun observeBillingUpdates() {
        viewModelScope.launch {
            billingManager.purchaseResult.collect { result ->
                when (result) {
                    is BillingPurchaseResult.Success -> {
                        soundManager.playCoinSound()
                        _purchaseMessage.value = "🎉 +${result.coinsAwarded} Coins added! Transaction synced with Nakama Server."
                    }
                    is BillingPurchaseResult.Purchasing -> {
                        // Handled by isPurchasing flow
                    }
                    is BillingPurchaseResult.Pending -> {
                        _purchaseMessage.value = "⏳ In-app purchase is pending Google Play confirmation."
                    }
                    is BillingPurchaseResult.Cancelled -> {
                        _purchaseMessage.value = "Google Play purchase cancelled."
                    }
                    is BillingPurchaseResult.Error -> {
                        _purchaseMessage.value = "❌ Purchase error: ${result.message}"
                    }
                    BillingPurchaseResult.Idle -> Unit
                }
            }
        }

        viewModelScope.launch {
            billingManager.productDetailsMap.collect { detailsMap ->
                if (detailsMap.isNotEmpty()) {
                    val updatedList = _coinPacks.value.map { pack ->
                        val canonical = BillingConstants.getCanonicalProductId(pack.productId)
                        val playDetails = detailsMap[canonical]
                        val livePrice = playDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                        if (livePrice != null) {
                            pack.copy(priceUsd = livePrice)
                        } else {
                            pack
                        }
                    }
                    _coinPacks.value = updatedList
                }
            }
        }
    }

    fun loadShopPackagesFromNakama() {
        viewModelScope.launch {
            val remotePacks = nakamaRepository.fetchShopPacksFromNakama()
            if (!remotePacks.isNullOrEmpty()) {
                _coinPacks.value = remotePacks.map { p ->
                    val canonical = BillingConstants.getCanonicalProductId(p.id)
                    p.copy(productId = canonical)
                }
            }
        }
    }

    fun buyCoinPack(activity: Activity?, pack: CoinPack) {
        val canonicalId = BillingConstants.getCanonicalProductId(pack.productId.ifBlank { pack.id })
        
        if (activity != null) {
            val launched = billingManager.launchBillingFlow(activity, canonicalId)
            if (!launched) {
                if (!billingManager.isConnected.value) {
                    _purchaseMessage.value = "⚠️ Google Play Store is connecting. Please ensure Google Play Services is available."
                } else if (!billingManager.productDetailsMap.value.containsKey(canonicalId)) {
                    _purchaseMessage.value = "⚠️ Google Play In-App Product ($canonicalId) is not yet published or active in Google Play Console."
                }
            }
        } else {
            _purchaseMessage.value = "⚠️ Unable to launch Google Play purchase sheet: Activity not found."
        }
    }

    fun watchRewardedAdForCoins(activity: Activity? = null) {
        adManager.watchRewardedAd(
            activity = activity,
            rewardCoins = 50,
            onSuccess = { coins ->
                soundManager.playCoinSound()
                _purchaseMessage.value = "🎉 +$coins Coins Earned from Watching Rewarded Ad!"
            }
        )
    }

    fun clearPurchaseMessage() {
        _purchaseMessage.value = null
        adManager.clearRewardToast()
    }
}
