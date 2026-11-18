package com.upisoundbox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.ui.theme.BackgroundSoft
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.FintechGreenLight
import com.upisoundbox.app.ui.theme.SurfaceWhite
import com.upisoundbox.app.ui.theme.TextPrimary
import com.upisoundbox.app.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundSoft,
        bottomBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Button(
                        onClick = onGetStarted,
                        colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Set Up Merchant Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
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
            Spacer(modifier = Modifier.height(48.dp))

            // App Brand Logo Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(FintechGreenLight)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Soundbox Icon",
                    tint = FintechGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "UPI Smart Soundbox",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Turn your Android phone into an instant digital UPI voice payment soundbox. Zero hardware rental cost.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Value props
            FeatureHighlightCard(
                icon = Icons.Default.VolumeUp,
                title = "Automatic Voice Announcements",
                description = "Clear English audio confirms received amounts like 'Payment received, 100 rupees.'"
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureHighlightCard(
                icon = Icons.Default.QrCode,
                title = "Instant UPI QR Generator",
                description = "Customers scan and pay directly using Google Pay, PhonePe, Paytm, or BHIM."
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureHighlightCard(
                icon = Icons.Default.Security,
                title = "Verified Payment Engine",
                description = "Only authentic, bank-verified transactions trigger voice announcements. Zero fake receipts."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FintechGreenLight)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FintechGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = TextSecondary
                )
            }
        }
    }
}

