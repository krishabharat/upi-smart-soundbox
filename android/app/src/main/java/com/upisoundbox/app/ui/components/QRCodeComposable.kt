package com.upisoundbox.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.utils.QRCodeBitmapGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QRCodeComposable(
    content: String,
    sizeDp: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    var qrBitmap by remember(content) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(content) { mutableStateOf(true) }

    LaunchedEffect(content) {
        if (content.isNotBlank()) {
            isLoading = true
            val bmp = withContext(Dispatchers.Default) {
                QRCodeBitmapGenerator.generateQrBitmap(content, size = 600)
            }
            qrBitmap = bmp
            isLoading = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = FintechGreen,
                modifier = Modifier.size(36.dp)
            )
        } else {
            qrBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "UPI Payment QR Code",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

