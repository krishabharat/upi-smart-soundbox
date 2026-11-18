package com.upisoundbox.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.upisoundbox.app.data.repository.SoundboxRepository
import com.upisoundbox.app.ui.screens.*

object SoundboxDestinations {
    const val WELCOME = "welcome"
    const val REGISTRATION = "registration"
    const val HOME = "home"
    const val QR = "qr"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun SoundboxNavGraph(
    repository: SoundboxRepository,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (repository.preferences.isRegistered) {
        SoundboxDestinations.HOME
    } else {
        SoundboxDestinations.WELCOME
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(SoundboxDestinations.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(SoundboxDestinations.REGISTRATION)
                }
            )
        }

        composable(SoundboxDestinations.REGISTRATION) {
            MerchantRegistrationScreen(
                repository = repository,
                onRegistrationSuccess = {
                    navController.navigate(SoundboxDestinations.HOME) {
                        popUpTo(SoundboxDestinations.WELCOME) { inclusive = true }
                        popUpTo(SoundboxDestinations.REGISTRATION) { inclusive = true }
                    }
                },
                onNavigateBack = if (repository.preferences.isRegistered) {
                    { navController.popBackStack() }
                } else null
            )
        }

        composable(SoundboxDestinations.HOME) {
            HomeDashboardScreen(
                repository = repository,
                onNavigateToQr = { navController.navigate(SoundboxDestinations.QR) },
                onNavigateToHistory = { navController.navigate(SoundboxDestinations.HISTORY) },
                onNavigateToSettings = { navController.navigate(SoundboxDestinations.SETTINGS) }
            )
        }

        composable(SoundboxDestinations.QR) {
            MyQRCodeScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(SoundboxDestinations.HISTORY) {
            PaymentHistoryScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(SoundboxDestinations.SETTINGS) {
            SettingsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onEditMerchant = { navController.navigate(SoundboxDestinations.REGISTRATION) }
            )
        }
    }
}

