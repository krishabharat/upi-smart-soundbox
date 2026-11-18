package com.upisoundbox.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.upisoundbox.app.data.model.Merchant

class MerchantPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "upi_soundbox_prefs"
        private const val KEY_MERCHANT_ID = "merchant_id"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_UPI_ID = "upi_id"
        private const val KEY_MOBILE_NUMBER = "mobile_number"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_SOUNDBOX_ENABLED = "soundbox_enabled"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_PITCH = "tts_pitch"
        
        // Default emulator backend host or fallback
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000"
    }

    var isRegistered: Boolean
        get() = prefs.getBoolean(KEY_IS_REGISTERED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_REGISTERED, value).apply()

    var isSoundboxEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUNDBOX_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUNDBOX_ENABLED, value).apply()

    var backendUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value.trim()).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEED, value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_PITCH, value).apply()

    fun saveMerchant(merchant: Merchant) {
        prefs.edit()
            .putString(KEY_MERCHANT_ID, merchant.id)
            .putString(KEY_BUSINESS_NAME, merchant.businessName)
            .putString(KEY_UPI_ID, merchant.upiId)
            .putString(KEY_MOBILE_NUMBER, merchant.mobileNumber)
            .putString(KEY_API_KEY, merchant.apiKey)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()
    }

    fun getMerchant(): Merchant? {
        val id = prefs.getString(KEY_MERCHANT_ID, null) ?: return null
        val name = prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
        val upi = prefs.getString(KEY_UPI_ID, "") ?: ""
        val mobile = prefs.getString(KEY_MOBILE_NUMBER, null)
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        return Merchant(
            id = id,
            businessName = name,
            upiId = upi,
            mobileNumber = mobile,
            apiKey = apiKey,
            isActive = true
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

