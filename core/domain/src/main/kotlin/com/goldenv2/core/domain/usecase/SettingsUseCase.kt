package com.goldenv2.core.domain.usecase

import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    val settingsFlow = repository.settingsFlow

    val firstRunFlow = repository.firstRunFlow

    val lastSelectedServerIdFlow = repository.lastSelectedServerIdFlow

    suspend fun saveSettings(settings: AppSettings) = repository.saveSettings(settings)

    suspend fun getSettings(): AppSettings = repository.getSettings()

    suspend fun setFirstRunComplete() = repository.setFirstRunComplete()

    suspend fun saveLastSelectedServerId(serverId: String?) = repository.saveLastSelectedServerId(serverId)
}