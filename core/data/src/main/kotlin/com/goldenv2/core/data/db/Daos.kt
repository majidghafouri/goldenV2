package com.goldenv2.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: Server)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(servers: List<Server>)

    @Update
    suspend fun update(server: Server)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM servers WHERE subscriptionId = :subscriptionId")
    suspend fun deleteBySubscription(subscriptionId: String)

    @Query("SELECT * FROM servers WHERE id = :id")
    fun getById(id: String): Flow<Server?>

    @Query("SELECT * FROM servers WHERE subscriptionId = :subscriptionId ORDER BY name ASC")
    fun getBySubscription(subscriptionId: String): Flow<List<Server>>

    @Query("SELECT * FROM servers ORDER BY subscriptionId, name ASC")
    fun getAll(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    fun getSelected(): Flow<Server?>

    @Query("SELECT * FROM servers WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<Server>>

    @Query("UPDATE servers SET latency = :latency WHERE id = :id")
    suspend fun updateLatency(id: String, latency: Long)

    @Transaction
    @Query("UPDATE servers SET isSelected = 0")
    suspend fun clearSelection()

    @Transaction
    @Query("UPDATE servers SET isSelected = 1 WHERE id = :id")
    suspend fun selectServer(id: String)

    @Transaction
    suspend fun selectServerInTransaction(id: String) {
        clearSelection()
        selectServer(id)
    }

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun count(): Int

    @Transaction
    @Query("SELECT * FROM servers WHERE subscriptionId = :subscriptionId ORDER BY name ASC")
    fun getWithSubscription(subscriptionId: String): Flow<List<Server>>
}

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<Subscription>)

    @Update
    suspend fun update(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun getById(id: String): Flow<Subscription?>

    @Query("SELECT * FROM subscriptions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabled(): Flow<List<Subscription>>

    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun count(): Int
}