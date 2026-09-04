package com.goldenv2.core.domain.di

import com.goldenv2.core.domain.usecase.GetServersUseCase
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.ServerManagementUseCase
import com.goldenv2.core.domain.usecase.SettingsUseCase
import com.goldenv2.core.domain.usecase.SubscriptionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideGetServersUseCase(useCase: GetServersUseCase): GetServersUseCase = useCase

    @Provides
    @Singleton
    fun provideServerManagementUseCase(useCase: ServerManagementUseCase): ServerManagementUseCase = useCase

    @Provides
    @Singleton
    fun provideSubscriptionUseCase(useCase: SubscriptionUseCase): SubscriptionUseCase = useCase

    @Provides
    @Singleton
    fun provideSettingsUseCase(useCase: SettingsUseCase): SettingsUseCase = useCase

    @Provides
    @Singleton
    fun provideLogUseCase(useCase: LogUseCase): LogUseCase = useCase
}