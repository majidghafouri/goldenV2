package com.goldenv2.core.data.di

import android.content.Context
import com.goldenv2.core.data.db.AppDatabase
import com.goldenv2.core.data.db.Converters
import com.goldenv2.core.data.db.ServerDao
import com.goldenv2.core.data.db.SubscriptionDao
import com.goldenv2.core.data.datastore.SettingsDataStore
import com.goldenv2.core.data.repository.LogRepositoryImpl
import com.goldenv2.core.data.repository.ServerRepositoryImpl
import com.goldenv2.core.data.repository.SettingsRepositoryImpl
import com.goldenv2.core.data.repository.SubscriptionRepositoryImpl
import com.goldenv2.core.domain.repository.LogRepository
import com.goldenv2.core.domain.repository.ServerRepository
import com.goldenv2.core.domain.repository.SettingsRepository
import com.goldenv2.core.domain.repository.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideServerDao(database: AppDatabase): ServerDao = database.serverDao()

    @Provides
    @Singleton
    fun provideSubscriptionDao(database: AppDatabase): SubscriptionDao = database.subscriptionDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideServerRepository(repo: ServerRepositoryImpl): ServerRepository = repo

    @Provides
    @Singleton
    fun provideSubscriptionRepository(repo: SubscriptionRepositoryImpl): SubscriptionRepository = repo

    @Provides
    @Singleton
    fun provideSettingsRepository(repo: SettingsRepositoryImpl): SettingsRepository = repo

    @Provides
    @Singleton
    fun provideLogRepository(): LogRepository = LogRepositoryImpl()
}