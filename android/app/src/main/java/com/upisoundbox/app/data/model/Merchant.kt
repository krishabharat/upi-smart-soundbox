package com.upisoundbox.app.data.model

import com.google.gson.annotations.SerializedName

data class Merchant(
    @SerializedName("id") val id: String = "",
    @SerializedName("business_name") val businessName: String = "",
    @SerializedName("upi_id") val upiId: String = "",
    @SerializedName("mobile_number") val mobileNumber: String? = null,
    @SerializedName("mcc") val mcc: String = "5411",
    @SerializedName("api_key") val apiKey: String = "",
    @SerializedName("is_active") val isActive: Boolean = true
)

