package com.upisoundbox.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.ui.components.QRCodeComposable
import com.upisoundbox.app.ui.theme.BackgroundSoft
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.FintechGreenLight
import com.upisoundbox.app.ui.theme.SurfaceWhite
import com.upisoundbox.app.ui.theme.TextPrimary
import com.upisoundbox.app.ui.theme.TextSecondary
import com.upisoundbox.app.utils.QRCodeBitmapGenerator
import com.upisoundbox.app.utils.UPIUriGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQRCodeScreen(
    repository: SoundboxRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val merchant = remember { repository.preferences.getMerchant() }
    val businessName = merchant?.businessName ?: "Merchant Store"
    val upiId = merchant?.upiId ?: "store@upi"

    var customAmountText by remember { mutableStateOf("") }
    var isAmountSpecified by remember { mutableStateOf(false) }

    val parsedAmount = customAmountText.toDoubleOrNull()
    val upiUri = remember(businessName, upiId, isAmountSpecified, parsedAmount) {
        UPIUriGenerator.buildUpiUri(
            upiId = upiId,
            businessName = businessName,
            amount = if (isAmountSpecified) parsedAmount else null
        )
    }

    Scaffold(
        containerColor = BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Merchant Payment QR",
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Main Standee Style QR Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(FintechGreenLight)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ACCEPTED HERE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = businessName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = upiId,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // QR Code Display
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QRCodeComposable(
                            content = upiUri,
                            sizeDp = 244.dp
                        )
                    }

                    if (isAmountSpecified && parsedAmount != null && parsedAmount > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Scan to pay ₹$customAmountText",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Supported Payment Logos Text
                    Text(
                        text = "BHIM • Google Pay • PhonePe • Paytm • Any UPI App",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Share & Download Buttons
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
                                    putExtra(Intent.EXTRA_TEXT, "Scan & Pay using UPI to $businessName ($upiId)")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share UPI QR Code"))
                            } else {
                                Toast.makeText(context, "Failed to share QR", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share QR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
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
                            val success = QRCodeBitmapGenerator.saveBitmapToGallery(
                                context,
                                bitmap,
                                fileName = "UPI_QR_${businessName.replace(" ", "_")}"
                            )
                            if (success) {
                                Toast.makeText(context, "QR saved to Pictures/UPI_Soundbox!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to save QR to gallery", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = FintechGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Optional Specific Amount Generator Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Pre-fill Specific Amount",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Generate QR with exact price already filled in",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = isAmountSpecified,
                            onCheckedChange = { isAmountSpecified = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FintechGreen
                            )
                        )
                    }

                    if (isAmountSpecified) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customAmountText,
                            onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Amount (INR ₹)") },
                            placeholder = { Text("e.g. 150") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FintechGreen,
                                focusedLabelColor = FintechGreen
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

