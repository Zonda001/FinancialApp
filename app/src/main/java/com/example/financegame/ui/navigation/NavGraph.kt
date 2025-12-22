package com.example.financegame.ui.navigation

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.financegame.ui.screens.achievements.AchievementsScreen
import com.example.financegame.ui.screens.expenses.ExpensesScreen
import com.example.financegame.ui.screens.expenses.ExpenseViewModel
import com.example.financegame.ui.screens.profile.ProfileScreen
import com.example.financegame.ui.screens.quests.QuestsScreen
import com.example.financegame.ui.screens.reports.ReportsScreen
import com.example.financegame.ui.screens.settings.SettingsScreen
import com.example.financegame.ui.screens.settings.SettingsViewModel
import com.example.financegame.ui.screens.trading.TradingScreen
import kotlinx.coroutines.launch

// Маршрути навігації
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Profile : Screen("profile", "Профіль", Icons.Default.Person)
    object Trading : Screen("trading", "Трейдинг", Icons.Default.ShowChart)
    object Expenses : Screen("expenses", "Витрати", Icons.Default.AccountBalanceWallet)
    object Quests : Screen("quests", "Квести", Icons.Default.EmojiEvents)
    object Achievements : Screen("achievements", "Досягнення", Icons.Default.Stars)
    object Reports : Screen("reports", "Звіти", Icons.Default.BarChart)
    object Settings : Screen("settings", "Налаштування", Icons.Default.Settings)
}

@Composable
fun MainScreenWithLaunchers(
    settingsViewModel: SettingsViewModel,
    expenseViewModel: ExpenseViewModel
) {
    // Launchers тепер в ExpensesScreen, тут вони не потрібні
    MainScreen(settingsViewModel = settingsViewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settingsViewModel: SettingsViewModel? = null
) {
    val screens = listOf(
        Screen.Profile,
        Screen.Trading,
        Screen.Expenses,
        Screen.Quests,
        Screen.Achievements,
        Screen.Reports,
        Screen.Settings
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { screens.size }
    )

    val scope = rememberCoroutineScope()
    var isPagerScrollEnabled by remember { mutableStateOf(true) }

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
                        label = { 
                            Text(
                                text = screen.title,
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    hyphens = Hyphens.Auto
                                )
                            )
                        },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
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
    ) { _ ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = isPagerScrollEnabled,
            pageSpacing = 0.dp,
            key = { it }
        ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
                    when (page) {
                        0 -> ProfileScreen()
                        1 -> TradingScreen()
                        2 -> ExpensesScreen()
                        3 -> QuestsScreen(
                            onNavigateToReports = { scope.launch { pagerState.scrollToPage(5) } },
                            onNavigateToSettings = { scope.launch { pagerState.scrollToPage(6) } },
                            onNavigateToAchievements = { scope.launch { pagerState.scrollToPage(4) } },
                            onNavigateToProfile = { scope.launch { pagerState.scrollToPage(0) } }
                        )
                        4 -> AchievementsScreen()
                        5 -> ReportsScreen()
                        6 -> {
                            if (settingsViewModel != null) {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onThemeChanged = { }
                                )
                            } else {
                                SettingsScreen(onThemeChanged = { })
                            }
                        }
                    }
                }
            }
        }
    }
