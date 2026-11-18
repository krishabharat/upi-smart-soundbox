package com.upisoundbox.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.service.TextToSpeechManager
import com.upisoundbox.app.ui.theme.BackgroundSoft
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.SurfaceWhite
import com.upisoundbox.app.ui.theme.TextPrimary
import com.upisoundbox.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SoundboxRepository,
    onNavigateBack: () -> Unit,
    onEditMerchant: () -> Unit
) {
    val context = LocalContext.current
    val merchant = remember { repository.preferences.getMerchant() }

    var backendUrl by remember { mutableStateOf(repository.preferences.backendUrl) }
    var speechRate by remember { mutableStateOf(repository.preferences.ttsSpeed) }
    var speechPitch by remember { mutableStateOf(repository.preferences.ttsPitch) }

    val ttsManager = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    Scaffold(
        containerColor = BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Merchant Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Merchant Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        IconButton(onClick = onEditMerchant) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = FintechGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Store: ${merchant?.businessName ?: "Not set"}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "UPI ID: ${merchant?.upiId ?: "Not set"}", fontSize = 13.sp, color = FintechGreen)
                    merchant?.mobileNumber?.let {
                        Text(text = "Mobile: $it", fontSize = 13.sp, color = TextSecondary)
                    }
                    Text(text = "ID: ${merchant?.id ?: ""}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Voice & Speech Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Voice Announcement Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Speech Rate: ${(speechRate * 100).roundToInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            repository.preferences.ttsSpeed = it
                            ttsManager.speechRate = it
                        },
                        valueRange = 0.75f..1.25f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = FintechGreen, activeTrackColor = FintechGreen)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Voice Pitch: ${(speechPitch * 100).roundToInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            repository.preferences.ttsPitch = it
                            ttsManager.speechPitch = it
                        },
                        valueRange = 0.8f..1.2f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = FintechGreen, activeTrackColor = FintechGreen)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            ttsManager.speakPayment(100.0, "test_settings_${System.currentTimeMillis()}")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = FintechGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Preview 'Payment received, 100 rupees.'", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Backend Server Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Backend Gateway Server URL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Android Emulator uses 10.0.2.2:8000. Physical devices should use computer's Wi-Fi IP or production domain.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("Server API Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FintechGreen,
                            focusedLabelColor = FintechGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            repository.updateBackendUrl(backendUrl.trim())
                            Toast.makeText(context, "Server URL updated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Save Server URL", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Background & Battery Optimization
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = FintechGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Background Reliability",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "To guarantee the soundbox speaks even when phone screen is locked or another app is open, exempt this app from Android battery restrictions.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Open Battery Optimization Settings", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

