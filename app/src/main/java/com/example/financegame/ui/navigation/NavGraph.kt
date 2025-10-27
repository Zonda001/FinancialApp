package com.example.financegame.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

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
    // Список екранів у правильному порядку
    val screens = listOf(
        Screen.Profile,
        Screen.Expenses,
        Screen.Quests,
        Screen.Achievements,
        Screen.Reports,
        Screen.Settings
    )

    // Стан для HorizontalPager
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { screens.size }
    )

    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                // Використовуємо scrollToPage для миттєвого переходу
                                pagerState.scrollToPage(index)
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
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                userScrollEnabled = true,
                pageSpacing = 0.dp,
                key = { it }
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> ProfileScreen()
                        1 -> ExpensesScreen()
                        2 -> QuestsScreen(
                            onNavigateToReports = {
                                scope.launch {
                                    pagerState.scrollToPage(4)
                                }
                            },
                            onNavigateToSettings = {
                                scope.launch {
                                    pagerState.scrollToPage(5)
                                }
                            },
                            onNavigateToAchievements = {
                                scope.launch {
                                    pagerState.scrollToPage(3)
                                }
                            },
                            onNavigateToProfile = {
                                scope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            }
                        )
                        3 -> AchievementsScreen()
                        4 -> ReportsScreen()
                        5 -> {
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
        }
    }
}