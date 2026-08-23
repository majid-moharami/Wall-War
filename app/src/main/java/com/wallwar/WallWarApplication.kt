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
        } catch (e: Exception) {
            android.util.Log.e("WallWarApplication", "Failed to configure Adivery: ${e.message}")
        }
    }
}

