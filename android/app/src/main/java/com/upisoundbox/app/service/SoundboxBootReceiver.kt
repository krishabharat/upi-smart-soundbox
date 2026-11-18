package com.upisoundbox.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.upisoundbox.app.data.local.MerchantPreferences

class SoundboxBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Device boot completed or package updated. Checking soundbox auto-start.")
            val prefs = MerchantPreferences(context)
            if (prefs.isRegistered && prefs.isSoundboxEnabled) {
                Log.d("BootReceiver", "Starting SoundboxForegroundService automatically.")
                SoundboxForegroundService.startService(context)
            }
        }
    }
}

