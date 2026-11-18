package com.upisoundbox.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.service.SoundboxForegroundService
import com.upisoundbox.app.service.TextToSpeechManager
import com.upisoundbox.app.ui.components.StatusPill
import com.upisoundbox.app.ui.theme.BackgroundSoft
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.FintechGreenLight
import com.upisoundbox.app.ui.theme.SurfaceWhite
import com.upisoundbox.app.ui.theme.TextPrimary
import com.upisoundbox.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    repository: SoundboxRepository,
    onNavigateToQr: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val merchant = remember { repository.preferences.getMerchant() }
    val connectionStatus by repository.connectionStatus.collectAsState()

    var isSoundboxOn by remember { mutableStateOf(repository.preferences.isSoundboxEnabled) }
    val todayAmount by repository.getTodayTotalAmount().collectAsState(initial = 0.0)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)
    val recentTransactions by repository.getTodayLocalTransactions().collectAsState(initial = emptyList())

    val ttsManager = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    Scaffold(
        containerColor = BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = merchant?.businessName ?: "My Store",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = merchant?.upiId ?: "UPI Soundbox",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Soundbox Master ON/OFF Switch & Live Connection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Soundbox Audio Announcements",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusPill(
                            status = connectionStatus,
                            isSoundboxEnabled = isSoundboxOn
                        )
                    }

                    Switch(
                        checked = isSoundboxOn,
                        onCheckedChange = { isChecked ->
                            isSoundboxOn = isChecked
                            repository.preferences.isSoundboxEnabled = isChecked
                            if (isChecked) {
                                SoundboxForegroundService.startService(context)
                                Toast.makeText(context, "Soundbox enabled and listening", Toast.LENGTH_SHORT).show()
                            } else {
                                SoundboxForegroundService.stopService(context)
                                Toast.makeText(context, "Soundbox audio turned OFF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FintechGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Today's Verified Collection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S VERIFIED COLLECTION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FintechGreenLight)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$todayCount Payments",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val formattedAmount = remember(todayAmount) {
                        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
                        format.minimumFractionDigits = 2
                        format.maximumFractionDigits = 2
                        format.format(todayAmount)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedAmount,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Large QR Code Button (Primary Action)
            Button(
                onClick = onNavigateToQr,
                colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Code",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Show Payment QR Code",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment History & Sound Test Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sound Test Button
                OutlinedButton(
                    onClick = {
                        ttsManager.speakTestSound()
                        Toast.makeText(context, "Playing sound test...", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = FintechGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sound Test",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // Payment History Button
                OutlinedButton(
                    onClick = onNavigateToHistory,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = FintechGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payment History",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Verified Payment Activity
            if (recentTransactions.isNotEmpty()) {
                Text(
                    text = "Recent Verified Payments",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                recentTransactions.take(3).forEach { tx ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(FintechGreenLight)
                                ) {
                                    Text(
                                        text = "₹",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = FintechGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tx.payerName ?: "UPI Customer",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = tx.bankRrn?.let { "UTR: $it" } ?: tx.orderId,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "+ ₹${String.format(Locale.US, "%.2f", tx.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FintechGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

