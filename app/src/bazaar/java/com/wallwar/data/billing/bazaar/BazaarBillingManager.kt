package com.wallwar.data.billing.bazaar

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
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.entity.SkuDetails
import ir.cafebazaar.poolakey.request.PurchaseRequest
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
class BazaarBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val analyticsManager: AnalyticsManager,
    private val soundManager: SoundManager
) : StoreBillingProvider {

    override val storeType: StoreBillingType = StoreBillingType.CAFE_BAZAAR

    private val tag = "BazaarBillingManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_bazaar_billing_prefs", Context.MODE_PRIVATE)

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    override val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _skuDetailsMap = MutableStateFlow<Map<String, SkuDetails>>(emptyMap())
    val skuDetailsMap: StateFlow<Map<String, SkuDetails>> = _skuDetailsMap.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    override val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    private val _purchaseResult = MutableSharedFlow<BillingPurchaseResult>(replay = 0)
    override val purchaseResult: SharedFlow<BillingPurchaseResult> = _purchaseResult.asSharedFlow()

    private var payment: Payment? = null
    private var paymentConnection: Connection? = null

    init {
        initPayment()
    }

    private fun initPayment() {
        try {
            val paymentConfig = PaymentConfiguration(
                localSecurityCheck = SecurityCheck.Disable
            )
            payment = Payment(context = context, config = paymentConfig)
        } catch (e: Throwable) {
            Log.e(tag, "Failed to initialize Poolakey Payment: ${e.message}", e)
        }
    }

    override fun startConnection() {
        val p = payment ?: run {
            initPayment()
            payment ?: return
        }

        try {
            paymentConnection = p.connect {
                connectionSucceed {
                    Log.i(tag, "Connected to Cafe Bazaar In-App Billing successfully.")
                    _isConnected.value = true
                    queryProductDetails()
                    queryPurchases()
                }
                connectionFailed { throwable ->
                    Log.w(tag, "Failed to connect to Cafe Bazaar billing: ${throwable.message}")
                    _isConnected.value = false
                }
                disconnected {
                    Log.w(tag, "Cafe Bazaar billing service disconnected.")
                    _isConnected.value = false
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Exception during Poolakey connect: ${e.message}", e)
            _isConnected.value = false
        }
    }

    override fun queryProductDetails() {
        val p = payment ?: return
        if (!_isConnected.value) {
            startConnection()
            return
        }

        try {
            p.getInAppSkuDetails(skuIds = BillingConstants.ALL_IN_APP_PRODUCT_IDS) {
                getSkuDetailsSucceed { skuList ->
                    Log.i(tag, "Queried ${skuList.size} SKUs from Cafe Bazaar.")
                    val map = skuList.associateBy { it.sku }
                    _skuDetailsMap.value = map
                    _productPrices.value = skuList.associate { it.sku to it.price }
                }
                getSkuDetailsFailed { throwable ->
                    Log.w(tag, "Failed to query Cafe Bazaar SKU details: ${throwable.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Exception querying SKU details from Cafe Bazaar: ${e.message}", e)
        }
    }

    override fun launchBillingFlow(activity: Activity, productId: String): Boolean {
        val canonicalId = BillingConstants.getCanonicalProductId(productId)
        val expectedCoins = BillingConstants.getCoinsForProductId(canonicalId)
        val p = payment ?: return false

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
            val purchaseRequest = PurchaseRequest(
                productId = canonicalId,
                payload = "bazaar_payload_${System.currentTimeMillis()}"
            )

            p.purchaseProduct(
                registry = (activity as? androidx.activity.ComponentActivity)?.activityResultRegistry
                    ?: run {
                        Log.e(tag, "Activity must be a ComponentActivity for Poolakey purchase")
                        _isPurchasing.value = false
                        return false
                    },
                request = purchaseRequest
            ) {
                purchaseFlowBegan {
                    Log.d(tag, "Cafe Bazaar purchase flow began for $canonicalId")
                }
                failedToBeginFlow { throwable ->
                    Log.e(tag, "Failed to begin Bazaar purchase flow: ${throwable.message}")
                    _isPurchasing.value = false
                    val errorMsg = throwable.message ?: "Could not open Cafe Bazaar purchase sheet"
                    analyticsManager.logPurchaseFailed(canonicalId, null, errorMsg)
                    scope.launch {
                        _purchaseResult.emit(BillingPurchaseResult.Error(errorMsg))
                    }
                }
                purchaseSucceed { purchaseInfo ->
                    _isPurchasing.value = false
                    Log.i(tag, "Cafe Bazaar purchase succeeded: ${purchaseInfo.purchaseToken}")
                    consumePurchase(purchaseInfo)
                }
                purchaseCanceled {
                    _isPurchasing.value = false
                    Log.d(tag, "Cafe Bazaar purchase cancelled by user")
                    analyticsManager.logPurchaseCancelled(canonicalId)
                    scope.launch {
                        _purchaseResult.emit(BillingPurchaseResult.Cancelled)
                    }
                }
                purchaseFailed { throwable ->
                    _isPurchasing.value = false
                    val errorMsg = throwable.message ?: "Cafe Bazaar purchase failed"
                    Log.e(tag, "Cafe Bazaar purchase failed: $errorMsg")
                    analyticsManager.logPurchaseFailed(canonicalId, null, errorMsg)
                    scope.launch {
                        _purchaseResult.emit(BillingPurchaseResult.Error(errorMsg))
                    }
                }
            }
            return true
        } catch (e: Throwable) {
            _isPurchasing.value = false
            Log.e(tag, "Exception launching Bazaar billing flow: ${e.message}", e)
            val errorMsg = e.message ?: "Exception launching Cafe Bazaar payment"
            analyticsManager.logPurchaseFailed(canonicalId, null, errorMsg)
            scope.launch {
                _purchaseResult.emit(BillingPurchaseResult.Error(errorMsg))
            }
            return false
        }
    }

    private fun consumePurchase(purchaseInfo: PurchaseInfo) {
        val p = payment ?: return
        val canonicalId = BillingConstants.getCanonicalProductId(purchaseInfo.productId)
        val coinAmount = BillingConstants.getCoinsForProductId(canonicalId)
        val orderId = purchaseInfo.orderId ?: "BAZAAR_${System.currentTimeMillis()}"
        val token = purchaseInfo.purchaseToken

        try {
            p.consumeProduct(token) {
                consumeSucceed {
                    Log.i(tag, "Cafe Bazaar purchase consumed successfully: $token")
                    val alreadyCredited = prefs.getBoolean("credited_$token", false)
                    if (!alreadyCredited) {
                        prefs.edit().putBoolean("credited_$token", true).apply()

                        authRepository.processGooglePlayCoinPurchase(
                            productId = canonicalId,
                            amount = coinAmount,
                            purchaseToken = token,
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
                }
                consumeFailed { throwable ->
                    val errorMsg = throwable.message ?: "Failed to consume Cafe Bazaar item"
                    Log.e(tag, "Failed to consume purchase in Cafe Bazaar: $errorMsg")
                    analyticsManager.logPurchaseFailed(canonicalId, null, errorMsg)
                    scope.launch {
                        _purchaseResult.emit(BillingPurchaseResult.Error(errorMsg))
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Exception during Bazaar consumeProduct: ${e.message}", e)
        }
    }

    private fun queryPurchases() {
        val p = payment ?: return
        try {
            p.getPurchasedProducts {
                querySucceed { purchases ->
                    Log.i(tag, "Found ${purchases.size} unconsumed purchases in Cafe Bazaar.")
                    for (purchase in purchases) {
                        consumePurchase(purchase)
                    }
                }
                queryFailed { throwable ->
                    Log.w(tag, "Failed to query purchased products in Cafe Bazaar: ${throwable.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(tag, "Exception during getPurchasedProducts: ${e.message}", e)
        }
    }

    override fun endConnection() {
        try {
            paymentConnection?.disconnect()
            paymentConnection = null
            _isConnected.value = false
        } catch (e: Throwable) {
            Log.e(tag, "Exception during disconnect: ${e.message}", e)
        }
    }
}
