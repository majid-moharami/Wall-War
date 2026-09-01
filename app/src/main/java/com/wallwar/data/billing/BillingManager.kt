package com.wallwar.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.wallwar.analytics.AnalyticsManager
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class BillingPurchaseResult {
    object Idle : BillingPurchaseResult()
    data class Purchasing(val productId: String) : BillingPurchaseResult()
    data class Success(
        val productId: String,
        val coinsAwarded: Int,
        val orderId: String?
    ) : BillingPurchaseResult()
    data class Pending(val productId: String) : BillingPurchaseResult()
    object Cancelled : BillingPurchaseResult()
    data class Error(val message: String, val responseCode: Int? = null) : BillingPurchaseResult()
}

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val billingProvider: StoreBillingProvider,
    private val analyticsManager: AnalyticsManager,
    private val soundManager: SoundManager
) {
    private val tag = "BillingManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val activeStore: StateFlow<StoreBillingType> = MutableStateFlow(billingProvider.storeType).asStateFlow()
    val isConnected: StateFlow<Boolean> = billingProvider.isConnected
    val isPurchasing: StateFlow<Boolean> = billingProvider.isPurchasing
    val productPrices: StateFlow<Map<String, String>> = billingProvider.productPrices
    val purchaseResult: SharedFlow<BillingPurchaseResult> = billingProvider.purchaseResult

    fun setStoreProvider(storeType: StoreBillingType) {
        Log.i(tag, "Store provider is managed per build flavor: ${billingProvider.storeType.displayName}")
    }

    fun startBillingConnection() {
        billingProvider.startConnection()
    }

    fun queryProductDetails() {
        billingProvider.queryProductDetails()
    }

    fun launchBillingFlow(activity: Activity, productId: String): Boolean {
        return billingProvider.launchBillingFlow(activity, productId)
    }

    fun processFallbackPurchase(productId: String, customCoins: Int? = null) {
        val canonicalId = BillingConstants.getCanonicalProductId(productId)
        val coins = customCoins ?: BillingConstants.getCoinsForProductId(canonicalId)
        val dummyToken = "sandbox_token_${System.currentTimeMillis()}"
        val dummyOrder = "ORDER.TEST-${(1000..9999).random()}-${(10000..99999).random()}"

        analyticsManager.logPurchaseInitiated(
            productId = canonicalId,
            expectedCoins = coins,
            priceString = "Sandbox $0.00"
        )

        authRepository.processGooglePlayCoinPurchase(
            productId = canonicalId,
            amount = coins,
            purchaseToken = dummyToken,
            orderId = dummyOrder
        )

        analyticsManager.logPurchaseSuccess(
            productId = canonicalId,
            coinsAwarded = coins,
            orderId = dummyOrder,
            isSandbox = true
        )

        soundManager.playCoinSound()
    }

    fun endConnection() {
        billingProvider.endConnection()
    }
}

