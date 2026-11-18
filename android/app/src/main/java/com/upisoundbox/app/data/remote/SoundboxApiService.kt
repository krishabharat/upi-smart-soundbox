package com.upisoundbox.app.data.remote

import com.upisoundbox.app.data.model.Merchant
import com.upisoundbox.app.data.model.PaymentTransaction
import com.upisoundbox.app.data.model.TodayCollectionSummary
import retrofit2.Response
import retrofit2.http.*

interface SoundboxApiService {

    @POST("api/v1/merchants/register")
    suspend fun registerMerchant(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Merchant>

    @GET("api/v1/merchants/{id}")
    suspend fun getMerchant(
        @Path("id") merchantId: String
    ): Response<Merchant>

    @GET("api/v1/merchants/{id}/summary/today")
    suspend fun getTodaySummary(
        @Path("id") merchantId: String
    ): Response<TodayCollectionSummary>

    @GET("api/v1/merchants/{id}/transactions")
    suspend fun getTransactions(
        @Path("id") merchantId: String,
        @Query("limit") limit: Int = 50
    ): Response<List<PaymentTransaction>>

    @GET("api/v1/merchants/{id}/transactions/pending-announcements")
    suspend fun getPendingAnnouncements(
        @Path("id") merchantId: String
    ): Response<List<PaymentTransaction>>

    @POST("api/v1/transactions/{id}/ack")
    suspend fun acknowledgeAnnouncement(
        @Path("id") transactionId: String
    ): Response<Map<String, Any>>
}

