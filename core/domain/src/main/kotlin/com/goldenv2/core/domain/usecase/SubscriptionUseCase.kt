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
    /**
     * Inserts [subscription] and returns the stored row.
     *
     * The returned value must be used for any follow-up write (for example recording a
     * refresh result). The id is generated here rather than in the entity default so that
     * the caller, the servers tagged with `subscriptionId`, and the row in the table all
     * agree - Room matches [update] on the primary key, so passing the pre-insert object
     * silently updates nothing and leaves the servers orphaned.
     */
    suspend fun addSubscription(subscription: Subscription): Subscription {
        val stored = subscription.copy(id = UUID.randomUUID().toString())
        repository.insert(stored)
        return stored
    }

    suspend fun updateSubscription(subscription: Subscription) = repository.update(subscription)

    suspend fun deleteSubscription(id: String) = repository.delete(id)

    fun getAll(): kotlinx.coroutines.flow.Flow<List<Subscription>> = repository.getAll()

    fun getEnabled(): kotlinx.coroutines.flow.Flow<List<Subscription>> = repository.getEnabled()

    fun getById(id: String): kotlinx.coroutines.flow.Flow<Subscription?> = repository.getById(id)

    suspend fun toggleEnabled(subscription: Subscription) =
        repository.update(subscription.copyEnabled(!subscription.isEnabled))

    /** Pins a subscription above the others, or unpins it. */
    suspend fun togglePinned(subscription: Subscription) =
        repository.update(subscription.copyPinned(!subscription.isPinned))

    suspend fun updateRefreshResult(
        subscription: Subscription,
        success: Boolean,
        serverCount: Int,
        error: String? = null
    ) = repository.update(subscription.copyWithRefreshResult(success, serverCount, error))
}