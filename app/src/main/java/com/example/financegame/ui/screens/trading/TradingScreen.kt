package com.example.financegame.ui.screens.trading

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.data.local.database.entities.*
import com.example.financegame.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    viewModel: TradingViewModel = viewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val activePositions by viewModel.activePositions.collectAsState()
    val closedPositions by viewModel.closedPositions.collectAsState()
    val assetPrices by viewModel.assetPrices.collectAsState()
    val totalProfitLoss by viewModel.totalProfitLoss.collectAsState()
    val winRate by viewModel.winRate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showTradeDialog by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<TradingAsset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📈 Trading Simulator", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPrices() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Оновити",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Статистика
            TradingStatsCard(
                balance = user?.totalPoints ?: 0,
                profitLoss = totalProfitLoss,
                winRate = winRate
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ринок") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Активні (${activePositions.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Історія") }
                )
            }

            // Content
            when (selectedTab) {
                0 -> MarketTab(
                    assets = DefaultTradingAssets.assets,
                    prices = assetPrices,
                    isLoading = isLoading,
                    onAssetClick = { asset ->
                        selectedAsset = asset
                        showTradeDialog = true
                    }
                )

                1 -> ActivePositionsTab(
                    positions = activePositions,
                    prices = assetPrices,
                    onClose = { viewModel.closePositionEarly(it) }
                )

                2 -> HistoryTab(positions = closedPositions)
            }
        }
    }

    // Trade Dialog
    if (showTradeDialog && selectedAsset != null) {
        TradeDialog(
            asset = selectedAsset!!,
            currentPrice = assetPrices[selectedAsset!!.symbol] ?: 0.0,
            userBalance = user?.totalPoints ?: 0,
            onDismiss = { showTradeDialog = false },
            onConfirm = { type, amount, duration ->
                viewModel.openPosition(selectedAsset!!, type, amount, duration)
                showTradeDialog = false
            }
        )
    }
}

@Composable
fun TradingStatsCard(balance: Int, profitLoss: Int, winRate: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Баланс",
                value = "$balance",
                icon = Icons.Default.AccountBalance,
                color = MaterialTheme.colorScheme.primary
            )

            VerticalDivider(modifier = Modifier.height(50.dp))

            StatItem(
                label = "P/L",
                value = if (profitLoss >= 0) "+$profitLoss" else "$profitLoss",
                icon = if (profitLoss >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                color = if (profitLoss >= 0) QuestCompletedColor else AccentRed
            )

            VerticalDivider(modifier = Modifier.height(50.dp))

            StatItem(
                label = "Win Rate",
                value = "${winRate.toInt()}%",
                icon = Icons.Default.ShowChart,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun MarketTab(
    assets: List<TradingAsset>,
    prices: Map<String, Double>,
    isLoading: Boolean,
    onAssetClick: (TradingAsset) -> Unit
) {
    val categories = AssetCategory.values()
    var selectedCategory by remember { mutableStateOf<AssetCategory?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category filters
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Всі") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) }
                )
            }
        }

        // Assets list
        val filteredAssets = if (selectedCategory == null) assets
        else assets.filter { it.category == selectedCategory }

        if (isLoading && prices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAssets) { asset ->
                    AssetCard(
                        asset = asset,
                        price = prices[asset.symbol],
                        onClick = { onAssetClick(asset) }
                    )
                }
            }
        }
    }
}

@Composable
fun AssetCard(asset: TradingAsset, price: Double?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(asset.icon, style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(asset.name, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            asset.symbol,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Показуємо левередж
                        Surface(
                            color = AccentOrange.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "10x",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (price != null) {
                    Text(
                        "$${String.format("%.2f", price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Можна додати симульований % зміни
                    val mockChange = (Math.random() * 4 - 2) // -2% до +2%
                    Text(
                        "${if (mockChange >= 0) "+" else ""}${String.format("%.2f", mockChange)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mockChange >= 0) QuestCompletedColor else AccentRed
                    )
                } else {
                    // Замість спінера показуємо плейсхолдер
                    Text(
                        "...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        "Завантаження",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ActivePositionsTab(
    positions: List<TradingPosition>,
    prices: Map<String, Double>,
    onClose: (TradingPosition) -> Unit
) {
    if (positions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ShowChart, null, modifier = Modifier.size(80.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Немає активних позицій", color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(positions) { position ->
                ActivePositionCard(position, prices[position.symbol], onClose)
            }
        }
    }
}

@Composable
fun ActivePositionCard(position: TradingPosition, currentPrice: Double?, onClose: (TradingPosition) -> Unit) {
    // Використовуємо currentPrice з параметра, якщо є, інакше з position
    val actualPrice = currentPrice ?: position.currentPrice

    // Розрахунок зміни ціни
    val priceChange = ((actualPrice - position.entryPrice) / position.entryPrice) * 100

    // Враховуємо SHORT
    val effectiveChange = if (position.type == PositionType.SHORT) -priceChange else priceChange

    // 10x левередж - прибуток/збиток в 10 разів більший від відсотку
    val profitLoss = (position.amount * effectiveChange * 10 / 100).toInt()
    val leveragedPercent = effectiveChange * 10

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(position.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (position.type == PositionType.LONG) "📈 LONG" else "📉 SHORT",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (position.type == PositionType.LONG) QuestCompletedColor else AccentRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Показуємо левередж
                        Text(
                            "10x",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (profitLoss >= 0) "+$profitLoss" else "$profitLoss",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (profitLoss >= 0) QuestCompletedColor else AccentRed
                    )
                    // Показуємо левереджований відсоток
                    Text(
                        "${if (leveragedPercent >= 0) "+" else ""}${String.format("%.1f", leveragedPercent)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (leveragedPercent >= 0) QuestCompletedColor else AccentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem("Ставка", "${position.amount}")
                InfoItem("Вхід", "${String.format("%.2f", position.entryPrice)}")
                InfoItem("Зараз", "${String.format("%.2f", actualPrice)}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Показуємо реальну зміну ціни (без леверіджу)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Зміна ціни: ${if (effectiveChange >= 0) "+" else ""}${String.format("%.2f", effectiveChange)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    "× 10 = ${if (leveragedPercent >= 0) "+" else ""}${String.format("%.1f", leveragedPercent)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onClose(position) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text("Закрити позицію")
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HistoryTab(positions: List<TradingPosition>) {
    if (positions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Історія порожня", color = TextSecondary)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(positions) { position ->
                HistoryPositionCard(position)
            }
        }
    }
}

@Composable
fun HistoryPositionCard(position: TradingPosition) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (position.status == PositionStatus.WON)
                QuestCompletedColor.copy(alpha = 0.1f)
            else
                AccentRed.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(position.symbol, fontWeight = FontWeight.Bold)
                Text(
                    if (position.type == PositionType.LONG) "LONG" else "SHORT",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (position.profitLoss >= 0) "+${position.profitLoss}" else "${position.profitLoss}",
                    fontWeight = FontWeight.Bold,
                    color = if (position.profitLoss >= 0) QuestCompletedColor else AccentRed
                )
                Text(
                    when (position.status) {
                        PositionStatus.WON -> "✅ Виграш"
                        PositionStatus.LOST -> "❌ Програш"
                        else -> "⏹️ Закрито"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeDialog(
    asset: TradingAsset,
    currentPrice: Double,
    userBalance: Int,
    onDismiss: () -> Unit,
    onConfirm: (PositionType, Int, TradingDuration) -> Unit
) {
    var selectedType by remember { mutableStateOf(PositionType.LONG) }
    var amount by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(TradingDuration.ONE_HOUR) }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(asset.icon, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(asset.name, fontWeight = FontWeight.Bold)
                            Text(asset.symbol, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Ціна", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "$${String.format("%.2f", currentPrice)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Position Type
                Text("Тип позиції", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == PositionType.LONG,
                        onClick = { selectedType = PositionType.LONG },
                        label = { Text("📈 LONG (Зростання)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QuestCompletedColor.copy(alpha = 0.3f)
                        )
                    )
                    FilterChip(
                        selected = selectedType == PositionType.SHORT,
                        onClick = { selectedType = PositionType.SHORT },
                        label = { Text("📉 SHORT (Падіння)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRed.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                Text("Ставка (ваш баланс: $userBalance)", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            amount = it
                        }
                    },
                    label = { Text("Кількість балів") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null)
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick amount buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50, 100, 250, 500).forEach { quickAmount ->
                        if (quickAmount <= userBalance) {
                            OutlinedButton(
                                onClick = { amount = quickAmount.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(quickAmount.toString(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration
                Text("Термін позиції", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TradingDuration.values()) { duration ->
                        FilterChip(
                            selected = selectedDuration == duration,
                            onClick = { selectedDuration = duration },
                            label = { Text(duration.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "ℹ️ Як це працює?",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (selectedType == PositionType.LONG) {
                                "Ви прогнозуєте, що ціна зросте. Якщо через ${selectedDuration.displayName} ціна буде вищою - ви виграєте!"
                            } else {
                                "Ви прогнозуєте, що ціна впаде. Якщо через ${selectedDuration.displayName} ціна буде нижчою - ви виграєте!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
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
                            val amountInt = amount.toIntOrNull()
                            if (amountInt != null && amountInt > 0 && amountInt <= userBalance) {
                                onConfirm(selectedType, amountInt, selectedDuration)
                            }
                        },
                        enabled = amount.toIntOrNull()?.let { it > 0 && it <= userBalance } == true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == PositionType.LONG)
                                QuestCompletedColor else AccentRed
                        )
                    ) {
                        Icon(
                            if (selectedType == PositionType.LONG)
                                Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Відкрити позицію")
                    }
                }
            }
        }
    }
}