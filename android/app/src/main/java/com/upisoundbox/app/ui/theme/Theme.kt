package com.upisoundbox.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FintechGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = FintechGreenLight,
    onPrimaryContainer = FintechGreenDark,
    secondary = FintechGreenVibrant,
    onSecondary = SurfaceWhite,
    background = BackgroundSoft,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    outline = CardBorder
)

@Composable
fun UPISoundboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Keep light, clean fintech appearance as per design requirements
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

