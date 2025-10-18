package com.example.financegame.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.data.settings.ThemeMode
import com.example.financegame.data.settings.AppTheme
import com.example.financegame.ui.theme.*
import com.example.financegame.ui.theme.TextPrimary
import com.example.financegame.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onThemeChanged: (ThemeMode) -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = TextLight
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Секція: Зовнішній вигляд
            item {
                SettingsSectionHeader(
                    title = "Зовнішній вигляд",
                    icon = Icons.Default.Palette
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Режим теми",
                        subtitle = when (themeMode) {
                            ThemeMode.LIGHT -> "Світла"
                            ThemeMode.DARK -> "Темна"
                            ThemeMode.SYSTEM -> "Системна"
                        },
                        onClick = { showThemeDialog = true }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Кольорова тема",
                        subtitle = "${appTheme.emoji} ${appTheme.displayName}",
                        onClick = { showColorThemeDialog = true }
                    )
                }
            }

            // Секція: Мова та регіон
            item {
                SettingsSectionHeader(
                    title = "Мова та регіон",
                    icon = Icons.Default.Language
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Translate,
                        title = "Мова",
                        subtitle = when (language) {
                            "uk" -> "Українська"
                            "en" -> "English"
                            else -> language
                        },
                        onClick = { showLanguageDialog = true }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Валюта",
                        subtitle = currency,
                        onClick = { showCurrencyDialog = true }
                    )
                }
            }

            // Секція: Сповіщення
            item {
                SettingsSectionHeader(
                    title = "Сповіщення",
                    icon = Icons.Default.Notifications
                )
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Сповіщення",
                        subtitle = "Отримувати push-сповіщення",
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchItem(
                        icon = Icons.Default.Warning,
                        title = "Попередження про бюджет",
                        subtitle = "Сповіщення при перевищенні ліміту",
                        checked = budgetAlertsEnabled,
                        onCheckedChange = { viewModel.setBudgetAlertsEnabled(it) }
                    )
                }
            }

            // Секція: Безпека
            item {
                SettingsSectionHeader(
                    title = "Безпека",
                    icon = Icons.Default.Security
                )
            }

            item {
                SettingsCard {
                    SettingsSwitchItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Біометрична автентифікація",
                        subtitle = "Використовувати відбиток/Face ID",
                        checked = biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
            }

            // Секція: Про програму
            item {
                SettingsSectionHeader(
                    title = "Про програму",
                    icon = Icons.Default.Info
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.AppShortcut,
                        title = "Версія",
                        subtitle = "1.0.0",
                        onClick = { }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "Ліцензія",
                        subtitle = "MIT License",
                        onClick = { }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "Розробник",
                        subtitle = "Finance Game Team",
                        onClick = { }
                    )
                }
            }
        }
    }

    // Діалог вибору режиму теми
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeMode,
            onThemeSelected = { theme ->
                viewModel.setTheme(theme)
                onThemeChanged(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Діалог вибору кольорової теми
    if (showColorThemeDialog) {
        ColorThemeSelectionDialog(
            currentTheme = appTheme,
            onThemeSelected = { theme ->
                viewModel.setAppTheme(theme)
                showColorThemeDialog = false
            },
            onDismiss = { showColorThemeDialog = false }
        )
    }

    // Діалог вибору мови
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = language,
            onLanguageSelected = { lang ->
                viewModel.setLanguage(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Діалог вибору валюти
    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = currency,
            onCurrencySelected = { curr ->
                viewModel.setCurrency(curr)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Режим теми") },
        text = {
            Column {
                ThemeMode.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (theme) {
                                ThemeMode.LIGHT -> "Світла"
                                ThemeMode.DARK -> "Темна"
                                ThemeMode.SYSTEM -> "Системна"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

@Composable
fun ColorThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Кольорова тема", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(AppTheme.values()) { theme ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentTheme == theme)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (currentTheme == theme) 4.dp else 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                theme.emoji,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    theme.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    when (theme) {
                                        AppTheme.OCEAN -> "Свіжий та спокійний"
                                        AppTheme.SAKURA -> "Ніжний та романтичний"
                                        AppTheme.FOREST -> "Природний та гармонійний"
                                        AppTheme.SUNSET -> "Теплий та затишний"
                                        AppTheme.MIDNIGHT -> "Елегантний та таємничий"
                                        AppTheme.ICE -> "Чистий та освіжаючий"
                                        AppTheme.LAVA -> "Енергійний та яскравий"
                                        AppTheme.MOONLIGHT -> "М'який та спокійний"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (currentTheme == theme) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "uk" to "Українська",
        "en" to "English"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вибрати мову") },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == code,
                            onClick = { onLanguageSelected(code) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

@Composable
fun CurrencySelectionDialog(
    currentCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val currencies = listOf(
        "грн" to "Українська гривня (грн)",
        "$" to "Долар США ($)",
        "€" to "Євро (€)",
        "£" to "Фунт стерлінгів (£)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вибрати валюту") },
        text = {
            Column {
                currencies.forEach { (symbol, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCurrencySelected(symbol) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentCurrency == symbol,
                            onClick = { onCurrencySelected(symbol) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}