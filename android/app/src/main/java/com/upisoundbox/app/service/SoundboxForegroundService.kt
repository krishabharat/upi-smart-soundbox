package com.upisoundbox.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.upisoundbox.app.MainActivity
import com.upisoundbox.app.R
import com.upisoundbox.app.data.remote.ConnectionStatus
import com.upisoundbox.app.data.remote.PaymentVerifiedEvent
import com.upisoundbox.app.data.repository.SoundboxRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class SoundboxForegroundService : Service() {

    private val tag = "SoundboxService"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: SoundboxRepository
    private lateinit var ttsManager: TextToSpeechManager
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "soundbox_active_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_SOUNDBOX"
        const val ACTION_STOP = "ACTION_STOP_SOUNDBOX"
        const val ACTION_TEST_SOUND = "ACTION_TEST_SOUND"

        fun startService(context: Context) {
            val intent = Intent(context, SoundboxForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SoundboxForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service onCreate")
        repository = SoundboxRepository.getInstance(applicationContext)
        ttsManager = TextToSpeechManager(applicationContext)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UPISoundbox::WakeLock")

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(tag, "Received ACTION_STOP")
                repository.preferences.isSoundboxEnabled = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TEST_SOUND -> {
                ttsManager.speakTestSound()
                return START_STICKY
            }
            ACTION_START, null -> {
                startForegroundServiceWithNotification()
                observePaymentsAndConnection()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification("Listening for verified customer payments…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observePaymentsAndConnection() {
        val merchant = repository.preferences.getMerchant()
        if (merchant == null) {
            Log.w(tag, "No merchant registered. Cannot connect stream.")
            return
        }

        val baseUrl = repository.preferences.backendUrl
        repository.streamListener.startListening(baseUrl, merchant.id)

        // Listen for incoming payment events
        serviceScope.launch {
            repository.paymentEvents.collect { event ->
                handleVerifiedPayment(event)
            }
        }

        // Listen to connection status to update notification
        serviceScope.launch {
            repository.connectionStatus.collectLatest { status ->
                val statusText = when (status) {
                    ConnectionStatus.CONNECTED -> "Soundbox Active - Connected"
                    ConnectionStatus.CONNECTING -> "Soundbox - Connecting to gateway…"
                    ConnectionStatus.DISCONNECTED -> "Soundbox - Disconnected"
                    ConnectionStatus.ERROR -> "Soundbox - Network reconnecting…"
                }
                updateNotification(statusText)
            }
        }
    }

    private fun handleVerifiedPayment(event: PaymentVerifiedEvent) {
        if (!repository.preferences.isSoundboxEnabled) {
            Log.d(tag, "Soundbox is toggled OFF by shopkeeper. Ignoring speech.")
            return
        }

        serviceScope.launch {
            // Check database duplicate check
            val alreadySpoken = repository.isTransactionSpoken(event.transactionId)
            if (alreadySpoken) {
                Log.w(tag, "Payment ${event.transactionId} already announced in past. Skipping.")
                return@launch
            }

            // Acquire temporary wake lock so CPU stays awake until speech completes
            try {
                wakeLock?.acquire(15000L) // 15 sec max safety timeout
            } catch (e: Exception) {
                Log.w(tag, "WakeLock acquire error: ${e.message}")
            }

            // Announce through phone speaker
            val spoken = ttsManager.speakPayment(event.amount, event.transactionId) {
                // Done callback
                serviceScope.launch {
                    repository.saveAndAcknowledgePayment(event)
                    if (wakeLock?.isHeld == true) {
                        try {
                            wakeLock?.release()
                        } catch (ignored: Exception) {}
                    }
                }
            }

            if (spoken) {
                val payer = event.payerName ?: "Customer"
                val text = "Received ₹${event.amount} from $payer"
                updateNotification(text)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UPI Soundbox Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the soundbox active in background for real-time announcements"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SoundboxForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UPI Smart Soundbox")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Turn OFF", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        Log.d(tag, "Service onDestroy")
        serviceJob.cancel()
        repository.streamListener.stopListening()
        ttsManager.shutdown()
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (ignored: Exception) {}
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

