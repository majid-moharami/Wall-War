package com.wallwar.ui.screens.shop

import androidx.lifecycle.ViewModel
import com.wallwar.data.AuthRepository
import com.wallwar.data.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val authRepository: AuthRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    val coinPacks = listOf(
        CoinPack("micro", "Micro Pack", 100, "$0.99"),
        CoinPack("starter", "Starter Pack", 300, "$2.49"),
        CoinPack("gamer", "Gamer Pack", 600, "$4.99"),
        CoinPack("pro", "Pro Pack", 1300, "$8.99", popularTag = "POPULAR"),
        CoinPack("master", "Master Pack", 3000, "$17.99", popularTag = "GREAT VALUE"),
        CoinPack("champion", "Champion Vault", 7500, "$39.99", popularTag = "BEST VALUE")
    )

    fun buyCoinPack(pack: CoinPack) {
        authRepository.addCoins(pack.coins)
        _purchaseMessage.value = "Successfully purchased ${pack.nameEn}! +${pack.coins} Coins added."
    }

    fun clearPurchaseMessage() {
        _purchaseMessage.value = null
    }
}
