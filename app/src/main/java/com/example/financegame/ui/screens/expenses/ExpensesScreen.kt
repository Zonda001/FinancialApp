package com.example.financegame.ui.screens.expenses

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.data.local.database.entities.*
import com.example.financegame.data.settings.SettingsDataStore
import com.example.financegame.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import java.io.File

// ======================== ОСНОВНИЙ ЕКРАН ========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpenseViewModel = viewModel()
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val monthExpenses by viewModel.currentMonthExpenses.collectAsState()
    val monthIncome by viewModel.currentMonthIncome.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val expenseLimit by viewModel.expenseLimit.collectAsState()

    // OCR стан
    val isProcessingReceipt by viewModel.isProcessingReceipt.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()
    val ocrError by viewModel.ocrError.collectAsState()

    var showLimitDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currency by remember { mutableStateOf("грн") }

    // ✅ Створюємо URI для фото з камери
    val photoUri = remember {
        val photoFile = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // ✅ CAMERA LAUNCHER - TakePicture замість TakePicturePreview
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        println("📷 Camera result: success=$success")
        if (success) {
            try {
                val inputStream = context.contentResolver.openInputStream(photoUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    println("📷 Processing receipt image from camera")
                    viewModel.processReceiptImage(bitmap)
                } else {
                    println("❌ Failed to decode bitmap from camera")
                }
            } catch (e: Exception) {
                println("❌ Error processing camera image: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("❌ Camera was cancelled or failed")
        }
    }

    // ✅ PERMISSION LAUNCHER - запит дозволу камери
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        println("📷 Camera permission granted: $isGranted")
        if (isGranted) {
            // Дозвіл надано - відкриваємо камеру
            cameraLauncher.launch(photoUri)
        } else {
            println("❌ Camera permission denied")
        }
    }


    // ✅ GALLERY LAUNCHER
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        println("🖼️ Gallery result received: $uri")
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    println("🖼️ Processing receipt image from gallery")
                    viewModel.processReceiptImage(bitmap)
                } else {
                    println("❌ Failed to decode bitmap from URI")
                }
            } catch (e: Exception) {
                println("❌ Error processing gallery image: ${e.message}")
                e.printStackTrace()
            }
        }
    }

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
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка сканування чека
                ReceiptScannerButton(
                    onCameraClick = {
                        println("📷 Camera button clicked")
                        // Перевіряємо чи надано дозвіл
                        when (PackageManager.PERMISSION_GRANTED) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) -> {
                                // Дозвіл вже надано - відкриваємо камеру
                                cameraLauncher.launch(photoUri)
                            }
                            else -> {
                                // Запитуємо дозвіл
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        }
                    },
                    onGalleryClick = {
                        println("🖼️ Gallery button clicked")
                        galleryLauncher.launch("image/*")
                    }
                )

                // Звичайна кнопка додавання
                FloatingActionButton(
                    onClick = { viewModel.showAddExpenseDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Додати витрату",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
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

            Spacer(modifier = Modifier.height(8.dp))

            ExpenseLimitCard(
                currentExpenses = monthExpenses,
                expenseLimit = expenseLimit,
                currency = currency,
                onSetLimit = { showLimitDialog = true }
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

    // Діалог звичайного додавання витрат
    if (showAddDialog) {
        AddExpenseDialog(
            currency = currency,
            onDismiss = { viewModel.hideAddExpenseDialog() },
            onConfirm = { amount, category, type, description ->
                viewModel.addExpense(amount, category, type, description)
            }
        )
    }

    // Діалог встановлення ліміту
    if (showLimitDialog) {
        SetExpenseLimitDialog(
            currentLimit = expenseLimit,
            currency = currency,
            onDismiss = { showLimitDialog = false },
            onConfirm = { newLimit ->
                viewModel.setExpenseLimit(newLimit)
                showLimitDialog = false
            }
        )
    }

    // Діалог обробки чека
    ReceiptProcessingDialog(
        isProcessing = isProcessingReceipt,
        onDismiss = { }
    )

    // Діалог результату OCR
    ocrResult?.let { result ->
        println("✅ OCR Result received: ${result.success}")
        ReceiptResultDialog(
            receiptData = result,
            currency = currency,
            onConfirm = { category ->
                viewModel.addExpenseFromReceipt(result, category)
            },
            onDismiss = {
                viewModel.clearOcrResult()
            }
        )
    }

    // Діалог помилки OCR
    ocrError?.let { error ->
        println("❌ OCR Error: $error")
        ReceiptErrorDialog(
            error = error,
            onDismiss = {
                viewModel.clearOcrResult()
            }
        )
    }
}

// ======================== BALANCE CARD ========================
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
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Баланс цього місяця",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatCurrency(balance, currency),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
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
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            formatCurrency(income, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
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
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            formatCurrency(expenses, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ======================== EXPENSE LIMIT CARD ========================
@Composable
fun ExpenseLimitCard(
    currentExpenses: Double,
    expenseLimit: Double,
    currency: String,
    onSetLimit: () -> Unit
) {
    val progress = if (expenseLimit > 0) {
        (currentExpenses / expenseLimit).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressColor = when {
        progress < 0.5f -> MaterialTheme.colorScheme.tertiary
        progress < 0.75f -> MaterialTheme.colorScheme.secondary
        progress < 0.9f -> AccentOrange
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onSetLimit() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Ліміт витрат",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (expenseLimit > 0) {
                        Text(
                            "${formatCurrency(currentExpenses, currency)} з ${formatCurrency(expenseLimit, currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            "Натисніть щоб встановити",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onSetLimit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Змінити ліміт",
                        tint = TextSecondary
                    )
                }
            }

            if (expenseLimit > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        progressColor,
                                        progressColor.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(progress * 100).toInt()}% використано",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (expenseLimit > currentExpenses) {
                        Text(
                            "Залишилось: ${formatCurrency(expenseLimit - currentExpenses, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            "Перевищено на ${formatCurrency(currentExpenses - expenseLimit, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ======================== SET EXPENSE LIMIT DIALOG ========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetExpenseLimitDialog(
    currentLimit: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var limitText by remember {
        mutableStateOf(if (currentLimit > 0) currentLimit.toInt().toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Встановити ліміт витрат",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    "Вкажіть максимальну суму витрат на місяць. Прогрес бар допоможе контролювати ваші витрати.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Ліміт ($currency)") },
                    leadingIcon = {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "💡 Підказка: встановіть реалістичний ліміт відповідно до ваших доходів",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (limit != null && limit > 0) {
                        onConfirm(limit)
                    }
                },
                enabled = limitText.toDoubleOrNull() != null && limitText.toDoubleOrNull()!! > 0
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            Row {
                if (currentLimit > 0) {
                    TextButton(
                        onClick = {
                            onConfirm(0.0)
                        }
                    ) {
                        Text("Видалити ліміт", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Скасувати")
                }
            }
        }
    )
}

// ======================== EXPENSE CARD ========================
@Composable
fun ExpenseCard(expense: Expense, currency: String, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val displayCategory = if (expense.type == ExpenseType.INCOME) "Дохід" else expense.category
    val categoryIcon = if (expense.type == ExpenseType.INCOME) "💰" else getCategoryIcon(expense.category)
    val categoryColor = if (expense.type == ExpenseType.INCOME) QuestActiveColor else getCategoryColor(expense.category)

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
                        categoryIcon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        displayCategory,
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
            title = { Text("Видалити ${if (expense.type == ExpenseType.INCOME) "дохід" else "витрату"}?") },
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

// ======================== EMPTY PLACEHOLDER ========================
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

// ======================== ADD EXPENSE DIALOG ========================
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

                if (selectedType == ExpenseType.EXPENSE) {
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
                }

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
                                val finalCategory = if (selectedType == ExpenseType.INCOME) "Дохід" else selectedCategory
                                onConfirm(amountValue, finalCategory, selectedType, description)
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

/// ======================== RECEIPT SCANNER BUTTON ========================
@Composable
fun ReceiptScannerButton(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { showMenu = true },
            containerColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Сканувати чек",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("📷 Зробити фото") },
                onClick = {
                    showMenu = false
                    onCameraClick()
                },
                leadingIcon = {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                }
            )

            DropdownMenuItem(
                text = { Text("🖼️ Вибрати з галереї") },
                onClick = {
                    showMenu = false
                    onGalleryClick()
                },
                leadingIcon = {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                }
            )
        }
    }
}
// ======================== RECEIPT PROCESSING DIALOG ========================
@Composable
fun ReceiptProcessingDialog(
    isProcessing: Boolean,
    onDismiss: () -> Unit
) {
    if (isProcessing) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Розпізнаємо чек...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Це може зайняти кілька секунд",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ======================== RECEIPT RESULT DIALOG ========================
// ======================== RECEIPT RESULT DIALOG ========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptResultDialog(
    receiptData: com.example.financegame.data.api.HuggingFaceOcrService.ReceiptData,
    currency: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(
            com.example.financegame.data.api.HuggingFaceOcrService().suggestCategory(receiptData.merchantName)
        )
    }
    var showCategoryMenu by remember { mutableStateOf(false) }

    // ✅ ВИПРАВЛЕНО: Використовуємо "До сплати" як основну суму
    val finalAmount = receiptData.doSplaty?.replace(",", ".")?.toDoubleOrNull()
        ?: receiptData.totalAmount

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = QuestCompletedColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Чек розпізнано! 🚀",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (receiptData.merchantName != null) {
                            Text(
                                receiptData.merchantName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ ПОКАЗУЄМО ЛИШЕ ЗАГАЛЬНУ СУМУ
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Основна сума "До сплати"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "До сплати:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${String.format("%.2f", finalAmount)} $currency",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Додаткова інформація (якщо є)
                        if (receiptData.pdv != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "ПДВ:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    "${receiptData.pdv} $currency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (receiptData.discount != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Знижка:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    "${receiptData.discount} $currency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QuestCompletedColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Вибір категорії
                Text(
                    "Категорія:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категорія витрат") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu)
                        },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Інфо про Hugging Face
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚀", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Розпізнано за допомогою Hugging Face OCR API",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Скасувати")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(selectedCategory) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuestCompletedColor
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Додати витрату")
                    }
                }
            }
        }
    }
}

// ======================== RECEIPT ERROR DIALOG ========================
@Composable
fun ReceiptErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Помилка розпізнавання")
        },
        text = {
            Text(error)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Зрозуміло")
            }
        }
    )
}

// ======================== UTILITY FUNCTIONS ========================
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