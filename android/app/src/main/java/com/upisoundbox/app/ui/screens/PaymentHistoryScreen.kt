package com.upisoundbox.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
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
import com.upisoundbox.app.data.local.TransactionEntity
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.ui.theme.BackgroundSoft
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.FintechGreenLight
import com.upisoundbox.app.ui.theme.SurfaceWhite
import com.upisoundbox.app.ui.theme.TextPrimary
import com.upisoundbox.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    repository: SoundboxRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val merchant = remember { repository.preferences.getMerchant() }
    val transactions by repository.getAllLocalTransactions().collectAsState(initial = emptyList())
    val todayAmount by repository.getTodayTotalAmount().collectAsState(initial = 0.0)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)

    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshHistory() {
        if (merchant == null) return
        isRefreshing = true
        coroutineScope.launch {
            val result = repository.fetchTransactions(merchant.id)
            isRefreshing = false
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "History synced", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, "Showing offline history", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    Scaffold(
        containerColor = BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payment History",
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
                actions = {
                    IconButton(onClick = { refreshHistory() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isRefreshing) FintechGreen else TextSecondary
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
        ) {
            // Today's Verified Collection Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TODAY'S VERIFIED TOTAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedAmount = remember(todayAmount) {
                            val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
                            format.minimumFractionDigits = 2
                            format.maximumFractionDigits = 2
                            format.format(todayAmount)
                        }
                        Text(
                            text = "₹ $formattedAmount",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FintechGreenLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$todayCount Received",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen
                        )
                    }
                }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No payments received yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When customers scan your QR code and make a payment, it will appear here instantly.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRowItem(tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRowItem(tx: TransactionEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(14.dp),
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
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(FintechGreenLight)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = FintechGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = tx.payerName ?: "UPI Customer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tx.bankRrn?.let { "UTR: $it" } ?: tx.orderId,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val dateFormatted = remember(tx.createdAt) {
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        sdf.format(Date(tx.createdAt))
                    }
                    Text(
                        text = dateFormatted,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+ ₹${String.format(Locale.US, "%.2f", tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = FintechGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(FintechGreenLight)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VERIFIED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FintechGreen
                    )
                }
            }
        }
    }
}

