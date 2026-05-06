package com.example.pesalens.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object History : Screen("history", "History", Icons.Default.History)
    object Outgoing : Screen("outgoing", "Expenses", Icons.Default.TrendingDown)
    object Incoming : Screen("incoming", "Income", Icons.Default.TrendingUp)
    object AI : Screen("ai", "AI Assistant", Icons.Default.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val navItems = listOf(
    Screen.History,
    Screen.Outgoing,
    Screen.Incoming,
    Screen.AI,
    Screen.Settings
)
