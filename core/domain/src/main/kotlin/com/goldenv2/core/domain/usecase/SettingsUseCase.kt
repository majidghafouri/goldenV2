package com.goldenv2.core.domain.usecase

import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.RoutingConfig
import com.goldenv2.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    val settingsFlow = repository.settingsFlow

    val routingConfigFlow = repository.routingConfigFlow

    val firstRunFlow = repository.firstRunFlow

    val lastSelectedServerIdFlow = repository.lastSelectedServerIdFlow

    suspend fun saveSettings(settings: AppSettings) = repository.saveSettings(settings)

    suspend fun getSettings(): AppSettings = repository.getSettings()

    suspend fun saveRoutingConfig(config: RoutingConfig) = repository.saveRoutingConfig(config)

    suspend fun getRoutingConfig(): RoutingConfig? = repository.getRoutingConfig()

    suspend fun setFirstRunComplete() = repository.setFirstRunComplete()

    suspend fun saveLastSelectedServerId(serverId: String?) = repository.saveLastSelectedServerId(serverId)
}