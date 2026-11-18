package com.upisoundbox.app.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.upisoundbox.app.data.local.TransactionEntity
import com.upisoundbox.app.data.repository.SoundboxRepository
import kotlinx.coroutines.*
import java.util.regex.Pattern

/**
 * Real-Time Bank & UPI Notification Listener Service.
 * Intercepts authentic bank credit alerts and UPI app notifications
 * (Google Pay, PhonePe, Paytm, BHIM, HDFC, SBI, ICICI, Axis, etc.)
 * when actual real money is received via the merchant's QR code.
 */
class UPIPaymentNotificationListenerService : NotificationListenerService() {

    private val tag = "UPIPaymentListener"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: SoundboxRepository
    private lateinit var ttsManager: TextToSpeechManager

    companion object {
        // Supported banking and UPI package names
        private val MONITORED_PACKAGES = setOf(
            // UPI Payment Apps
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app",                        // PhonePe Personal
            "com.phonepe.app.business",               // PhonePe Business
            "net.one97.paytm",                        // Paytm Personal
            "com.paytm.business",                     // Paytm for Business
            "in.org.npci.upiapp",                     // BHIM UPI
            "com.bharatpe.app",                       // BharatPe

            // Indian Bank Apps & SMS Managers
            "com.hdfcbank.eazybuy",
            "com.snapwork.hdfc",                      // HDFC Bank
            "com.sbi.lotusintouch",                   // SBI Yono
            "com.csam.icici.bank.imobile",            // ICICI iMobile
            "com.axis.mobile",                        // Axis Mobile
            "com.msf.kbank.mobile",                   // Kotak Bank

            // Default Android SMS Apps (where bank credit SMS notifications arrive)
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging"
        )

        // Comprehensive Regex for Indian UPI & Bank Credit Notifications
        // Matches:
        // "Received Rs. 200 from Rahul Sharma"
        // "Received ₹250.00 on PhonePe from Priya"
        // "A/c *1234 credited with Rs 500.00 via UPI"
        // "INR 1,000.00 credited to account ending ... UPI Ref 412345678901"
        private val AMOUNT_PATTERN = Pattern.compile(
            "(?:credited(?:\\s+by|\\s+with)?|received|payment\\s+of|credit\\s+of|rcvd|deposited)\\s*(?:Rs\\.?|INR|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )

        private val ALTERNATIVE_AMOUNT_PATTERN = Pattern.compile(
            "(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:credited|received|added)",
            Pattern.CASE_INSENSITIVE
        )

        private val UTR_PATTERN = Pattern.compile(
            "(?:ref|rrn|utr|upi\\s*ref|txn(?:\\s*id)?)[\\s:#-]*([0-9a-zA-Z]{8,16})",
            Pattern.CASE_INSENSITIVE
        )

        private val SENDER_PATTERN = Pattern.compile(
            "(?:from|by)\\s+([A-Za-z\\s]{2,30}?)(?:\\s+via|\\s+on|\\s+through|\\s+using|\\.|\\,|$)",
            Pattern.CASE_INSENSITIVE
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "UPIPaymentNotificationListenerService started.")
        repository = SoundboxRepository.getInstance(applicationContext)
        ttsManager = TextToSpeechManager(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract title and body
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullContent = "$title $text $bigText".trim()
        if (fullContent.isBlank()) return

        // Check if package is monitored OR content contains explicit UPI credit words
        val isMonitoredPackage = MONITORED_PACKAGES.contains(packageName)
        val hasCreditKeywords = isCreditNotification(fullContent)

        if (!isMonitoredPackage && !hasCreditKeywords) {
            return
        }

        // Must be a credit alert (NOT debit or OTP)
        if (isDebitOrOtp(fullContent)) {
            Log.d(tag, "Ignored debit/OTP notification: $title")
            return
        }

        // Parse amount
        val amount = extractAmount(fullContent) ?: return
        val utr = extractUtr(fullContent) ?: "RRN${System.currentTimeMillis().toString().takeLast(8)}"
        val sender = extractSender(fullContent) ?: "UPI Customer"

        Log.i(tag, "REAL PAYMENT DETECTED! Amount: ₹$amount, Sender: $sender, UTR: $utr from $packageName")

        handleRealPaymentReceived(amount, utr, sender)
    }

    private fun isCreditNotification(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("credited") || lower.contains("received") || lower.contains("credit of")) &&
                (lower.contains("upi") || lower.contains("₹") || lower.contains("rs") || lower.contains("inr"))
    }

    private fun isDebitOrOtp(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("debited") || lower.contains("spent") || lower.contains("withdrawn") ||
                lower.contains("otp") || lower.contains("verification code") || lower.contains("requested")
    }

    private fun extractAmount(text: String): Double? {
        var matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            return amountStr?.toDoubleOrNull()
        }

        matcher = ALTERNATIVE_AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            return amountStr?.toDoubleOrNull()
        }

        return null
    }

    private fun extractUtr(text: String): String? {
        val matcher = UTR_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun extractSender(text: String): String? {
        val matcher = SENDER_PATTERN.matcher(text)
        if (matcher.find()) {
            val sender = matcher.group(1)?.trim()
            if (!sender.isNullOrBlank() && sender.length > 1 && !sender.equals("upi", true)) {
                return sender
            }
        }
        return null
    }

    private fun handleRealPaymentReceived(amount: Double, utr: String, sender: String) {
        serviceScope.launch {
            // Duplicate check by UTR
            val alreadySpoken = repository.isTransactionSpoken(utr)
            if (alreadySpoken) {
                Log.w(tag, "Transaction $utr already announced before. Skipping duplicate.")
                return@launch
            }

            // 1. Play dual-tone soundbox chime & English voice announcement:
            // "Payment received, [amount] rupees. Thank you!"
            val announcementText = "Payment received, ${amount.toInt()} rupees. Thank you!"
            ttsManager.speakPayment(amount, utr)

            // 2. Add to local Received History
            val merchant = repository.preferences.getMerchant()
            val entity = TransactionEntity(
                id = "tx_${System.currentTimeMillis()}",
                merchantId = merchant?.id ?: "local_merchant",
                orderId = "ord_${System.currentTimeMillis().toString().takeLast(8)}",
                paymentId = "pay_real_$utr",
                bankRrn = utr,
                payerVpa = "$sender@upi",
                payerName = sender,
                amount = amount,
                currency = "INR",
                status = "SUCCESS",
                announced = true,
                announcementText = announcementText,
                createdAt = System.currentTimeMillis()
            )

            val db = com.upisoundbox.app.data.local.SoundboxDatabase.getDatabase(applicationContext)
            db.transactionDao().insertTransaction(entity)

            Log.i(tag, "Transaction successfully added to Received History: ₹$amount from $sender (Ref: $utr)")
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        ttsManager.shutdown()
        super.onDestroy()
    }
}

