package com.example.financegame.ui.screens.quests

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.data.local.database.entities.Quest
import com.example.financegame.data.local.database.entities.QuestType
import com.example.financegame.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen(
    viewModel: QuestViewModel = viewModel(),
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val activeQuests by viewModel.activeQuests.collectAsState()
    val completedQuests by viewModel.completedQuests.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мої квести", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshQuests() }) {
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Активні (${activeQuests.size})") },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Завершені (${completedQuests.size})") },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (selectedTab) {
                0 -> {
                    if (activeQuests.isEmpty()) {
                        EmptyQuestsPlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeQuests, key = { it.id }) { quest ->
                                QuestCard(
                                    quest = quest,
                                    onComplete = { viewModel.completeQuest(quest) },
                                    onOneClick = { viewModel.completeOneClickQuest(quest.id) },
                                    canCompleteInstantly = viewModel.canCompleteInstantly(quest)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (completedQuests.isEmpty()) {
                        EmptyCompletedQuestsPlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(completedQuests, key = { it.id }) { quest ->
                                CompletedQuestCard(quest = quest)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestCard(
    quest: Quest,
    onComplete: () -> Unit,
    onOneClick: () -> Unit,
    canCompleteInstantly: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (canCompleteInstantly)
                                Icons.Default.TouchApp
                            else
                                getQuestIcon(quest.questType),
                            contentDescription = null,
                            tint = if (canCompleteInstantly) GoldColor else QuestActiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            quest.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        quest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = GoldColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "+${quest.reward}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                canCompleteInstantly && quest.progress < 1f -> {
                    // Квест ще не виконано - показуємо кнопку активації
                    Button(
                        onClick = onOneClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldColor
                        )
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Виконати зараз!", color = TextPrimary)
                    }
                }

                quest.progress >= 1f && !quest.isCompleted -> {
                    // Квест виконано - показуємо кнопку отримання нагороди
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuestCompletedColor
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Отримати нагороду!", color = TextPrimary)
                    }
                }

                else -> {
                    // Звичайні квести з прогресом
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Прогрес",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                "${(quest.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = QuestActiveColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = quest.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (quest.progress >= 1f) QuestCompletedColor else QuestActiveColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Підказка як виконати квест
                        Text(
                            getQuestHint(quest.title),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.8f)
                        )
                    }

                    // КНОПКА "ОТРИМАТИ НАГОРОДУ" - показується тільки коли прогрес 100%
                    if (quest.progress >= 1f && !quest.isCompleted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuestCompletedColor
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отримати нагороду!", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// Функція підказок для квестів
fun getQuestHint(questTitle: String): String {
    return when {
        questTitle.contains("📊") -> "💡 Зайди у вкладку 'Звіти' щоб виконати"
        questTitle.contains("⚙️") && questTitle.contains("тему") -> "💡 Відкрий 'Налаштування' → 'Кольорова тема'"
        questTitle.contains("🏆") -> "💡 Відкрий вкладку 'Досягнення'"
        questTitle.contains("🌟") -> "💡 Зайди в 'Профіль' та натисни кнопку редагування"
        questTitle.contains("🎨") -> "💡 Відкрий 'Налаштування' → 'Режим теми' → 'Темна'"
        questTitle.contains("💰") -> "💡 Відкрий 'Налаштування' → 'Валюта'"
        questTitle.contains("🔔") -> "💡 Відкрий 'Налаштування' → увімкни 'Сповіщення'"
        questTitle.contains("Перший крок") -> "💡 Додай витрату у вкладці 'Витрати'"
        questTitle.contains("Економний тиждень") -> "💡 Витрачай менше 200 грн кожен день протягом 7 днів"
        questTitle.contains("П'ять транзакцій") -> "💡 Додай 5 витрат за сьогодні"
        questTitle.contains("Місяць економії") -> "💡 Витрачай менше 2000 грн протягом місяця"
        else -> ""
    }
}

@Composable
fun CompletedQuestCard(quest: Quest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = QuestCompletedColor.copy(alpha = 0.1f)
        ),
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = QuestCompletedColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Виконано!",
                        style = MaterialTheme.typography.bodySmall,
                        color = QuestCompletedColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    tint = GoldColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "+${quest.reward}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldColor
                )
            }
        }
    }
}

@Composable
fun EmptyQuestsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Немає активних квестів",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary
            )
            Text(
                "Нові квести з'являться незабаром!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyCompletedQuestsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Ще немає завершених квестів",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary
            )
            Text(
                "Виконуйте квести щоб побачити їх тут",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

fun getQuestIcon(questType: QuestType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (questType) {
        QuestType.SAVE_MONEY -> Icons.Default.Savings
        QuestType.NO_SPENDING -> Icons.Default.Block
        QuestType.WEEKLY_GOAL -> Icons.Default.CalendarMonth
        QuestType.DAILY_LIMIT -> Icons.Default.Today
    }
}