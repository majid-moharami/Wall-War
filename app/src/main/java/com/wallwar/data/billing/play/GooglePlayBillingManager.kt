package com.wallwar.data.billing.play

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.wallwar.analytics.AnalyticsManager
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.billing.BillingConstants
import com.wallwar.data.billing.BillingPurchaseResult
import com.wallwar.data.billing.StoreBillingProvider
import com.wallwar.data.billing.StoreBillingType
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

@Singleton
class GooglePlayBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val analyticsManager: AnalyticsManager,
    private val soundManager: SoundManager
) : PurchasesUpdatedListener, BillingClientStateListener, StoreBillingProvider {

    override val storeType: StoreBillingType = StoreBillingType.GOOGLE_PLAY

    private val tag = "GooglePlayBilling"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_billing_prefs", Context.MODE_PRIVATE)

    // Billing Client instance
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    // State
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    override val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    private val _purchaseResult = MutableSharedFlow<BillingPurchaseResult>(replay = 0)
    override val purchaseResult: SharedFlow<BillingPurchaseResult> = _purchaseResult.asSharedFlow()

    private val _isPurchasing = MutableStateFlow(false)
    override val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    init {
        startConnection()
    }

    /**
     * Connects to Google Play Billing Service
     */
    override fun startConnection() {
        if (billingClient.isReady) {
            _isConnected.value = true
            return
        }

        try {
            billingClient.startConnection(this)
        } catch (e: Exception) {
            Log.e(tag, "Exception starting billing connection: ${e.message}", e)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.i(tag, "Google Play Billing setup successful.")
            _isConnected.value = true
            reconnectAttempts = 0

            // 1. Query available consumable coin products
            queryProductDetails()

            // 2. Query any unconsumed / pending purchases to ensure user receives coins
            processPendingPurchases()
        } else {
            Log.w(tag, "Billing setup failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            _isConnected.value = false
            retryBillingConnectionWithBackoff()
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(tag, "Billing service disconnected.")
        _isConnected.value = false
        retryBillingConnectionWithBackoff()
    }

    private fun retryBillingConnectionWithBackoff() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            val delayMillis = (1000L * reconnectAttempts).coerceAtMost(10000L)
            mainHandler.postDelayed({
                if (!_isConnected.value) {
                    Log.d(tag, "Retrying billing connection (attempt #$reconnectAttempts)...")
                    startConnection()
                }
            }, delayMillis)
        }
    }

    /**
     * Queries Google Play for the details of our in-app consumable coin packs
     */
    override fun queryProductDetails() {
        if (!billingClient.isReady) {
            Log.d(tag, "queryProductDetails: BillingClient not ready, reconnecting...")
            startConnection()
            return
        }

        val productList = BillingConstants.ALL_IN_APP_PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params, object : ProductDetailsResponseListener {
            override fun onProductDetailsResponse(
                billingResult: BillingResult,
                queryProductDetailsResult: QueryProductDetailsResult
            ) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetailsList = queryProductDetailsResult.productDetailsList ?: emptyList()
                    Log.i(tag, "Queried ${productDetailsList.size} product details from Google Play.")
                    val map = productDetailsList.associateBy { it.productId }
                    _productDetailsMap.value = map
                    _productPrices.value = productDetailsList.mapNotNull { details ->
                        val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                        if (price != null) details.productId to price else null
                    }.toMap()
                } else {
                    Log.w(tag, "Failed to query product details: ${billingResult.debugMessage}")
                }
            }
        })
    }

    /**
     * Launches the Google Play in-app purchase flow
     */
    override fun launchBillingFlow(activity: Activity, productId: String): Boolean {
        val canonicalId = BillingConstants.getCanonicalProductId(productId)
        val expectedCoins = BillingConstants.getCoinsForProductId(canonicalId)

        if (!billingClient.isReady) {
            Log.w(tag, "launchBillingFlow: BillingClient not ready. Reconnecting...")
            startConnection()
            scope.launch {
                _purchaseResult.emit(
                    BillingPurchaseResult.Error("Google Play Store connection initializing. Please try again.")
                )
            }
            return false
        }

        val productDetails = _productDetailsMap.value[canonicalId]
        if (productDetails == null) {
            Log.w(tag, "launchBillingFlow: ProductDetails not found for $canonicalId in Play Store cache.")
            return false
        }

        val priceFormatted = productDetails.oneTimePurchaseOfferDetails?.formattedPrice
        analyticsManager.logPurchaseInitiated(
            productId = canonicalId,
            expectedCoins = expectedCoins,
            priceString = priceFormatted
        )

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _isPurchasing.value = true
        scope.launch {
            _purchaseResult.emit(BillingPurchaseResult.Purchasing(canonicalId))
        }

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(tag, "Failed to launch billing flow: ${billingResult.debugMessage}")
            _isPurchasing.value = false
            val errorMsg = billingResult.debugMessage.ifBlank { "Could not initiate Google Play purchase." }
            analyticsManager.logPurchaseFailed(
                productId = canonicalId,
                responseCode = billingResult.responseCode,
                errorMessage = errorMsg
            )
            scope.launch {
                _purchaseResult.emit(
                    BillingPurchaseResult.Error(
                        errorMsg,
                        billingResult.responseCode
                    )
                )
            }
            return false
        }

        return true
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        _isPurchasing.value = false

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(tag, "Purchase was cancelled by user.")
                val productId = purchases?.firstOrNull()?.products?.firstOrNull() ?: "unknown_product"
                analyticsManager.logPurchaseCancelled(productId)
                scope.launch {
                    _purchaseResult.emit(BillingPurchaseResult.Cancelled)
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(tag, "Item already owned; querying purchases to consume.")
                processPendingPurchases()
            }
            else -> {
                val errorMsg = billingResult.debugMessage.ifBlank { "Google Play Purchase failed (${billingResult.responseCode})" }
                Log.e(tag, "Purchase error: ${billingResult.responseCode} - $errorMsg")
                val productId = purchases?.firstOrNull()?.products?.firstOrNull() ?: "unknown_product"
                analyticsManager.logPurchaseFailed(
                    productId = productId,
                    responseCode = billingResult.responseCode,
                    errorMessage = errorMsg
                )
                scope.launch {
                    _purchaseResult.emit(
                        BillingPurchaseResult.Error(
                            errorMsg,
                            billingResult.responseCode
                        )
                    )
                }
            }
        }
    }

    fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            consumePurchase(purchase)
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.i(tag, "Purchase is pending approval or transaction completion.")
            val productId = purchase.products.firstOrNull() ?: "coin_pack"
            analyticsManager.logPurchasePending(productId)
            scope.launch {
                _purchaseResult.emit(BillingPurchaseResult.Pending(productId))
            }
        }
    }

    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            val productId = purchase.products.firstOrNull() ?: BillingConstants.COINS_PACK_100
            val canonicalId = BillingConstants.getCanonicalProductId(productId)
            val coinAmount = BillingConstants.getCoinsForProductId(canonicalId)
            val orderId = purchase.orderId ?: "ORDER_${System.currentTimeMillis()}"

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(tag, "Purchase successfully consumed: $purchaseToken")

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
                Log.e(tag, "Failed to consume purchase: ${billingResult.debugMessage}")
                analyticsManager.logPurchaseFailed(
                    productId = canonicalId,
                    responseCode = billingResult.responseCode,
                    errorMessage = "Failed to consume: ${billingResult.debugMessage}"
                )
                scope.launch {
                    _purchaseResult.emit(
                        BillingPurchaseResult.Error(
                            "Failed to finalize purchase: ${billingResult.debugMessage}",
                            billingResult.responseCode
                        )
                    )
                }
            }
        }
    }

    fun processPendingPurchases() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(tag, "Found ${purchasesList.size} existing in-app purchases to process.")
                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                    }
                }
            } else {
                Log.w(tag, "Failed to query existing purchases: ${billingResult.debugMessage}")
            }
        }
    }

    override fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
