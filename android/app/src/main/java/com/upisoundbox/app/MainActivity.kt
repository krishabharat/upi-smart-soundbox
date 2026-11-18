package com.upisoundbox.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.service.SoundboxForegroundService
import com.upisoundbox.app.ui.screens.MainSoundboxScreen
import com.upisoundbox.app.ui.theme.UPISoundboxTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: SoundboxRepository

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndStartSoundboxService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = (application as SoundboxApplication).repository

        requestNotificationPermission()

        setContent {
            UPISoundboxTheme {
                MainSoundboxScreen(repository = repository)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkAndStartSoundboxService()
            }
        } else {
            checkAndStartSoundboxService()
        }
    }

    private fun checkAndStartSoundboxService() {
        if (repository.preferences.isRegistered && repository.preferences.isSoundboxEnabled) {
            SoundboxForegroundService.startService(this)
        }
    }
}
