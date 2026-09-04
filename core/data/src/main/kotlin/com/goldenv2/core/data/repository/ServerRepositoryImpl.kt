package com.goldenv2.core.data.repository

import com.goldenv2.core.data.db.AppDatabase
import com.goldenv2.core.data.db.ServerDao
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : ServerRepository {
    private val dao: ServerDao = database.serverDao()

    override suspend fun insert(server: Server) = dao.insert(server)

    override suspend fun insertAll(servers: List<Server>) = dao.insertAll(servers)

    override suspend fun update(server: Server) = dao.update(server)

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun deleteBySubscription(subscriptionId: String) = dao.deleteBySubscription(subscriptionId)

    override fun getById(id: String): Flow<Server?> = dao.getById(id)

    override fun getBySubscription(subscriptionId: String): Flow<List<Server>> = dao.getBySubscription(subscriptionId)

    override fun getAll(): Flow<List<Server>> = dao.getAll()

    override fun getSelected(): Flow<Server?> = dao.getSelected()

    override fun search(query: String): Flow<List<Server>> = dao.search(query)

    override suspend fun updateLatency(id: String, latency: Long) = dao.updateLatency(id, latency)

    override suspend fun clearSelection() = dao.clearSelection()

    override suspend fun selectServer(id: String) = dao.selectServer(id)

    override suspend fun count(): Int = dao.count()
}