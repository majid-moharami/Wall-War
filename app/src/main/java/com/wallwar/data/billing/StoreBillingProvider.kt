package com.wallwar.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class StoreBillingType(val displayName: String, val storeName: String) {
    GOOGLE_PLAY("Google Play", "Google Play Store"),
    CAFE_BAZAAR("Cafe Bazaar", "کافه بازار"),
    MYKET("Myket", "مایکت")
}

interface StoreBillingProvider {
    val storeType: StoreBillingType
    val isConnected: StateFlow<Boolean>
    val isPurchasing: StateFlow<Boolean>
    val productPrices: StateFlow<Map<String, String>>
    val purchaseResult: SharedFlow<BillingPurchaseResult>

    fun startConnection()
    fun queryProductDetails()
    fun launchBillingFlow(activity: Activity, productId: String): Boolean
    fun endConnection()
}
