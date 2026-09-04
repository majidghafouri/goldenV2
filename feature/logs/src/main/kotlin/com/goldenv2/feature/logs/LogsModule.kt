package com.goldenv2.feature.logs

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogsModule {
    // ViewModels are provided by Hilt automatically via @HiltViewModel
}