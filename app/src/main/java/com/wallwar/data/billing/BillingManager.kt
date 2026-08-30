package com.wallwar.data.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.wallwar.analytics.AnalyticsManager
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.ad.GeoLocationDetector
import com.wallwar.data.billing.bazaar.BazaarBillingManager
import com.wallwar.data.billing.myket.MyketBillingManager
import com.wallwar.data.billing.play.GooglePlayBillingManager
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
    private val googlePlayBillingManager: GooglePlayBillingManager,
    private val bazaarBillingManager: BazaarBillingManager,
    private val myketBillingManager: MyketBillingManager,
    private val geoLocationDetector: GeoLocationDetector,
    private val analyticsManager: AnalyticsManager,
    private val soundManager: SoundManager
) {
    private val tag = "BillingManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_store_billing_prefs", Context.MODE_PRIVATE)

    // Store billing provider selection
    private val _activeStore = MutableStateFlow(loadSelectedStore())
    val activeStore: StateFlow<StoreBillingType> = _activeStore.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    private val _purchaseResult = MutableSharedFlow<BillingPurchaseResult>(replay = 0)
    val purchaseResult: SharedFlow<BillingPurchaseResult> = _purchaseResult.asSharedFlow()

    val currentProvider: StoreBillingProvider
        get() = when (_activeStore.value) {
            StoreBillingType.CAFE_BAZAAR -> bazaarBillingManager
            StoreBillingType.MYKET -> myketBillingManager
            StoreBillingType.GOOGLE_PLAY -> googlePlayBillingManager
        }

    init {
        observeActiveProvider()
    }

    private fun loadSelectedStore(): StoreBillingType {
        // If build specifically targets a store flavor (MYKET, BAZAAR, PLAY)
        val buildTarget = com.wallwar.BuildConfig.TARGET_STORE
        when (buildTarget.uppercase()) {
            "MYKET" -> {
                Log.i(tag, "Build flavor locked to MYKET billing")
                return StoreBillingType.MYKET
            }
            "BAZAAR", "CAFE_BAZAAR" -> {
                Log.i(tag, "Build flavor locked to CAFE_BAZAAR billing")
                return StoreBillingType.CAFE_BAZAAR
            }
            "PLAY", "GOOGLE_PLAY" -> {
                Log.i(tag, "Build flavor locked to GOOGLE_PLAY billing")
                return StoreBillingType.GOOGLE_PLAY
            }
        }

        val saved = prefs.getString("selected_store_billing", null)
        if (saved != null) {
            try {
                return StoreBillingType.valueOf(saved)
            } catch (e: Exception) {
                // fall through to auto-detection
            }
        }
        return detectInstalledStore(context)
    }

    private fun detectInstalledStore(context: Context): StoreBillingType {
        return try {
            val packageName = context.packageName
            val packageManager = context.packageManager

            val installerPackage: String? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val installSourceInfo = packageManager.getInstallSourceInfo(packageName)
                installSourceInfo.installingPackageName ?: installSourceInfo.initiatingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }

            Log.i(tag, "Detected app installer package: $installerPackage")

            when (installerPackage) {
                "ir.mservices.market" -> StoreBillingType.MYKET
                "com.farsitel.bazaar" -> StoreBillingType.CAFE_BAZAAR
                "com.android.vending" -> StoreBillingType.GOOGLE_PLAY
                else -> {
                    val hasMyket = try {
                        packageManager.getPackageInfo("ir.mservices.market", 0)
                        true
                    } catch (e: Exception) {
                        false
                    }
                    val hasBazaar = try {
                        packageManager.getPackageInfo("com.farsitel.bazaar", 0)
                        true
                    } catch (e: Exception) {
                        false
                    }
                    val hasPlay = try {
                        packageManager.getPackageInfo("com.android.vending", 0)
                        true
                    } catch (e: Exception) {
                        false
                    }

                    if (hasMyket) {
                        StoreBillingType.MYKET
                    } else if (hasBazaar && !hasPlay) {
                        StoreBillingType.CAFE_BAZAAR
                    } else if (geoLocationDetector.isIranUser.value || !hasPlay) {
                        // Myket 2.0 allows payment via Custom Tabs/Web even when Myket app is not installed
                        Log.i(tag, "Selecting Myket 2.0 (Web/App fallback) for Iranian / sideloaded environment")
                        StoreBillingType.MYKET
                    } else {
                        StoreBillingType.GOOGLE_PLAY
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error detecting installer: ${e.message}")
            StoreBillingType.GOOGLE_PLAY
        }
    }

    fun setStoreProvider(storeType: StoreBillingType) {
        if (_activeStore.value != storeType) {
            _activeStore.value = storeType
            prefs.edit().putString("selected_store_billing", storeType.name).apply()
            Log.i(tag, "Switched active store billing provider to: ${storeType.displayName}")
            startBillingConnection()
        }
    }

    private fun observeActiveProvider() {
        scope.launch {
            googlePlayBillingManager.purchaseResult.collect { result ->
                if (_activeStore.value == StoreBillingType.GOOGLE_PLAY) {
                    _purchaseResult.emit(result)
                }
            }
        }

        scope.launch {
            bazaarBillingManager.purchaseResult.collect { result ->
                if (_activeStore.value == StoreBillingType.CAFE_BAZAAR) {
                    _purchaseResult.emit(result)
                }
            }
        }

        scope.launch {
            myketBillingManager.purchaseResult.collect { result ->
                if (_activeStore.value == StoreBillingType.MYKET) {
                    _purchaseResult.emit(result)
                }
            }
        }

        scope.launch {
            _activeStore.collect { store ->
                when (store) {
                    StoreBillingType.CAFE_BAZAAR -> {
                        _isConnected.value = bazaarBillingManager.isConnected.value
                        _isPurchasing.value = bazaarBillingManager.isPurchasing.value
                        _productPrices.value = bazaarBillingManager.productPrices.value
                    }
                    StoreBillingType.MYKET -> {
                        _isConnected.value = myketBillingManager.isConnected.value
                        _isPurchasing.value = myketBillingManager.isPurchasing.value
                        _productPrices.value = myketBillingManager.productPrices.value
                    }
                    StoreBillingType.GOOGLE_PLAY -> {
                        _isConnected.value = googlePlayBillingManager.isConnected.value
                        _isPurchasing.value = googlePlayBillingManager.isPurchasing.value
                        _productPrices.value = googlePlayBillingManager.productPrices.value
                    }
                }
            }
        }

        scope.launch {
            googlePlayBillingManager.isConnected.collect { conn ->
                if (_activeStore.value == StoreBillingType.GOOGLE_PLAY) {
                    _isConnected.value = conn
                }
            }
        }

        scope.launch {
            bazaarBillingManager.isConnected.collect { conn ->
                if (_activeStore.value == StoreBillingType.CAFE_BAZAAR) {
                    _isConnected.value = conn
                }
            }
        }

        scope.launch {
            myketBillingManager.isConnected.collect { conn ->
                if (_activeStore.value == StoreBillingType.MYKET) {
                    _isConnected.value = conn
                }
            }
        }

        scope.launch {
            googlePlayBillingManager.productPrices.collect { prices ->
                if (_activeStore.value == StoreBillingType.GOOGLE_PLAY && prices.isNotEmpty()) {
                    _productPrices.value = prices
                }
            }
        }

        scope.launch {
            bazaarBillingManager.productPrices.collect { prices ->
                if (_activeStore.value == StoreBillingType.CAFE_BAZAAR && prices.isNotEmpty()) {
                    _productPrices.value = prices
                }
            }
        }

        scope.launch {
            myketBillingManager.productPrices.collect { prices ->
                if (_activeStore.value == StoreBillingType.MYKET && prices.isNotEmpty()) {
                    _productPrices.value = prices
                }
            }
        }

        scope.launch {
            googlePlayBillingManager.isPurchasing.collect { purchasing ->
                if (_activeStore.value == StoreBillingType.GOOGLE_PLAY) {
                    _isPurchasing.value = purchasing
                }
            }
        }

        scope.launch {
            bazaarBillingManager.isPurchasing.collect { purchasing ->
                if (_activeStore.value == StoreBillingType.CAFE_BAZAAR) {
                    _isPurchasing.value = purchasing
                }
            }
        }

        scope.launch {
            myketBillingManager.isPurchasing.collect { purchasing ->
                if (_activeStore.value == StoreBillingType.MYKET) {
                    _isPurchasing.value = purchasing
                }
            }
        }
    }

    fun startBillingConnection() {
        currentProvider.startConnection()
    }

    fun queryProductDetails() {
        currentProvider.queryProductDetails()
    }

    fun launchBillingFlow(activity: Activity, productId: String): Boolean {
        return currentProvider.launchBillingFlow(activity, productId)
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

        scope.launch {
            _purchaseResult.emit(
                BillingPurchaseResult.Success(
                    productId = canonicalId,
                    coinsAwarded = coins,
                    orderId = dummyOrder
                )
            )
        }
    }

    fun endConnection() {
        googlePlayBillingManager.endConnection()
        bazaarBillingManager.endConnection()
        myketBillingManager.endConnection()
    }
}
