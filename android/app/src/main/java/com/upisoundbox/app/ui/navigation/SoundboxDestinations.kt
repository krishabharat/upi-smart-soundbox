package com.upisoundbox.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object MyQR : Screen("my_qr", "My QR")
    object ReceivedHistory : Screen("received_history", "Received History")
}

