package com.upisoundbox.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.data.model.Merchant
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.service.SoundboxForegroundService
import com.upisoundbox.app.ui.components.QRCodeComposable
import com.upisoundbox.app.ui.theme.*
import com.upisoundbox.app.utils.QRCodeBitmapGenerator
import com.upisoundbox.app.utils.UPIUriGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MyQRScreen(
    repository: SoundboxRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val savedMerchant = remember { repository.preferences.getMerchant() }

    var businessNameInput by remember { mutableStateOf(savedMerchant?.businessName ?: "") }
    var upiIdInput by remember { mutableStateOf(savedMerchant?.upiId ?: "") }
    var isEditMode by remember { mutableStateOf(savedMerchant == null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val currentMerchant = remember { repository.preferences.getMerchant() }
    val displayBusinessName = currentMerchant?.businessName ?: businessNameInput
    val displayUpiId = currentMerchant?.upiId ?: upiIdInput

    val upiUri = remember(displayBusinessName, displayUpiId) {
        if (displayBusinessName.isNotBlank() && displayUpiId.isNotBlank()) {
            UPIUriGenerator.buildUpiUri(
                upiId = displayUpiId,
                businessName = displayBusinessName,
                transactionNote = "Payment to $displayBusinessName"
            )
        } else ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF0FDF4), BackgroundSoft, Color.White)
                )
            )
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Top App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UPI Smart Soundbox",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Digital Payment Soundbox & QR",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }

            if (!isEditMode && savedMerchant != null) {
                // Edit details button
                TextButton(
                    onClick = { isEditMode = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = FintechGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit Details",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Notification Access Banner for Real Money Detection
        val isListenerGranted = remember {
            androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }
        if (!isListenerGranted) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔔", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Detect Real Money Payments",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Allow access to read incoming GPay, PhonePe & Bank alerts",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==========================================
        // STATE 1: MERCHANT FORM (If editing/not set)
        // ==========================================
        if (isEditMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x2600875A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FintechGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = FintechGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Setup Your Store QR",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Enter details to generate your payment QR code",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Business Name Input
                    OutlinedTextField(
                        value = businessNameInput,
                        onValueChange = {
                            businessNameInput = it
                            validationError = null
                        },
                        label = { Text("Business Name *", fontSize = 13.sp) },
                        placeholder = { Text("e.g. Sharma Kirana Store", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null, tint = FintechGreen)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FintechGreen,
                            focusedLabelColor = FintechGreen,
                            unfocusedBorderColor = CardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // UPI ID Input
                    OutlinedTextField(
                        value = upiIdInput,
                        onValueChange = {
                            upiIdInput = it.trim().lowercase()
                            validationError = null
                        },
                        label = { Text("UPI ID (VPA) *", fontSize = 13.sp) },
                        placeholder = { Text("e.g. sharma@okhdfcbank", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = FintechGreen)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FintechGreen,
                            focusedLabelColor = FintechGreen,
                            unfocusedBorderColor = CardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Supports: @okhdfcbank, @oksbi, @paytm, @icici, @upi, etc.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = validationError ?: "",
                            color = StatusError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // One Large Button: Generate My QR
                    Button(
                        onClick = {
                            val name = businessNameInput.trim()
                            val upi = upiIdInput.trim()

                            if (name.length < 2) {
                                validationError = "Please enter a valid business name."
                                return@Button
                            }
                            if (!UPIUriGenerator.isValidUpiId(upi)) {
                                validationError = "Please enter a valid UPI ID (e.g. name@bank)."
                                return@Button
                            }

                            // Save locally in DataStore / Preferences
                            val merchant = Merchant(
                                id = savedMerchant?.id ?: "mer_${System.currentTimeMillis()}",
                                businessName = name,
                                upiId = upi
                            )
                            repository.preferences.saveMerchant(merchant)

                            // Also sync in background to server and start soundbox service
                            coroutineScope.launch {
                                repository.registerMerchant(name, upi, null)
                                SoundboxForegroundService.startService(context)
                            }

                            isEditMode = false
                            Toast.makeText(context, "QR Code Generated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Generate My QR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // STATE 2: GENERATED QR DISPLAY (Scan & Pay)
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x15000000), spotColor = Color(0x3000875A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // "Scan & Pay" Heading Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(FintechGreenLight)
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SCAN & PAY",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FintechGreen,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // QR Code Display
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.5.dp, CardBorder, RoundedCornerShape(20.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QRCodeComposable(
                            content = upiUri,
                            sizeDp = 224.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show Business Name below QR
                    Text(
                        text = displayBusinessName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // UPI ID hidden for privacy as requested by merchant
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FintechGreenLight,
                        border = BorderStroke(1.dp, FintechGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔒 UPI Verified Merchant",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Accepted Here UPI Logos Text
                    Text(
                        text = "BHIM • Google Pay • PhonePe • Paytm • Any UPI App",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Share QR & Download QR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Share QR Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val bitmap = withContext(Dispatchers.Default) {
                                QRCodeBitmapGenerator.generateQrBitmap(upiUri, size = 800)
                            }
                            val shareUri = QRCodeBitmapGenerator.saveBitmapForSharing(context, bitmap)
                            if (shareUri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    putExtra(Intent.EXTRA_TEXT, "Scan & Pay using any UPI app to $displayBusinessName ($displayUpiId)")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Payment QR Code"))
                            } else {
                                Toast.makeText(context, "Could not share QR", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share QR",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Download QR Button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val bitmap = withContext(Dispatchers.Default) {
                                QRCodeBitmapGenerator.generateQrBitmap(upiUri, size = 1024)
                            }
                            val fileName = "UPI_QR_${displayBusinessName.replace(" ", "_")}"
                            val success = QRCodeBitmapGenerator.saveBitmapToGallery(context, bitmap, fileName)
                            if (success) {
                                Toast.makeText(context, "QR saved to Pictures/UPI_Soundbox!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to download QR", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(CardBorder, CardBorder))),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = FintechGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

