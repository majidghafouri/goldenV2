package com.goldenv2.core.network

import com.goldenv2.core.domain.model.Subscription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.goldenv2.core.network.parser.parseSubscriptionContent

interface SubscriptionApi {
    @retrofit2.http.GET
    suspend fun fetchSubscription(
        @retrofit2.http.Url url: String,
        @retrofit2.http.Header("User-Agent") userAgent: String
    ): String
}

class SubscriptionFetcher(
    private val api: SubscriptionApi,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetchAndParse(url: String, subscriptionId: String): Result<List<com.goldenv2.core.domain.model.Server>> {
        return fetchAndParse(url, subscriptionId, subscription = null)
    }

    suspend fun fetchAndParse(
        url: String,
        subscriptionId: String,
        subscription: Subscription?
    ): Result<List<com.goldenv2.core.domain.model.Server>> {
        return try {
            val content = withContext(ioDispatcher) {
                api.fetchSubscription(url, userAgentFor(subscription))
            }
            val servers = parseSubscriptionContent(content)
                .map { it.copy(subscriptionId = subscriptionId) }
            Result.success(servers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

companion object {
        /**
         * Most subscription providers serve the base64 config only to clients that
         * identify themselves; without this they commonly answer 403 or return an HTML
         * error page that parses to zero servers. A User-Agent is therefore always sent,
         * and a per-subscription override wins when set.
         */
        const val DEFAULT_USER_AGENT = "v2rayNG/1.8.5 (Android 13; okhttp 4.12.0)"

        fun userAgentFor(subscription: Subscription?): String =
            subscription?.userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT

        /**
         * Turns a fetch failure into something the user can act on. Android refuses
         * plain-HTTP subscriptions by default, and the raw message for that looks
         * exactly like an unreachable server.
         */
        fun describeFetchFailure(e: Throwable): String {
            val message = e.message.orEmpty()
            return when {
                message.contains("CLEARTEXT", ignoreCase = true) ->
                    "This link uses http:// which Android blocks. Use an https:// link."
                message.contains("Unable to resolve host", ignoreCase = true) ->
                    "Host not found: check the subscription link."
                message.contains("timeout", ignoreCase = true) ||
                    e is java.net.SocketTimeoutException ->
                    "The server did not respond in time."
                e is java.net.UnknownHostException -> "Host not found: check the subscription link."
                message.isNotBlank() -> message
                else -> e.javaClass.simpleName
            }
        }
    }
}