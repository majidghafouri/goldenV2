package com.goldenv2.app

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.ServerManagementUseCase
import com.goldenv2.core.domain.usecase.SubscriptionUseCase
import com.goldenv2.core.network.SubscriptionFetcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Periodically re-pulls every enabled subscription.
 *
 * Each subscription's servers are replaced atomically per subscription, so a failed
 * fetch leaves the previously downloaded servers untouched rather than wiping them.
 */
@HiltWorker
class SubscriptionRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val subscriptionUseCase: SubscriptionUseCase,
    private val serverManagementUseCase: ServerManagementUseCase,
    private val subscriptionFetcher: SubscriptionFetcher,
    private val logUseCase: LogUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val subscriptions = subscriptionUseCase.getEnabled().firstOrNull().orEmpty()
        if (subscriptions.isEmpty()) return Result.success()

        var failures = 0
        subscriptions.forEach { subscription ->
            runCatching {
                val servers = subscriptionFetcher.fetchAndParse(
                    url = subscription.url,
                    subscriptionId = subscription.id,
                    subscription = subscription
                ).getOrThrow()

                serverManagementUseCase.deleteBySubscription(subscription.id)
                serverManagementUseCase.addServers(servers)
                subscriptionUseCase.updateRefreshResult(subscription, true, servers.size)
            }.onFailure { error ->
                failures++
                subscriptionUseCase.updateRefreshResult(subscription, false, 0, error.message)
                logUseCase.e(TAG, "Auto-refresh failed for ${subscription.name}", error)
            }
        }
        logUseCase.i(TAG, "Auto-refresh finished: ${subscriptions.size} subscriptions, $failures failed")
        return if (failures == subscriptions.size) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "SubscriptionRefresh"

        const val UNIQUE_NAME = "goldenv2-subscription-refresh"

        /** Minimum WorkManager period; the OS decides the actual cadence. */
        private const val REFRESH_HOURS = 12L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SubscriptionRefreshWorker>(
                REFRESH_HOURS, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                // KEEP so a relaunch does not reset the schedule and never refreshes
                // more often than the OS already agreed to.
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
