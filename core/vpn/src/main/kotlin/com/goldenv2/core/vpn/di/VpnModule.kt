package com.goldenv2.core.vpn.di

import com.goldenv2.core.vpn.service.VpnController
import com.goldenv2.core.vpn.service.VpnControllerImpl
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
    fun provideVpnController(impl: VpnControllerImpl): VpnController = impl
}