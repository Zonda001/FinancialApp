package com.example.financegame.ui.screens.expenses

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.data.local.database.entities.*
import com.example.financegame.data.settings.SettingsDataStore
import com.example.financegame.ui.theme.*
import com.example.financegame.ui.theme.TextPrimary
import com.example.financegame.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpenseViewModel = viewModel()
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val monthExpenses by viewModel.currentMonthExpenses.collectAsState()
    val monthIncome by viewModel.currentMonthIncome.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currency by remember { mutableStateOf("грн") }

    LaunchedEffect(Unit) {
        scope.launch {
            val settingsDataStore = SettingsDataStore(context)
            currency = settingsDataStore.currencyFlow.first()
        }
    }

    val balance = monthIncome - monthExpenses

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мої витрати", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = TextLight
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddExpenseDialog() },
                containerColor = AccentOrange
            ) {
                Icon(Icons.Default.Add, contentDescription = "Додати витрату", tint = TextLight)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BalanceCard(
                income = monthIncome,
                expenses = monthExpenses,
                balance = balance,
                currency = currency
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (expenses.isEmpty()) {
                EmptyExpensesPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseCard(
                            expense = expense,
                            currency = currency,
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            currency = currency,
            onDismiss = { viewModel.hideAddExpenseDialog() },
            onConfirm = { amount, category, type, description ->
                viewModel.addExpense(amount, category, type, description)
            }
        )
    }
}

@Composable
fun BalanceCard(income: Double, expenses: Double, balance: Double, currency: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryBlue, SecondaryGreen)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Баланс цього місяця",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatCurrency(balance, currency),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = QuestCompletedColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Доходи",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            formatCurrency(income, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextLight
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Витрати",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            formatCurrency(expenses, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: Expense, currency: String, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(expense.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        getCategoryIcon(expense.category),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        expense.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (expense.description.isNotEmpty()) {
                        Text(
                            expense.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        formatDate(expense.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (expense.type == ExpenseType.EXPENSE) "-" else "+"} ${formatCurrency(expense.amount, currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (expense.type == ExpenseType.EXPENSE) AccentRed else QuestActiveColor
                )
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Видалити",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити витрату?") },
            text = { Text("Ця дія незворотна") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Видалити", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
fun EmptyExpensesPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Ще немає витрат",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary
            )
            Text(
                "Натисніть + щоб додати першу витрату",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, ExpenseType, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DefaultCategories.categories[0].name) }
    var selectedType by remember { mutableStateOf(ExpenseType.EXPENSE) }
    var description by remember { mutableStateOf("") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Додати транзакцію",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == ExpenseType.EXPENSE,
                        onClick = { selectedType = ExpenseType.EXPENSE },
                        label = { Text("Витрата") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == ExpenseType.INCOME,
                        onClick = { selectedType = ExpenseType.INCOME },
                        label = { Text("Дохід") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сума ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категорія") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        DefaultCategories.categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(category.icon)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.name)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category.name
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис (опційно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Скасувати")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue != null && amountValue > 0) {
                                onConfirm(amountValue, selectedCategory, selectedType, description)
                            }
                        },
                        enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
                    ) {
                        Text("Додати")
                    }
                }
            }
        }
    }
}

fun formatCurrency(amount: Double, currency: String): String {
    return String.format("%.2f %s", amount, currency)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getCategoryColor(category: String): androidx.compose.ui.graphics.Color {
    return when (category) {
        "Їжа" -> FoodColor
        "Транспорт" -> TransportColor
        "Розваги" -> EntertainmentColor
        "Здоров'я" -> HealthColor
        "Комунальні" -> UtilitiesColor
        "Одяг" -> ClothingColor
        "Освіта" -> EducationColor
        else -> OtherColor
    }
}

fun getCategoryIcon(category: String): String {
    return DefaultCategories.categories.find { it.name == category }?.icon ?: "💰"
}