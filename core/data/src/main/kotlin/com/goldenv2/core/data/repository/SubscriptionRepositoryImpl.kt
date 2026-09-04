package com.goldenv2.core.data.repository

import com.goldenv2.core.data.db.AppDatabase
import com.goldenv2.core.data.db.SubscriptionDao
import com.goldenv2.core.domain.model.Subscription
import com.goldenv2.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : SubscriptionRepository {
    private val dao: SubscriptionDao = database.subscriptionDao()

    override suspend fun insert(subscription: Subscription) = dao.insert(subscription)

    override suspend fun insertAll(subscriptions: List<Subscription>) = dao.insertAll(subscriptions)

    override suspend fun update(subscription: Subscription) = dao.update(subscription)

    override suspend fun delete(id: String) = dao.delete(id)

    override fun getById(id: String): Flow<Subscription?> = dao.getById(id)

    override fun getAll(): Flow<List<Subscription>> = dao.getAll()

    override fun getEnabled(): Flow<List<Subscription>> = dao.getEnabled()

    override suspend fun count(): Int = dao.count()
}