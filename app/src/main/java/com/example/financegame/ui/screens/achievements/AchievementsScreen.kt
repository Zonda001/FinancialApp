package com.example.financegame.ui.screens.achievements

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financegame.data.local.database.entities.Achievement
import com.example.financegame.data.local.database.entities.AchievementCategory
import com.example.financegame.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementViewModel = viewModel()
) {
    val allAchievements by viewModel.allAchievements.collectAsState()
    val unlockedCount by viewModel.unlockedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    val filteredAchievements = remember(allAchievements, selectedCategory) {
        if (selectedCategory == null) {
            allAchievements
        } else {
            allAchievements.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Досягнення", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AchievementProgressCard(
                unlocked = unlockedCount,
                total = totalCount
            )

            Spacer(modifier = Modifier.height(8.dp))

            CategoryFilters(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredAchievements.isEmpty()) {
                EmptyAchievementsPlaceholder()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAchievements, key = { it.id }) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            onClick = { selectedAchievement = achievement }
                        )
                    }
                }
            }
        }
    }

    // Діалог деталей досягнення
    selectedAchievement?.let { achievement ->
        AchievementDetailDialog(
            achievement = achievement,
            onDismiss = { selectedAchievement = null }
        )
    }
}

@Composable
fun AchievementProgressCard(unlocked: Int, total: Int) {
    val progress = if (total > 0) unlocked.toFloat() / total.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(GoldColor, AccentOrange)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Твій прогрес",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$unlocked / $total",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    }

                    Icon(
                        Icons.Default.MilitaryTech,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = TextLight.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = TextLight,
                    trackColor = TextLight.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "${(progress * 100).toInt()}% виконано",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLight.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun CategoryFilters(
    selectedCategory: AchievementCategory?,
    onCategorySelected: (AchievementCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Всі") }
        )
        FilterChip(
            selected = selectedCategory == AchievementCategory.GENERAL,
            onClick = { onCategorySelected(AchievementCategory.GENERAL) },
            label = { Text("Загальні") },
            leadingIcon = {
                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        FilterChip(
            selected = selectedCategory == AchievementCategory.SAVINGS,
            onClick = { onCategorySelected(AchievementCategory.SAVINGS) },
            label = { Text("Економія") },
            leadingIcon = {
                Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        FilterChip(
            selected = selectedCategory == AchievementCategory.QUESTS,
            onClick = { onCategorySelected(AchievementCategory.QUESTS) },
            label = { Text("Квести") },
            leadingIcon = {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
}

@Composable
fun AchievementCard(achievement: Achievement, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.isUnlocked) 4.dp else 1.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            if (achievement.isUnlocked) {
                                Brush.linearGradient(
                                    colors = listOf(GoldColor, AccentOrange)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (achievement.isUnlocked) {
                        Text(
                            achievement.icon,
                            style = MaterialTheme.typography.displayMedium
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (achievement.isUnlocked) TextPrimary else TextSecondary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (!achievement.isUnlocked) {
                    LinearProgressIndicator(
                        progress = achievement.currentProgress.toFloat() / achievement.requirement.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PrimaryBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        "${achievement.currentProgress}/${achievement.requirement}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = QuestCompletedColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Відкрито!",
                            style = MaterialTheme.typography.bodySmall,
                            color = QuestCompletedColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementDetailDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Іконка
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (achievement.isUnlocked) {
                                Brush.linearGradient(
                                    colors = listOf(GoldColor, AccentOrange)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (achievement.isUnlocked) {
                        Text(
                            achievement.icon,
                            style = MaterialTheme.typography.displayLarge
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Назва
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (achievement.isUnlocked) TextPrimary else TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Опис
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Прогрес
                if (!achievement.isUnlocked) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Прогрес",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${achievement.currentProgress} / ${achievement.requirement}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = achievement.currentProgress.toFloat() / achievement.requirement.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PrimaryBlue,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Залишилось: ${achievement.requirement - achievement.currentProgress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    // Статус відкрито
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = QuestCompletedColor.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = QuestCompletedColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Досягнення відкрито!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = QuestCompletedColor
                            )
                        }
                    }

                    achievement.unlockedAt?.let { timestamp ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Відкрито: ${formatDate(timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Категорія
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (achievement.category) {
                            AchievementCategory.GENERAL -> Icons.Default.Dashboard
                            AchievementCategory.SAVINGS -> Icons.Default.Savings
                            AchievementCategory.QUESTS -> Icons.Default.EmojiEvents
                            AchievementCategory.STREAK -> Icons.Default.LocalFireDepartment
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (achievement.category) {
                            AchievementCategory.GENERAL -> "Загальне"
                            AchievementCategory.SAVINGS -> "Заощадження"
                            AchievementCategory.QUESTS -> "Квести"
                            AchievementCategory.STREAK -> "Серії"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка закрити
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрити")
                }
            }
        }
    }
}

@Composable
fun EmptyAchievementsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Stars,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Немає досягнень",
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}