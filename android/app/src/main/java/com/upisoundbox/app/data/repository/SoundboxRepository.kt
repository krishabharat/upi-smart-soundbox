package com.upisoundbox.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.upisoundbox.app.data.local.MerchantPreferences
import com.upisoundbox.app.data.local.SoundboxDatabase
import com.upisoundbox.app.data.local.TransactionEntity
import com.upisoundbox.app.data.model.Merchant
import com.upisoundbox.app.data.model.PaymentTransaction
import com.upisoundbox.app.data.model.TodayCollectionSummary
import com.upisoundbox.app.data.remote.ConnectionStatus
import com.upisoundbox.app.data.remote.PaymentVerifiedEvent
import com.upisoundbox.app.data.remote.SoundboxApiService
import com.upisoundbox.app.data.remote.SoundboxStreamListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SoundboxRepository private constructor(context: Context) {

    val preferences = MerchantPreferences(context)
    private val database = SoundboxDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for SSE
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private var apiService: SoundboxApiService = createApiService(preferences.backendUrl)
    val streamListener = SoundboxStreamListener(okHttpClient)

    val connectionStatus: StateFlow<ConnectionStatus> = streamListener.connectionStatus
    val paymentEvents: SharedFlow<PaymentVerifiedEvent> = streamListener.paymentEvents

    companion object {
        @Volatile
        private var INSTANCE: SoundboxRepository? = null

        fun getInstance(context: Context): SoundboxRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundboxRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun updateBackendUrl(newUrl: String) {
        preferences.backendUrl = newUrl
        apiService = createApiService(newUrl)
    }

    private fun createApiService(baseUrl: String): SoundboxApiService {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SoundboxApiService::class.java)
    }

    suspend fun registerMerchant(businessName: String, upiId: String, mobile: String?): Result<Merchant> {
        return try {
            val body = mutableMapOf<String, Any>(
                "business_name" to businessName.trim(),
                "upi_id" to upiId.trim().lowercase()
            )
            if (!mobile.isNullOrBlank()) {
                body["mobile_number"] = mobile.trim()
            }

            val response = apiService.registerMerchant(body)
            if (response.isSuccessful && response.body() != null) {
                val merchant = response.body()!!
                preferences.saveMerchant(merchant)
                Result.success(merchant)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTodaySummary(merchantId: String): Result<TodayCollectionSummary> {
        return try {
            val response = apiService.getTodaySummary(merchantId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch summary: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTransactions(merchantId: String): Result<List<PaymentTransaction>> {
        return try {
            val response = apiService.getTransactions(merchantId, limit = 100)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                // Sync to local database
                val entities = list.map { tx ->
                    TransactionEntity(
                        id = tx.id,
                        merchantId = tx.merchantId,
                        orderId = tx.orderId,
                        paymentId = tx.paymentId,
                        bankRrn = tx.bankRrn,
                        payerVpa = tx.payerVpa,
                        payerName = tx.payerName,
                        amount = tx.amount,
                        currency = tx.currency,
                        status = tx.status,
                        announced = tx.announced,
                        announcementText = tx.announcementText,
                        createdAt = System.currentTimeMillis()
                    )
                }
                transactionDao.insertAll(entities)
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch transactions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAndAcknowledgePayment(event: PaymentVerifiedEvent) {
        val entity = TransactionEntity(
            id = event.transactionId,
            merchantId = event.merchantId,
            orderId = event.orderId,
            paymentId = event.paymentId,
            bankRrn = event.bankRrn,
            payerVpa = event.payerVpa,
            payerName = event.payerName,
            amount = event.amount,
            currency = event.currency,
            status = "SUCCESS",
            announced = true,
            announcementText = event.announcementText,
            createdAt = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(entity)

        // Acknowledge back to server
        try {
            apiService.acknowledgeAnnouncement(event.transactionId)
        } catch (ignored: Exception) {}
    }

    suspend fun isTransactionSpoken(txId: String): Boolean {
        return transactionDao.isTransactionAlreadySpoken(txId)
    }

    fun getAllLocalTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactionsFlow()
    }

    fun getTodayLocalTransactions(): Flow<List<TransactionEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return transactionDao.getTodayTransactionsFlow(calendar.timeInMillis)
    }

    fun getTodayTotalAmount(): Flow<Double> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return transactionDao.getTodayTotalAmountFlow(calendar.timeInMillis)
    }

    fun getTodayCount(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return transactionDao.getTodayCountFlow(calendar.timeInMillis)
    }
}

