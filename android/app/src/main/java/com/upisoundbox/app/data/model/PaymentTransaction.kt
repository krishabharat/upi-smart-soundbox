package com.upisoundbox.app.data.model

import com.google.gson.annotations.SerializedName

data class PaymentTransaction(
    @SerializedName("id") val id: String,
    @SerializedName("merchant_id") val merchantId: String,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("payment_id") val paymentId: String? = null,
    @SerializedName("bank_rrn") val bankRrn: String? = null,
    @SerializedName("payer_vpa") val payerVpa: String? = null,
    @SerializedName("payer_name") val payerName: String? = null,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("status") val status: String,
    @SerializedName("gateway_provider") val gatewayProvider: String = "sandbox",
    @SerializedName("announced") val announced: Boolean = false,
    @SerializedName("announcement_text") val announcementText: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("verified_at") val verifiedAt: String? = null
)

