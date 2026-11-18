package com.upisoundbox.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.upisoundbox.app.utils.AmountSpeechFormatter
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "TTSManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // In-memory duplicate guard with fast O(1) concurrent lookup
    private val spokenTransactionIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    // Pending announcements while TTS is warming up
    private val pendingSpeechQueue = mutableListOf<Triple<String, String, (() -> Unit)?>>()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    var speechRate: Float = 0.95f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(tag, "TTS Initialized successfully.")
            // Try Indian English first, then general English
            val inLocale = Locale("en", "IN")
            val result = tts?.setLanguage(inLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(tag, "en_IN not available, falling back to US English")
                tts?.language = Locale.US
            }

            // Route audio explicitly to speaker stream
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)

            setupUtteranceListener()
            isInitialized = true

            // Flush pending items
            synchronized(pendingSpeechQueue) {
                while (pendingSpeechQueue.isNotEmpty()) {
                    val item = pendingSpeechQueue.removeAt(0)
                    speakInternal(item.first, item.second, item.third)
                }
            }
        } else {
            Log.e(tag, "Failed to initialize TextToSpeech engine. Status: $status")
            isInitialized = false
        }
    }

    private val callbackMap = ConcurrentHashMap<String, () -> Unit>()

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(tag, "TTS Started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(tag, "TTS Done: $utteranceId")
                utteranceId?.let { id ->
                    callbackMap.remove(id)?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e(tag, "TTS Error on: $utteranceId")
                utteranceId?.let { id ->
                    callbackMap.remove(id)?.invoke()
                }
            }
        })
    }

    /**
     * Announces a verified payment amount.
     * Prevents repeated announcements for the same transaction.
     */
    fun speakPayment(amount: Double, transactionId: String, onDone: (() -> Unit)? = null): Boolean {
        // Duplicate check
        if (spokenTransactionIds.contains(transactionId)) {
            Log.w(tag, "Duplicate payment announcement blocked for txId: $transactionId")
            return false
        }
        spokenTransactionIds.add(transactionId)

        val announcementText = AmountSpeechFormatter.format(amount)
        speakText(announcementText, transactionId, onDone)
        return true
    }

    private fun playChimeTone() {
        try {
            val toneGen = android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 180)
        } catch (e: Exception) {
            Log.w(tag, "Chime tone generation failed: ${e.message}")
        }
    }

    /**
     * Test sound button function.
     */
    fun speakTestSound(onDone: (() -> Unit)? = null) {
        val testText = "Payment received, 100 rupees. Thank you! UPI soundbox is active."
        playChimeTone()
        speakText(testText, "test_${System.currentTimeMillis()}", onDone)
    }

    fun speakText(text: String, utteranceId: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized) {
            synchronized(pendingSpeechQueue) {
                pendingSpeechQueue.add(Triple(text, utteranceId, onDone))
            }
            return
        }
        playChimeTone()
        speakInternal(text, utteranceId, onDone)
    }

    private fun speakInternal(text: String, utteranceId: String, onDone: (() -> Unit)?) {
        if (onDone != null) {
            callbackMap[utteranceId] = onDone
        }

        // Request audio focus so background music ducks or pauses
        requestAudioFocus()

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to request audio focus: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS: ${e.message}")
        }
        isInitialized = false
    }
}

