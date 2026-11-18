package com.upisoundbox.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE merchantId = :merchantId ORDER BY createdAt DESC")
    fun getTransactionsForMerchant(merchantId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE createdAt >= :startTimeMillis AND status = 'SUCCESS' ORDER BY createdAt DESC")
    fun getTodayTransactionsFlow(startTimeMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE createdAt >= :startTimeMillis AND status = 'SUCCESS'")
    fun getTodayTotalAmountFlow(startTimeMillis: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE createdAt >= :startTimeMillis AND status = 'SUCCESS'")
    fun getTodayCountFlow(startTimeMillis: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE (id = :txId OR orderId = :txId OR paymentId = :txId) AND announced = 1)")
    suspend fun isTransactionAlreadySpoken(txId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("UPDATE transactions SET announced = 1 WHERE id = :id OR orderId = :id")
    suspend fun markAsAnnounced(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

