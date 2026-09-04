package com.goldenv2.core.domain.usecase

import com.goldenv2.core.domain.model.Subscription
import com.goldenv2.core.domain.repository.SubscriptionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend fun addSubscription(subscription: Subscription) = repository.insert(subscription.copy(id = UUID.randomUUID().toString()))

    suspend fun updateSubscription(subscription: Subscription) = repository.update(subscription)

    suspend fun deleteSubscription(id: String) = repository.delete(id)

    fun getAll(): kotlinx.coroutines.flow.Flow<List<Subscription>> = repository.getAll()

    fun getEnabled(): kotlinx.coroutines.flow.Flow<List<Subscription>> = repository.getEnabled()

    fun getById(id: String): kotlinx.coroutines.flow.Flow<Subscription?> = repository.getById(id)

    suspend fun toggleEnabled(subscription: Subscription) =
        repository.update(subscription.copy(isEnabled = !subscription.isEnabled))

    suspend fun updateRefreshResult(
        subscription: Subscription,
        success: Boolean,
        serverCount: Int,
        error: String? = null
    ) = repository.update(subscription.copyWithRefreshResult(success, serverCount, error))
}