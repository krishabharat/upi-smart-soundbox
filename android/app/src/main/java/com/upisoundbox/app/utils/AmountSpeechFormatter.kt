package com.upisoundbox.app.utils

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object AmountSpeechFormatter {

    /**
     * Formats amount according to project specifications:
     * - "Payment received, 50 rupees."
     * - "Payment received, 100 rupees."
     * - "Payment received, 250 rupees."
     * - "Payment received, 1,000 rupees."
     */
    fun format(amount: Double): String {
        val roundedAmount = (amount * 100.0).roundToLong() / 100.0
        val rupees = roundedAmount.toLong()
        val paise = ((roundedAmount - rupees) * 100).roundToLong()

        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
        val formattedRupees = indianFormat.format(rupees)

        return when {
            rupees == 1L && paise == 0L -> "Payment received, 1 rupee. Thank you!"
            paise > 0L -> {
                if (rupees == 0L) {
                    "Payment received, $paise paise. Thank you!"
                } else {
                    "Payment received, $formattedRupees rupees and $paise paise. Thank you!"
                }
            }
            else -> "Payment received, $formattedRupees rupees. Thank you!"
        }
    }
}

