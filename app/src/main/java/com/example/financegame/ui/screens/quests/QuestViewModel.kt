package com.example.financegame.ui.screens.quests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.Quest
import com.example.financegame.data.local.database.entities.QuestType
import com.example.financegame.data.repository.QuestRepository
import com.example.financegame.data.repository.UserRepository
import com.example.financegame.data.repository.ExpenseRepository
import com.example.financegame.util.AchievementTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class QuestViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val questRepository = QuestRepository(database.questDao())
    private val userRepository = UserRepository(database.userDao())
    private val expenseRepository = ExpenseRepository(database.expenseDao())

    // 🆕 Система відстеження досягнень
    private val achievementTracker = AchievementTracker(database, viewModelScope)

    val activeQuests: StateFlow<List<Quest>> = questRepository.getActiveQuests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedQuests: StateFlow<List<Quest>> = questRepository.getCompletedQuests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            expenseRepository.getAllExpenses(1).collect {
                checkAndUpdateQuestProgress()
            }
        }
    }

    private suspend fun checkAndUpdateQuestProgress() {
        activeQuests.value.forEach { quest ->
            when (quest.questType) {
                QuestType.SAVE_MONEY -> checkSaveMoneyQuest(quest)
                QuestType.NO_SPENDING -> checkNoSpendingQuest(quest)
                QuestType.WEEKLY_GOAL -> checkWeeklyGoalQuest(quest)
                QuestType.DAILY_LIMIT -> checkDailyLimitQuest(quest)
            }
        }
    }

    private suspend fun checkSaveMoneyQuest(quest: Quest) {
        if (quest.title == "Перший крок") {
            expenseRepository.getAllExpenses(1).first().let { expenses ->
                if (expenses.isNotEmpty() && !quest.isCompleted) {
                    // ✅ Тільки оновлюємо прогрес до 100%, не завершуємо автоматично
                    questRepository.updateQuestProgress(quest.id, 1f)
                }
            }
            return
        }

        val endTime = quest.startDate + (quest.targetDays * 24 * 60 * 60 * 1000L)
        expenseRepository.getTotalExpenses(1, quest.startDate, endTime).firstOrNull()?.let { total ->
            val saved = quest.targetAmount - (total ?: 0.0)
            val progress = (saved / quest.targetAmount).toFloat().coerceIn(0f, 1f)

            // ✅ Тільки оновлюємо прогрес, не завершуємо автоматично
            questRepository.updateQuestProgress(quest.id, progress)
        }
    }

    private suspend fun checkNoSpendingQuest(quest: Quest) {
        val now = System.currentTimeMillis()
        val daysPassed = ((now - quest.startDate) / (24 * 60 * 60 * 1000)).toInt()

        expenseRepository.getExpensesByCategory(1, quest.category).firstOrNull()?.let { expenses ->
            val expensesInPeriod = expenses.filter {
                it.date >= quest.startDate && it.date <= now
            }

            if (expensesInPeriod.isEmpty() && daysPassed >= quest.targetDays) {
                // ✅ Тільки оновлюємо прогрес до 100%
                questRepository.updateQuestProgress(quest.id, 1f)
            } else {
                val progress = (daysPassed.toFloat() / quest.targetDays.toFloat()).coerceIn(0f, 1f)
                questRepository.updateQuestProgress(quest.id, progress)
            }
        }
    }

    private suspend fun checkWeeklyGoalQuest(quest: Quest) {
        val now = System.currentTimeMillis()
        val endTime = quest.startDate + (quest.targetDays * 24 * 60 * 60 * 1000L)

        // ✅ Перевіряємо чи період завершився
        val periodCompleted = now >= endTime

        expenseRepository.getTotalExpenses(1, quest.startDate, endTime).firstOrNull()?.let { total ->
            val actualTotal = total ?: 0.0

            // Розраховуємо прогрес
            if (actualTotal <= quest.targetAmount) {
                // Витрати в межах ліміту
                if (periodCompleted) {
                    // ✅ Період завершився - встановлюємо прогрес 100%, але НЕ завершуємо
                    questRepository.updateQuestProgress(quest.id, 1f)
                } else {
                    // Період ще йде - показуємо прогрес по часу
                    val timeProgress = ((now - quest.startDate).toFloat() / (endTime - quest.startDate).toFloat()).coerceIn(0f, 0.99f)
                    questRepository.updateQuestProgress(quest.id, timeProgress)
                }
            } else {
                // ❌ Витрати перевищили ліміт - квест провалений
                val moneyProgress = (quest.targetAmount / actualTotal).toFloat().coerceIn(0f, 0.99f)
                questRepository.updateQuestProgress(quest.id, moneyProgress)
            }
        }
    }

    private suspend fun checkDailyLimitQuest(quest: Quest) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startOfDay = calendar.timeInMillis

        expenseRepository.getTotalExpenses(1, startOfDay, System.currentTimeMillis()).firstOrNull()?.let { total ->
            val progress = if (total != null && total <= quest.targetAmount) {
                1f
            } else {
                ((quest.targetAmount / (total ?: quest.targetAmount)).toFloat()).coerceIn(0f, 1f)
            }

            // ✅ Тільки оновлюємо прогрес
            questRepository.updateQuestProgress(quest.id, progress)
        }
    }

    // ✅ Ця функція викликається тільки коли користувач натискає "Отримати нагороду"
    fun completeQuest(quest: Quest) {
        viewModelScope.launch {
            questRepository.completeQuest(quest.id)

            userRepository.getCurrentUser().first()?.let { user ->
                val newExp = user.experience + quest.reward
                val newLevel = calculateLevel(newExp)
                val newTotalPoints = user.totalPoints + quest.reward

                userRepository.updateUser(
                    user.copy(
                        experience = newExp,
                        level = newLevel,
                        totalPoints = newTotalPoints
                    )
                )

                // 🆕 Відстежуємо досягнення
                achievementTracker.onQuestCompleted()
                achievementTracker.onLevelUp(newLevel)
            }

            checkAndUpdateQuestProgress()
        }
    }

    fun refreshQuests() {
        viewModelScope.launch {
            checkAndUpdateQuestProgress()
        }
    }

    // 🆕 НОВА ЛОГІКА: Квести з навігацією замість миттєвого виконання
    fun getQuestNavigationTarget(quest: Quest): QuestNavigationTarget? {
        return when {
            quest.title.contains("📊 Переглянь статистику") -> QuestNavigationTarget.REPORTS
            quest.title.contains("⚙️ Налаштуй тему") -> QuestNavigationTarget.SETTINGS
            quest.title.contains("🏆 Переглянь досягнення") -> QuestNavigationTarget.ACHIEVEMENTS
            quest.title.contains("🌟 Зміни аватар") -> QuestNavigationTarget.PROFILE
            quest.title.contains("🎨 Спробуй темну тему") -> QuestNavigationTarget.SETTINGS
            quest.title.contains("💰 Вибери валюту") -> QuestNavigationTarget.SETTINGS
            quest.title.contains("🔔 Увімкни сповіщення") -> QuestNavigationTarget.SETTINGS
            else -> null
        }
    }

    // Для квестів які можна виконати одразу
    fun canCompleteInstantly(quest: Quest): Boolean {
        return when {
            quest.title.contains("🎯 Швидкий старт") -> true
            quest.title.contains("💪 Щоденна мотивація") -> true
            quest.title.contains("🎁 Бонус новачка") -> true
            else -> false
        }
    }

    fun completeOneClickQuest(questId: Int) {
        viewModelScope.launch {
            activeQuests.value.find { it.id == questId }?.let { quest ->
                if (canCompleteInstantly(quest)) {
                    questRepository.updateQuestProgress(quest.id, 1f)
                    completeQuest(quest)
                }
            }
        }
    }

    private fun calculateLevel(experience: Int): Int {
        return (kotlin.math.sqrt(experience.toDouble() / 100.0)).toInt() + 1
    }
}

// 🆕 Enum для навігації квестів
enum class QuestNavigationTarget {
    REPORTS,
    SETTINGS,
    ACHIEVEMENTS,
    PROFILE
}