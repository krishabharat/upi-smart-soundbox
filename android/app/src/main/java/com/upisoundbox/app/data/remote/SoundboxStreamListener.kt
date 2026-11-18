package com.upisoundbox.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.upisoundbox.app.data.model.PaymentTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class PaymentVerifiedEvent(
    val merchantId: String,
    val transactionId: String,
    val orderId: String,
    val paymentId: String?,
    val bankRrn: String?,
    val amount: Double,
    val currency: String,
    val payerName: String?,
    val payerVpa: String?,
    val announcementText: String,
    val timestamp: String
)

class SoundboxStreamListener(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val tag = "SoundboxStream"
    private var eventSource: EventSource? = null
    private var isManualStop = false

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _paymentEvents = MutableSharedFlow<PaymentVerifiedEvent>(extraBufferCapacity = 64)
    val paymentEvents = _paymentEvents.asSharedFlow()

    private val listenerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startListening(baseUrl: String, merchantId: String) {
        if (merchantId.isBlank()) return
        isManualStop = false

        stopListening()
        _connectionStatus.value = ConnectionStatus.CONNECTING

        val streamUrl = "${baseUrl.trimEnd('/')}/api/v1/merchants/$merchantId/stream"
        Log.d(tag, "Connecting to SSE stream: $streamUrl")

        val request = Request.Builder()
            .url(streamUrl)
            .addHeader("Accept", "text/event-stream")
            .build()

        val factory = EventSources.createFactory(client)
        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(tag, "SSE Stream Connected successfully!")
                _connectionStatus.value = ConnectionStatus.CONNECTED
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.d(tag, "Received SSE data: $data")
                try {
                    val map = gson.fromJson(data, Map::class.java)
                    val eventType = map["event"] as? String

                    if (eventType == "PAYMENT_VERIFIED") {
                        val event = PaymentVerifiedEvent(
                            merchantId = (map["merchant_id"] as? String) ?: "",
                            transactionId = (map["transaction_id"] as? String) ?: "",
                            orderId = (map["order_id"] as? String) ?: "",
                            paymentId = map["payment_id"] as? String,
                            bankRrn = map["bank_rrn"] as? String,
                            amount = ((map["amount"] as? Number)?.toDouble()) ?: 0.0,
                            currency = (map["currency"] as? String) ?: "INR",
                            payerName = map["payer_name"] as? String,
                            payerVpa = map["payer_vpa"] as? String,
                            announcementText = (map["announcement_text"] as? String) ?: "",
                            timestamp = (map["timestamp"] as? String) ?: ""
                        )
                        _paymentEvents.tryEmit(event)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to parse SSE payload: ${e.message}")
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(tag, "SSE Stream closed")
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                retryIfNeeded(baseUrl, merchantId)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.w(tag, "SSE Stream connection failure: ${t?.message}")
                _connectionStatus.value = ConnectionStatus.ERROR
                retryIfNeeded(baseUrl, merchantId)
            }
        })
    }

    private fun retryIfNeeded(baseUrl: String, merchantId: String) {
        if (!isManualStop) {
            listenerScope.launch {
                delay(3000) // 3 seconds retry backoff
                if (!isManualStop) {
                    startListening(baseUrl, merchantId)
                }
            }
        }
    }

    fun stopListening() {
        isManualStop = true
        eventSource?.cancel()
        eventSource = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }
}

