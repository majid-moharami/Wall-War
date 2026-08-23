package com.wallwar

import android.app.Application
import com.adivery.sdk.Adivery
import com.google.android.gms.ads.MobileAds
import com.wallwar.data.ad.AdiveryConstants
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WallWarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        try {
            Adivery.configure(this, AdiveryConstants.ADIVERY_APP_ID)
            Adivery.setLoggingEnabled(true)
            Adivery.prepareRewardedAd(this, AdiveryConstants.REWARDED_PLACEMENT_ID)
            Adivery.prepareInterstitialAd(this, AdiveryConstants.INTERSTITIAL_PLACEMENT_ID)
            android.util.Log.d("WallWarApplication", "Adivery configured and ads preloaded successfully.")
        } catch (e: Exception) {
            android.util.Log.e("WallWarApplication", "Failed to configure Adivery: ${e.message}")
        }
    }
}

