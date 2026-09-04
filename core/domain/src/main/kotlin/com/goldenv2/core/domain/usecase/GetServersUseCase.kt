package com.goldenv2.core.domain.usecase

import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetServersUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    operator fun invoke(): Flow<List<Server>> = repository.getAll()

    fun bySubscription(subscriptionId: String): Flow<List<Server>> = repository.getBySubscription(subscriptionId)

    fun search(query: String): Flow<List<Server>> = repository.search(query)

    fun getSelected(): Flow<Server?> = repository.getSelected()
}