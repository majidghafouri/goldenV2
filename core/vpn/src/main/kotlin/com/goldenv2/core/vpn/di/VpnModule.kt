package com.goldenv2.core.vpn.di

import com.goldenv2.core.vpn.service.VpnController
import com.goldenv2.core.vpn.service.VpnServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VpnModule {

    @Provides
    @Singleton
    fun provideVpnController(service: VpnServiceImpl): VpnController = service
}