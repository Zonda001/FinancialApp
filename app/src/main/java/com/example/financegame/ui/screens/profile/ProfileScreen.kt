package com.example.financegame.ui.screens.profile

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.ui.screens.auth.avatarsList
import com.example.financegame.ui.theme.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val achievementsCount by viewModel.unlockedAchievementsCount.collectAsState()
    val piggyBankGoal by viewModel.piggyBankGoal.collectAsState()
    val currentBalance by viewModel.currentBalance.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showPiggyBankDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val streakPrefs = remember { context.getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE) }
    val currentStreak = remember {
        val lastStreakDate = streakPrefs.getString("last_streak_date", "") ?: ""
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        if (lastStreakDate != today && lastStreakDate != yesterday && lastStreakDate.isNotEmpty()) {
            streakPrefs.edit().apply {
                putInt("current_streak", 0)
                putString("last_streak_date", "")
                apply()
            }
            0
        } else {
            streakPrefs.getInt("current_streak", 0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мій профіль", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редагувати",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                user?.let { currentUser ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Аватар
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { showEditDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentUser.avatarUrl.isNotEmpty())
                                    currentUser.avatarUrl
                                else
                                    currentUser.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Рівень",
                                tint = GoldColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Рівень ${currentUser.level}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldColor
                            )
                        }
                    }
                }
            }

            item {
                user?.let { currentUser ->
                    ExperienceProgressCard(
                        currentExp = currentUser.experience,
                        level = currentUser.level,
                        onExpNeeded = viewModel::getExperienceForNextLevel
                    )
                }
            }

            item {
                user?.let { currentUser ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Досвід",
                                value = currentUser.experience.toString(),
                                icon = Icons.Default.EmojiEvents,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Бали",
                                value = currentUser.totalPoints.toString(),
                                icon = Icons.Default.Stars,
                                color = GoldColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Досягнення",
                                value = achievementsCount.toString(),
                                icon = Icons.Default.MilitaryTech,
                                color = AccentOrange,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Серія днів",
                                value = currentStreak.toString(),
                                icon = Icons.Default.LocalFireDepartment,
                                color = AccentOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                StreakRewardCard(
                    currentStreak = currentStreak,
                    onClaimReward = {
                        viewModel.claimStreakReward(currentStreak)
                    }
                )
            }

            // 🆕 КОПІЛКА - НОВА СЕКЦІЯ
            item {
                PiggyBankCard(
                    currentBalance = currentBalance,
                    goalAmount = piggyBankGoal.amount,
                    goalName = piggyBankGoal.name,
                    onSetGoal = { showPiggyBankDialog = true },
                    onClaimReward = {
                        viewModel.claimPiggyBankReward()
                    }
                )
            }
        }
    }

    if (showEditDialog && user != null) {
        EditProfileDialog(
            currentName = user!!.name,
            currentAvatar = user!!.avatarUrl,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newAvatar ->
                viewModel.updateUserProfile(newName, newAvatar)
                showEditDialog = false
            }
        )
    }

    if (showPiggyBankDialog) {
        SetPiggyBankGoalDialog(
            currentGoal = piggyBankGoal,
            onDismiss = { showPiggyBankDialog = false },
            onSave = { name, amount ->
                viewModel.setPiggyBankGoal(name, amount)
                showPiggyBankDialog = false
            }
        )
    }
}

// 🆕 КАРТКА КОПІЛКИ
@Composable
fun PiggyBankCard(
    currentBalance: Double,
    goalAmount: Double,
    goalName: String,
    onSetGoal: () -> Unit,
    onClaimReward: () -> Unit
) {
    val progress = if (goalAmount > 0) {
        (currentBalance / goalAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val goalReached = currentBalance >= goalAmount && goalAmount > 0

    // Анімація для копілки
    val infiniteTransition = rememberInfiniteTransition(label = "piggy")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (goalReached) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (goalReached)
                GoldColor.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (goalReached) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🐷",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = if (goalReached) {
                            Modifier.graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                        } else {
                            Modifier
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Віртуальна копілка",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (goalAmount > 0) {
                            Text(
                                goalName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                "Натисни щоб встановити ціль",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                IconButton(onClick = onSetGoal) {
                    Icon(
                        if (goalAmount > 0) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (goalReached) GoldColor else TextSecondary
                    )
                }
            }

            if (goalAmount > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                // Прогрес бар
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (goalReached) {
                                        listOf(GoldColor, GoldColor.copy(alpha = 0.7f))
                                    } else {
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Зібрано",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            String.format("%.2f грн", currentBalance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (goalReached) GoldColor else MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Ціль",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            String.format("%.2f грн", goalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (goalReached) {
                    Button(
                        onClick = onClaimReward,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldColor
                        )
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "🎉 Отримати нагороду +200 XP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${(progress * 100).toInt()}% виконано",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (currentBalance < goalAmount) {
                            Text(
                                "Залишилось: ${String.format("%.2f грн", goalAmount - currentBalance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "💡 Підказка: Заробляй більше доходів ніж витрат, щоб швидше досягти мети!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// 🆕 ДІАЛОГ ВСТАНОВЛЕННЯ ЦІЛІ КОПІЛКИ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPiggyBankGoalDialog(
    currentGoal: PiggyBankGoal,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var goalName by remember { mutableStateOf(currentGoal.name) }
    var goalAmount by remember {
        mutableStateOf(if (currentGoal.amount > 0) currentGoal.amount.toInt().toString() else "")
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🐷", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Встановити ціль копілки",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Встанови фінансову ціль та заощаджуй гроші. При досягненні мети отримаєш +200 XP!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = goalName,
                    onValueChange = { if (it.length <= 30) goalName = it },
                    label = { Text("Назва цілі") },
                    placeholder = { Text("Наприклад: Новий телефон") },
                    leadingIcon = {
                        Icon(Icons.Default.FlagCircle, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "${goalName.length}/30 символів",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = goalAmount,
                    onValueChange = { goalAmount = it },
                    label = { Text("Сума (грн)") },
                    leadingIcon = {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "💡 Копілка автоматично наповнюється різницею між твоїми доходами та витратами",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))

// Кнопки в колонці для кращого відображення
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Основні кнопки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Скасувати")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = goalAmount.toDoubleOrNull()
                                if (goalName.isNotBlank() && amount != null && amount > 0) {
                                    onSave(goalName, amount)
                                }
                            },
                            enabled = goalName.isNotBlank() &&
                                    goalAmount.toDoubleOrNull() != null &&
                                    goalAmount.toDoubleOrNull()!! > 0
                        ) {
                            Text("Зберегти")
                        }
                    }

                    // Кнопка "Скинути" окремо знизу
                    if (currentGoal.amount > 0) {
                        OutlinedButton(
                            onClick = {
                                onSave("", 0.0)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Скинути ціль")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreakRewardCard(
    currentStreak: Int,
    onClaimReward: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("StreakRewards", Context.MODE_PRIVATE) }

    var lastClaimedLevel by remember { mutableStateOf(prefs.getInt("last_claimed_level", 0)) }
    val currentLevel = currentStreak / 5
    val progress = (currentStreak % 5) / 5f
    val canClaimReward = currentLevel > lastClaimedLevel && currentStreak >= 5

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canClaimReward)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (canClaimReward) 6.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Нагорода за стрік",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (canClaimReward)
                                "Натисни щоб отримати!"
                            else
                                "${currentStreak % 5}/5 днів до нагороди",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canClaimReward)
                                MaterialTheme.colorScheme.primary
                            else
                                TextSecondary
                        )
                    }
                }

                if (!canClaimReward) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = GoldColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "+100",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (canClaimReward) {
                Button(
                    onClick = {
                        onClaimReward()
                        prefs.edit().putInt("last_claimed_level", currentLevel).apply()
                        lastClaimedLevel = currentLevel
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Отримати +100 балів",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
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
                                        AccentOrange,
                                        AccentOrange.copy(alpha = 0.7f)
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
                        "Залишилось ${5 - (currentStreak % 5)} днів",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (lastClaimedLevel > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Отримано нагород: $lastClaimedLevel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "Всього балів: ${lastClaimedLevel * 100}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ExperienceProgressCard(
    currentExp: Int,
    level: Int,
    onExpNeeded: (Int, Int) -> Int
) {
    val expForNextLevel = onExpNeeded(currentExp, level)
    val totalExpNeeded = (level * level) * 100
    val progress = (currentExp.toFloat() / totalExpNeeded.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Досвід до наступного рівня",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$expForNextLevel XP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

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
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
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
                    "$currentExp XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    "$totalExpNeeded XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "${(progress * 100).toInt()}% до рівня ${level + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var newAvatar by remember { mutableStateOf(currentAvatar.ifEmpty { "👨" }) }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Редагувати профіль",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        newAvatar,
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = MaterialTheme.typography.displayMedium.fontSize * 1.2f
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(avatarsList) { avatar ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (avatar == newAvatar)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (avatar == newAvatar) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { newAvatar = avatar },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    avatar,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 20) newName = it },
                    label = { Text("Нікнейм") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${newName.length}/20 символів",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            if (newName.length >= 3) {
                                onSave(newName, newAvatar)
                            }
                        },
                        enabled = newName.length >= 3
                    ) {
                        Text("Зберегти")
                    }
                }
            }
        }
    }
}

private fun getTodayDateString(): String {
    val calendar = java.util.Calendar.getInstance()
    return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
}

private fun getYesterdayDateString(): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
    return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
}