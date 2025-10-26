package com.example.financegame.ui.screens.quests

import android.app.Application
import android.content.Context
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

    // SharedPreferences для відстеження щоденних квестів
    private val prefs = application.getSharedPreferences("QuestPrefs", Context.MODE_PRIVATE)
    private val DAILY_QUEST_PREFIX = "daily_quest_"

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
            // Перевіряємо чи потрібно скинути щоденні квести
            resetDailyQuestsIfNeeded()

            expenseRepository.getAllExpenses(1).collect {
                checkAndUpdateQuestProgress()
            }
        }
    }

    // Функція для скидання щоденних квестів
    private suspend fun resetDailyQuestsIfNeeded() {
        val today = getTodayDateString()

        // Отримуємо ВСІ квести (включно з завершеними)
        val allQuests = database.questDao().getAllQuests().first()

        allQuests.forEach { quest ->
            if (isDailyQuest(quest)) {
                val lastCompletedDate = prefs.getString("${DAILY_QUEST_PREFIX}${quest.id}", "")

                // Якщо квест виконано не сьогодні - скидаємо його
                if (lastCompletedDate != today && quest.isCompleted) {
                    questRepository.resetQuest(quest.id)
                }
            }
        }
    }

    // Перевірка чи квест щоденний
    private fun isDailyQuest(quest: Quest): Boolean {
        return quest.title.contains("💪 Щоденна мотивація")
    }

    // Отримання сьогоднішньої дати у форматі рядка
    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
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
                    questRepository.updateQuestProgress(quest.id, 1f)
                }
            }
            return
        }

        val endTime = quest.startDate + (quest.targetDays * 24 * 60 * 60 * 1000L)
        expenseRepository.getTotalExpenses(1, quest.startDate, endTime).firstOrNull()?.let { total ->
            val saved = quest.targetAmount - (total ?: 0.0)
            val progress = (saved / quest.targetAmount).toFloat().coerceIn(0f, 1f)
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
        val periodCompleted = now >= endTime

        expenseRepository.getTotalExpenses(1, quest.startDate, endTime).firstOrNull()?.let { total ->
            val actualTotal = total ?: 0.0

            if (actualTotal <= quest.targetAmount) {
                if (periodCompleted) {
                    questRepository.updateQuestProgress(quest.id, 1f)
                } else {
                    val timeProgress = ((now - quest.startDate).toFloat() / (endTime - quest.startDate).toFloat()).coerceIn(0f, 0.99f)
                    questRepository.updateQuestProgress(quest.id, timeProgress)
                }
            } else {
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
            questRepository.updateQuestProgress(quest.id, progress)
        }
    }

    fun completeQuest(quest: Quest) {
        viewModelScope.launch {
            questRepository.completeQuest(quest.id)

            // Якщо це щоденний квест - зберігаємо дату виконання
            if (isDailyQuest(quest)) {
                val today = getTodayDateString()
                prefs.edit().putString("${DAILY_QUEST_PREFIX}${quest.id}", today).apply()
            }

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

                achievementTracker.onQuestCompleted()
                achievementTracker.onLevelUp(newLevel)
            }

            checkAndUpdateQuestProgress()
        }
    }

    fun refreshQuests() {
        viewModelScope.launch {
            resetDailyQuestsIfNeeded()
            checkAndUpdateQuestProgress()
        }
    }

    // 🧪 ТЕСТОВА ФУНКЦІЯ - для перевірки скидання щоденних квестів
    fun testResetDailyQuests() {
        viewModelScope.launch {
            // Очищаємо всі дати виконання щоденних квестів
            val allQuests = database.questDao().getAllQuests().first()
            allQuests.forEach { quest ->
                if (isDailyQuest(quest)) {
                    prefs.edit().remove("${DAILY_QUEST_PREFIX}${quest.id}").apply()
                    if (quest.isCompleted) {
                        questRepository.resetQuest(quest.id)
                    }
                }
            }
        }
    }

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
        // Перевіряємо чи це щоденний квест і чи не виконувався він сьогодні
        if (isDailyQuest(quest)) {
            val today = getTodayDateString()
            val lastCompleted = prefs.getString("${DAILY_QUEST_PREFIX}${quest.id}", "")

            // Якщо виконувався сьогодні - не показуємо як миттєвий
            if (lastCompleted == today) {
                return false
            }

            return when {
                quest.title.contains("🎯 Швидкий старт") -> true
                quest.title.contains("💪 Щоденна мотивація") -> true
                quest.title.contains("🎁 Бонус новачка") -> true
                else -> false
            }
        }

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

enum class QuestNavigationTarget {
    REPORTS,
    SETTINGS,
    ACHIEVEMENTS,
    PROFILE
}