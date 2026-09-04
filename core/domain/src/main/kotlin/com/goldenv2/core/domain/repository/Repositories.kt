package com.goldenv2.core.domain.repository

import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    suspend fun insert(server: Server)
    suspend fun insertAll(servers: List<Server>)
    suspend fun update(server: Server)
    suspend fun delete(id: String)
    suspend fun deleteBySubscription(subscriptionId: String)
    fun getById(id: String): Flow<Server?>
    fun getBySubscription(subscriptionId: String): Flow<List<Server>>
    fun getAll(): Flow<List<Server>>
    fun getSelected(): Flow<Server?>
    fun search(query: String): Flow<List<Server>>
    suspend fun updateLatency(id: String, latency: Long)
    suspend fun clearSelection()
    suspend fun selectServer(id: String)
    suspend fun count(): Int
}

interface SubscriptionRepository {
    suspend fun insert(subscription: Subscription)
    suspend fun insertAll(subscriptions: List<Subscription>)
    suspend fun update(subscription: Subscription)
    suspend fun delete(id: String)
    fun getById(id: String): Flow<Subscription?>
    fun getAll(): Flow<List<Subscription>>
    fun getEnabled(): Flow<List<Subscription>>
    suspend fun count(): Int
}

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun saveSettings(settings: AppSettings)
    suspend fun getSettings(): AppSettings
    val firstRunFlow: Flow<Boolean>
    suspend fun setFirstRunComplete()
    val lastSelectedServerIdFlow: Flow<String?>
    suspend fun saveLastSelectedServerId(serverId: String?)
}

interface LogRepository {
    val logsFlow: Flow<List<com.goldenv2.core.domain.model.LogEntry>>
    suspend fun addLog(entry: com.goldenv2.core.domain.model.LogEntry)
    suspend fun clearLogs()
    fun getLogs(): Flow<List<com.goldenv2.core.domain.model.LogEntry>>
}