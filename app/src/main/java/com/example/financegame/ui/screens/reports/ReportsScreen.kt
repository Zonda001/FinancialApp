package com.example.financegame.ui.screens.reports

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.data.settings.SettingsDataStore
import com.example.financegame.ui.screens.expenses.getCategoryColor
import com.example.financegame.ui.screens.expenses.getCategoryIcon
import com.example.financegame.ui.theme.*
import com.example.financegame.ui.theme.TextPrimary
import com.example.financegame.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportViewModel = viewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val periodReport by viewModel.periodReport.collectAsState()
    val expenses by viewModel.expensesForPeriod.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currency by remember { mutableStateOf("грн") }

    LaunchedEffect(Unit) {
        scope.launch {
            val settingsDataStore = SettingsDataStore(context)
            currency = settingsDataStore.currencyFlow.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Звіти та аналітика", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.height(90.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }

            periodReport?.let { report ->
                item {
                    OverallStatsCard(report = report, currency = currency)
                }

                if (report.categoryBreakdown.isNotEmpty()) {
                    item {
                        Text(
                            "Витрати по категоріях",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(report.categoryBreakdown) { categoryExpense ->
                        CategoryExpenseCard(
                            categoryExpense = categoryExpense,
                            totalExpenses = report.totalExpenses,
                            currency = currency,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    item {
                        EmptyReportPlaceholder()
                    }
                }

                item {
                    AdditionalStatsCard(
                        dailyAverage = report.dailyAverage,
                        transactionCount = expenses.size,
                        currency = currency
                    )
                }
            } ?: item {
                EmptyReportPlaceholder()
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit
) {
    // Створюємо NestedScrollConnection, який НЕ передає горизонтальний скрол далі
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Споживаємо залишковий горизонтальний скрол
                return Offset(available.x, 0f)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Споживаємо залишковий горизонтальний fling
                return Velocity(available.x, 0f)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Період",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.nestedScroll(nestedScrollConnection)
        ) {
            items(ReportPeriod.values()) { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                    label = {
                        Text(
                            period.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = if (selectedPeriod == period) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun OverallStatsCard(report: PeriodReport, currency: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Загальна статистика",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Баланс",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        formatCurrency(report.balance, currency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (report.balance >= 0) QuestActiveColor else AccentRed
                    )
                }

                Icon(
                    if (report.balance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (report.balance >= 0) QuestActiveColor else AccentRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    title = "Доходи",
                    value = formatCurrency(report.totalIncome, currency),
                    icon = Icons.Default.ArrowUpward,
                    color = QuestActiveColor
                )

                VerticalDivider(
                    modifier = Modifier.height(60.dp),
                    thickness = 1.dp
                )

                StatColumn(
                    title = "Витрати",
                    value = formatCurrency(report.totalExpenses, currency),
                    icon = Icons.Default.ArrowDownward,
                    color = AccentRed
                )
            }
        }
    }
}

@Composable
fun StatColumn(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CategoryExpenseCard(
    categoryExpense: CategoryExpense,
    totalExpenses: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(categoryExpense.category)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        getCategoryIcon(categoryExpense.category),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        categoryExpense.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${categoryExpense.percentage.toInt()}% від витрат",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = categoryExpense.percentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = categoryColor,
                        trackColor = categoryColor.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                formatCurrency(categoryExpense.amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = categoryColor
            )
        }
    }
}

@Composable
fun AdditionalStatsCard(
    dailyAverage: Double,
    transactionCount: Int,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Додаткова статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Середнє за день",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        formatCurrency(dailyAverage, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Транзакції",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        transactionCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyReportPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Немає даних за цей період",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary
            )
            Text(
                "Додайте витрати щоб побачити звіт",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatCurrency(amount: Double, currency: String): String {
    return String.format("%.2f %s", amount, currency)
}