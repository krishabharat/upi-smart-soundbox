package com.upisoundbox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.ui.theme.CardBorder
import com.upisoundbox.app.ui.theme.FintechGreen
import com.upisoundbox.app.ui.theme.FintechGreenLight
import com.upisoundbox.app.ui.theme.SurfaceWhite
import com.upisoundbox.app.ui.theme.TextPrimary
import com.upisoundbox.app.ui.theme.TextSecondary

enum class SoundboxTab {
    MY_QR,
    RECEIVED_HISTORY
}

@Composable
fun MainSoundboxScreen(
    repository: SoundboxRepository,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SoundboxTab.MY_QR) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp)
            ) {
                // Tab 1: My QR
                NavigationBarItem(
                    selected = selectedTab == SoundboxTab.MY_QR,
                    onClick = { selectedTab = SoundboxTab.MY_QR },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == SoundboxTab.MY_QR) Icons.Default.QrCode else Icons.Outlined.QrCode,
                            contentDescription = "My QR",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "My QR",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == SoundboxTab.MY_QR) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FintechGreen,
                        selectedTextColor = FintechGreen,
                        indicatorColor = FintechGreenLight,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                // Tab 2: Received History
                NavigationBarItem(
                    selected = selectedTab == SoundboxTab.RECEIVED_HISTORY,
                    onClick = { selectedTab = SoundboxTab.RECEIVED_HISTORY },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == SoundboxTab.RECEIVED_HISTORY) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Received History",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Received History",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == SoundboxTab.RECEIVED_HISTORY) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FintechGreen,
                        selectedTextColor = FintechGreen,
                        indicatorColor = FintechGreenLight,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                SoundboxTab.MY_QR -> {
                    MyQRScreen(repository = repository)
                }
                SoundboxTab.RECEIVED_HISTORY -> {
                    ReceivedHistoryScreen(repository = repository)
                }
            }
        }
    }
}

