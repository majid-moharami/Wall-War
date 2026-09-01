package com.wallwar.data.billing.myket

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.wallwar.analytics.AnalyticsManager
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.billing.BillingConstants
import com.wallwar.data.billing.BillingPurchaseResult
import com.wallwar.data.billing.StoreBillingProvider
import com.wallwar.data.billing.StoreBillingType
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.IabResult
import ir.myket.billingclient.util.Inventory
import ir.myket.billingclient.util.Purchase
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

@Singleton
class MyketBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val analyticsManager: AnalyticsManager,
    private val soundManager: SoundManager
) : StoreBillingProvider {

    override val storeType: StoreBillingType = StoreBillingType.MYKET

    private val tag = "MyketBillingManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_myket_billing_prefs", Context.MODE_PRIVATE)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    override val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    override val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    private val _purchaseResult = MutableSharedFlow<BillingPurchaseResult>(replay = 0)
    override val purchaseResult: SharedFlow<BillingPurchaseResult> = _purchaseResult.asSharedFlow()

    private var iabHelper: IabHelper? = null

    init {
        startConnection()
    }

    override fun startConnection() {
        if (_isConnected.value) return

        try {
            iabHelper?.dispose()
            val helper = IabHelper(context, null)
            helper.enableDebugLogging(true, tag)
            iabHelper = helper

            helper.startSetup { result: IabResult ->
                if (result.isSuccess) {
                    Log.i(tag, "Myket Billing 2.0 setup successful")
                    _isConnected.value = true
                    queryProductDetails()
                } else {
                    Log.w(tag, "Myket Billing 2.0 setup failed: ${result.message}")
                    _isConnected.value = false
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Exception during Myket Billing startConnection: ${e.message}", e)
            _isConnected.value = false
        }
    }

    override fun queryProductDetails() {
        val helper = iabHelper ?: return
        if (!_isConnected.value) {
            startConnection()
            return
        }

        try {
            val skus = BillingConstants.ALL_IN_APP_PRODUCT_IDS
            helper.queryInventoryAsync(true, skus) { result: IabResult, inventory: Inventory? ->
                if (result.isSuccess && inventory != null) {
                    val prices = mutableMapOf<String, String>()
                    for (sku in skus) {
                        val details = inventory.getSkuDetails(sku)
                        if (details != null && details.price.isNotEmpty()) {
                            prices[sku] = details.price
                        }
                    }
                    _productPrices.value = prices
                    Log.i(tag, "Queried ${prices.size} SKUs from Myket 2.0.")

                    // Check for unconsumed purchases
                    val purchases = inventory.allPurchases
                    if (purchases != null) {
                        for (purchase in purchases) {
                            if (purchase != null && purchase.token.isNotEmpty()) {
                                consumePurchase(purchase)
                            }
                        }
                    }
                } else {
                    Log.w(tag, "Myket 2.0 queryInventoryAsync failed: ${result.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Exception querying Myket inventory: ${e.message}", e)
        }
    }

    override fun launchBillingFlow(activity: Activity, productId: String): Boolean {
        val canonicalId = BillingConstants.getCanonicalProductId(productId)
        val expectedCoins = BillingConstants.getCoinsForProductId(canonicalId)
        val helper = iabHelper ?: return false

        if (!_isConnected.value) {
            startConnection()
            return false
        }

        analyticsManager.logPurchaseInitiated(
            productId = canonicalId,
            expectedCoins = expectedCoins,
            priceString = _productPrices.value[canonicalId]
        )

        _isPurchasing.value = true

        scope.launch {
            _purchaseResult.emit(BillingPurchaseResult.Purchasing(canonicalId))
        }

        try {
            val payload = "myket_payload_${System.currentTimeMillis()}"
            helper.launchPurchaseFlow(
                activity,
                canonicalId,
                { result: IabResult, purchase: Purchase? ->
                    _isPurchasing.value = false
                    if (result.isSuccess && purchase != null) {
                        Log.i(tag, "Myket 2.0 purchase successful for ${purchase.sku}")
                        consumePurchase(purchase)
                    } else if (result.response == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED ||
                        result.response == IabHelper.IABHELPER_USER_CANCELLED
                    ) {
                        Log.d(tag, "Myket 2.0 purchase cancelled by user")
                        analyticsManager.logPurchaseCancelled(canonicalId)
                        scope.launch {
                            _purchaseResult.emit(BillingPurchaseResult.Cancelled)
                        }
                    } else if (result.response == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                        Log.i(tag, "Item $canonicalId already owned in Myket, querying to consume...")
                        queryProductDetails()
                    } else {
                        val errorMsg = result.message ?: "Purchase failed (code ${result.response})"
                        Log.e(tag, "Myket 2.0 purchase error: $errorMsg")
                        analyticsManager.logPurchaseFailed(canonicalId, result.response, errorMsg)
                        scope.launch {
                            _purchaseResult.emit(BillingPurchaseResult.Error(errorMsg))
                        }
                    }
                },
                payload
            )
            return true
        } catch (e: Throwable) {
            _isPurchasing.value = false
            val errorMsg = e.message ?: "Exception in Myket launchPurchaseFlow"
            Log.e(tag, "Exception in Myket launchPurchaseFlow: $errorMsg", e)
            analyticsManager.logPurchaseFailed(canonicalId, null, errorMsg)
            scope.launch {
                _purchaseResult.emit(BillingPurchaseResult.Error(errorMsg))
            }
            return false
        }
    }

    private fun consumePurchase(purchase: Purchase) {
        val helper = iabHelper ?: return
        val sku = purchase.sku
        val canonicalId = BillingConstants.getCanonicalProductId(sku)
        val coinAmount = BillingConstants.getCoinsForProductId(canonicalId)
        val purchaseToken = purchase.token
        val orderId = purchase.orderId ?: "MYKET_${System.currentTimeMillis()}"

        try {
            helper.consumeAsync(purchase) { consumedPurchase: Purchase?, result: IabResult ->
                if (result.isSuccess) {
                    Log.i(tag, "Myket 2.0 purchase consumed successfully: $purchaseToken")
                    val alreadyCredited = prefs.getBoolean("credited_$purchaseToken", false)
                    if (!alreadyCredited) {
                        prefs.edit().putBoolean("credited_$purchaseToken", true).apply()

                        authRepository.processGooglePlayCoinPurchase(
                            productId = canonicalId,
                            amount = coinAmount,
                            purchaseToken = purchaseToken,
                            orderId = orderId
                        )

                        analyticsManager.logPurchaseSuccess(
                            productId = canonicalId,
                            coinsAwarded = coinAmount,
                            orderId = orderId,
                            isSandbox = false
                        )

                        soundManager.playCoinSound()
                    }

                    scope.launch {
                        _purchaseResult.emit(
                            BillingPurchaseResult.Success(
                                productId = canonicalId,
                                coinsAwarded = coinAmount,
                                orderId = orderId
                            )
                        )
                    }
                } else {
                    val errorMsg = "Failed to consume Myket purchase: ${result.message}"
                    Log.e(tag, errorMsg)
                    scope.launch {
                        _purchaseResult.emit(BillingPurchaseResult.Error(errorMsg))
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Exception during Myket consumeAsync: ${e.message}", e)
        }
    }

    override fun endConnection() {
        try {
            iabHelper?.dispose()
            iabHelper = null
            _isConnected.value = false
        } catch (e: Throwable) {
            Log.e(tag, "Exception during Myket endConnection: ${e.message}", e)
        }
    }
}
