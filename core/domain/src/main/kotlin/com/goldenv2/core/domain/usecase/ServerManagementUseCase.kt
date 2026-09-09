package com.goldenv2.core.domain.usecase

import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerManagementUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    suspend fun addServer(server: Server) = repository.insert(server.copy(id = UUID.randomUUID().toString()))

    suspend fun addServers(servers: List<Server>) = repository.insertAll(
        servers.map { it.copy(id = UUID.randomUUID().toString()) }
    )

    suspend fun updateServer(server: Server) = repository.update(server)

    suspend fun deleteServer(id: String) = repository.delete(id)

    suspend fun deleteBySubscription(subscriptionId: String) = repository.deleteBySubscription(subscriptionId)

    suspend fun updateLatency(id: String, latency: Long) = repository.updateLatency(id, latency)

    fun observeSelectedServer(): Flow<Server?> = repository.getSelected()

    fun getById(id: String): Flow<Server?> = repository.getById(id)

    suspend fun selectServer(id: String) {
        repository.clearSelection()
        repository.selectServer(id)
    }

    suspend fun selectServerAndGet(id: String): Server? {
        selectServer(id)
        return repository.getById(id).first()
    }

    suspend fun duplicateServer(server: Server): Server {
        val newServer = server.copy(
            id = UUID.randomUUID().toString(),
            name = "${server.name} (copy)",
            isSelected = false
        )
        repository.insert(newServer)
        return newServer
    }

    suspend fun count(): Int = repository.count()
}