package com.upisoundbox.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
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
import com.upisoundbox.app.data.local.TransactionEntity
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.service.TextToSpeechManager
import com.upisoundbox.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceivedHistoryScreen(
    repository: SoundboxRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val merchant = remember { repository.preferences.getMerchant() }
    val transactions by repository.getAllLocalTransactions().collectAsState(initial = emptyList())
    val todayAmount by repository.getTodayTotalAmount().collectAsState(initial = 0.0)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)

    val ttsManager = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshHistory() {
        if (merchant == null) return
        isRefreshing = true
        coroutineScope.launch {
            repository.fetchTransactions(merchant.id)
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF0FDF4), BackgroundSoft, Color.White)
                )
            )
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Received History",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Verified payment confirmations & voice logs",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = { refreshHistory() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, CardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync",
                    tint = if (isRefreshing) FintechGreen else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Today's Total Received Top Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10000000), spotColor = Color(0x2000875A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S TOTAL RECEIVED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(FintechGreenLight)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$todayCount Verified",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val formattedToday = remember(todayAmount) {
                    val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
                    nf.minimumFractionDigits = 2
                    nf.maximumFractionDigits = 2
                    nf.format(todayAmount)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = FintechGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedToday,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Verified Payment List
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(FintechGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = FintechGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No payments received yet today",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "When customers scan your QR and make a payment, verified transactions will appear here automatically with voice confirmation.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(transactions, key = { it.id }) { tx ->
                    ReceivedPaymentCard(tx)
                }
            }
        }
    }
}

@Composable
private fun ReceivedPaymentCard(tx: TransactionEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(FintechGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = FintechGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    // Payer / Customer Name
                    Text(
                        text = tx.payerName ?: "UPI Customer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Date and Time
                    val (dateStr, timeStr) = remember(tx.createdAt) {
                        val dFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val tFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val d = Date(tx.createdAt)
                        Pair(dFmt.format(d), tFmt.format(d))
                    }
                    Text(
                        text = "$dateStr • $timeStr",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Transaction Reference ID (UTR / RRN)
                    Text(
                        text = "Ref: ${tx.bankRrn ?: tx.orderId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                // Received Amount
                Text(
                    text = "+ ₹${String.format(Locale.US, "%.2f", tx.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FintechGreen
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Payment Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(FintechGreenLight)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VERIFIED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FintechGreen
                    )
                }
            }
        }
    }
}

