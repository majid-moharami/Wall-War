package com.wallwar.di

import com.wallwar.data.billing.StoreBillingProvider
import com.wallwar.data.billing.bazaar.BazaarBillingManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingFlavorModule {
    @Binds
    @Singleton
    abstract fun bindStoreBillingProvider(
        impl: BazaarBillingManager
    ): StoreBillingProvider
}
