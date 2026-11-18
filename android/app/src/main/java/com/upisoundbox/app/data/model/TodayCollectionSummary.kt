package com.upisoundbox.app.data.model

import com.google.gson.annotations.SerializedName

data class TodayCollectionSummary(
    @SerializedName("merchant_id") val merchantId: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("transaction_count") val transactionCount: Int = 0,
    @SerializedName("latest_transaction") val latestTransaction: PaymentTransaction? = null
)
