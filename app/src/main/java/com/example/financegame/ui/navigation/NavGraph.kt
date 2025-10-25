package com.example.financegame.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.financegame.ui.screens.achievements.AchievementsScreen
import com.example.financegame.ui.screens.expenses.ExpensesScreen
import com.example.financegame.ui.screens.profile.ProfileScreen
import com.example.financegame.ui.screens.quests.QuestsScreen
import com.example.financegame.ui.screens.reports.ReportsScreen
import com.example.financegame.ui.screens.settings.SettingsScreen
import com.example.financegame.ui.screens.settings.SettingsViewModel

// Маршрути навігації
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Profile : Screen("profile", "Профіль", Icons.Default.Person)
    object Expenses : Screen("expenses", "Витрати", Icons.Default.AccountBalanceWallet)
    object Quests : Screen("quests", "Квести", Icons.Default.EmojiEvents)
    object Achievements : Screen("achievements", "Досягнення", Icons.Default.Stars)
    object Reports : Screen("reports", "Звіти", Icons.Default.BarChart)
    object Settings : Screen("settings", "Налаштування", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(settingsViewModel: SettingsViewModel? = null) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Profile,
        Screen.Expenses,
        Screen.Quests,
        Screen.Achievements,
        Screen.Reports,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Profile.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Expenses.route) { ExpensesScreen() }
            composable(Screen.Quests.route) { QuestsScreen() }
            composable(Screen.Achievements.route) { AchievementsScreen() }
            composable(Screen.Reports.route) { ReportsScreen() }
            composable(Screen.Settings.route) {
                if (settingsViewModel != null) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onThemeChanged = { /* Автоматично оновиться */ }
                    )
                } else {
                    SettingsScreen(
                        onThemeChanged = { /* Автоматично оновиться */ }
                    )
                }
            }
        }
    }
}