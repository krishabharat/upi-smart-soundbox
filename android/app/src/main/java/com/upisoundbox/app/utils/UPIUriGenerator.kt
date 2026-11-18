package com.upisoundbox.app.utils

import android.net.Uri
import java.util.regex.Pattern

object UPIUriGenerator {

    private val UPI_PATTERN = Pattern.compile("^[a-zA-Z0-9.\\-_]{2,64}@[a-zA-Z0-9.\\-_]{2,64}$")

    fun isValidUpiId(upiId: String): Boolean {
        return UPI_PATTERN.matcher(upiId.trim()).matches()
    }

    /**
     * Generates a compliant NPCI UPI payment URI string.
     * Example: upi://pay?pa=store@upi&pn=Store%20Name&mc=5411&cu=INR&am=100.00
     */
    fun buildUpiUri(
        upiId: String,
        businessName: String,
        amount: Double? = null,
        transactionNote: String? = null,
        mcc: String = "5411",
        orderId: String? = null
    ): String {
        val builder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId.trim())
            .appendQueryParameter("pn", businessName.trim())
            .appendQueryParameter("mc", mcc)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", transactionNote ?: "Payment to ${businessName.trim()}")

        if (orderId != null && orderId.isNotBlank()) {
            builder.appendQueryParameter("tr", orderId)
            builder.appendQueryParameter("tid", orderId)
        }

        if (amount != null && amount > 0) {
            builder.appendQueryParameter("am", String.format(java.util.Locale.US, "%.2f", amount))
        }

        return builder.build().toString()
    }
}

