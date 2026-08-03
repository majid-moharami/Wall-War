package com.wallwar.di

import android.content.Context
import com.wallwar.audio.SoundManager
import com.wallwar.data.AppDatabase
import com.wallwar.data.MatchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMatchDao(database: AppDatabase): MatchDao {
        return database.matchDao()
    }

    @Provides
    @Singleton
    fun provideSoundManager(@ApplicationContext context: Context): SoundManager {
        return SoundManager(context)
    }
}
