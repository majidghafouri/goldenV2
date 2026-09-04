package com.goldenv2.core.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.goldenv2.core.network.parser.parseSubscriptionContent

interface SubscriptionApi {
    @retrofit2.http.GET
    suspend fun fetchSubscription(@retrofit2.http.Url url: String): String
}

class SubscriptionFetcher(
    private val api: SubscriptionApi,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetchAndParse(url: String, subscriptionId: String): Result<List<com.goldenv2.core.domain.model.Server>> {
        return try {
            val content = withContext(ioDispatcher) { api.fetchSubscription(url) }
            val servers = parseSubscriptionContent(content)
                .map { it.copy(subscriptionId = subscriptionId) }
            Result.success(servers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
