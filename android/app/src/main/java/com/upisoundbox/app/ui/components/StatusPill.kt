package com.upisoundbox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.data.remote.ConnectionStatus
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.StatusError
import com.upisoundbox.app.ui.theme.StatusWarning

@Composable
fun StatusPill(
    status: ConnectionStatus,
    isSoundboxEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgColor, text) = when {
        !isSoundboxEnabled -> Triple(Color.Gray, Color(0xFFF1F5F9), "Soundbox OFF")
        status == ConnectionStatus.CONNECTED -> Triple(FintechGreen, Color(0xFFE3FCEF), "Live & Ready")
        status == ConnectionStatus.CONNECTING -> Triple(StatusWarning, Color(0xFFFEF3C7), "Connecting…")
        status == ConnectionStatus.ERROR -> Triple(StatusError, Color(0xFFFEE2E2), "Reconnecting")
        else -> Triple(Color.Gray, Color(0xFFF1F5F9), "Offline")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (!isSoundboxEnabled) Color(0xFF475569) else dotColor
        )
    }
}

