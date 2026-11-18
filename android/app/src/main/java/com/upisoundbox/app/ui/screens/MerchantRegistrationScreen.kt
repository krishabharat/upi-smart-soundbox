package com.upisoundbox.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.ui.theme.BackgroundSoft
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.SurfaceWhite
import com.upisoundbox.app.ui.theme.TextPrimary
import com.upisoundbox.app.ui.theme.TextSecondary
import com.upisoundbox.app.utils.UPIUriGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantRegistrationScreen(
    repository: SoundboxRepository,
    onRegistrationSuccess: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentMerchant = remember { repository.preferences.getMerchant() }

    var businessName by remember { mutableStateOf(currentMerchant?.businessName ?: "") }
    var upiId by remember { mutableStateOf(currentMerchant?.upiId ?: "") }
    var mobileNumber by remember { mutableStateOf(currentMerchant?.mobileNumber ?: "") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentMerchant != null) "Edit Merchant Profile" else "Merchant Registration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Your Store Details",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your UPI ID will be used to generate your payment QR code and link verified voice notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Business Name Input
            OutlinedTextField(
                value = businessName,
                onValueChange = {
                    businessName = it
                    errorMessage = null
                },
                label = { Text("Business / Store Name *") },
                placeholder = { Text("e.g. Sharma Kirana Store") },
                leadingIcon = {
                    Icon(Icons.Default.Business, contentDescription = null, tint = FintechGreen)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FintechGreen,
                    focusedLabelColor = FintechGreen
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // UPI ID Input
            OutlinedTextField(
                value = upiId,
                onValueChange = {
                    upiId = it.trim().lowercase()
                    errorMessage = null
                },
                label = { Text("Merchant UPI ID / VPA *") },
                placeholder = { Text("e.g. sharma.store@okhdfcbank") },
                leadingIcon = {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = FintechGreen)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FintechGreen,
                    focusedLabelColor = FintechGreen
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Examples: store@okhdfcbank, name@oksbi, shop@paytm, retail@icici",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Mobile Number (optional)
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = {
                    mobileNumber = it.filter { char -> char.isDigit() }.take(10)
                },
                label = { Text("Mobile Number (Optional)") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = FintechGreen)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FintechGreen,
                    focusedLabelColor = FintechGreen
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    val name = businessName.trim()
                    val upi = upiId.trim()

                    if (name.length < 2) {
                        errorMessage = "Please enter a valid store name (at least 2 characters)."
                        return@Button
                    }
                    if (!UPIUriGenerator.isValidUpiId(upi)) {
                        errorMessage = "Please enter a valid UPI ID (e.g. store@okhdfcbank)."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null

                    coroutineScope.launch {
                        val result = repository.registerMerchant(
                            businessName = name,
                            upiId = upi,
                            mobile = mobileNumber.ifBlank { null }
                        )

                        isSubmitting = false
                        result.fold(
                            onSuccess = {
                                Toast.makeText(context, "Merchant registered successfully!", Toast.LENGTH_SHORT).show()
                                onRegistrationSuccess()
                            },
                            onFailure = { error ->
                                // If network is temporarily unreachable, still save locally so shopkeeper can use app offline
                                repository.preferences.saveMerchant(
                                    com.upisoundbox.app.data.model.Merchant(
                                        id = "mer_local_${System.currentTimeMillis()}",
                                        businessName = name,
                                        upiId = upi,
                                        mobileNumber = mobileNumber.ifBlank { null }
                                    )
                                )
                                Toast.makeText(context, "Merchant profile saved on device", Toast.LENGTH_SHORT).show()
                                onRegistrationSuccess()
                            }
                        )
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Save Merchant Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

