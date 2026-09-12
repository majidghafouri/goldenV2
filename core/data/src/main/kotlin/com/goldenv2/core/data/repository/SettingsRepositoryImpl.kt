package com.goldenv2.core.data.repository

import com.goldenv2.core.data.datastore.SettingsDataStore
import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.RoutingConfig
import com.goldenv2.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun saveSettings(settings: AppSettings) = dataStore.saveSettings(settings)

    override suspend fun getSettings(): AppSettings = dataStore.getSettings()

    override val routingConfigFlow: Flow<RoutingConfig?> = dataStore.routingConfigFlow

    override suspend fun saveRoutingConfig(config: RoutingConfig) = dataStore.saveRoutingConfig(config)

    override suspend fun getRoutingConfig(): RoutingConfig? = dataStore.getRoutingConfig()

    override val firstRunFlow: Flow<Boolean> = dataStore.firstRunFlow

    override suspend fun setFirstRunComplete() = dataStore.setFirstRunComplete()

    override val lastSelectedServerIdFlow: Flow<String?> = dataStore.lastSelectedServerIdFlow

    override suspend fun saveLastSelectedServerId(serverId: String?) = dataStore.saveLastSelectedServerId(serverId)
}
