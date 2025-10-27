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

    // SharedPreferences для відстеження прогресу квестів
    private val questProgressPrefs = application.getSharedPreferences("QuestProgressPrefs", Context.MODE_PRIVATE)

    // 🆕 Система відстеження досягнень
    private val achievementTracker = AchievementTracker(database, viewModelScope, getApplication())

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
                QuestType.NO_SPENDING -> {} // Видалено
                QuestType.WEEKLY_GOAL -> checkWeeklyGoalQuest(quest)
                QuestType.DAILY_LIMIT -> checkDailyLimitQuest(quest)
            }
        }
    }

    private suspend fun checkSaveMoneyQuest(quest: Quest) {
        if (quest.title == "Перший крок") {
            expenseRepository.getAllExpenses(1).first().let { expenses ->
                if (expenses.isNotEmpty() && quest.progress < 1f) {
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

    private suspend fun checkWeeklyGoalQuest(quest: Quest) {
        val questKey = "quest_${quest.id}_start_date"
        val streakKey = "quest_${quest.id}_streak_days"
        val lastCheckKey = "quest_${quest.id}_last_check"

        val today = getTodayDateString()
        val lastCheck = questProgressPrefs.getString(lastCheckKey, "") ?: ""

        // Перевіряємо чи вже перевіряли сьогодні
        if (lastCheck == today) {
            return
        }

        // Отримуємо поточну серію
        var streakDays = questProgressPrefs.getInt(streakKey, 0)
        val startDate = questProgressPrefs.getString(questKey, "") ?: ""

        // Якщо перший запуск квесту - встановлюємо дату старту
        if (startDate.isEmpty()) {
            questProgressPrefs.edit().apply {
                putString(questKey, today)
                putInt(streakKey, 0)
                apply()
            }
            return
        }

        // Перевіряємо витрати за вчора
        val yesterday = getYesterdayDateString()
        val yesterdayExpenses = getExpensesForDate(yesterday)

        if (yesterdayExpenses <= quest.targetAmount) {
            // Витрати в межах ліміту - збільшуємо серію
            streakDays++

            questProgressPrefs.edit().apply {
                putInt(streakKey, streakDays)
                putString(lastCheckKey, today)
                apply()
            }

            // Оновлюємо прогрес
            val progress = (streakDays.toFloat() / quest.targetDays.toFloat()).coerceIn(0f, 1f)
            questRepository.updateQuestProgress(quest.id, progress)

            // Якщо досягли цілі - квест готовий до завершення
            if (streakDays >= quest.targetDays && quest.progress < 1f) {
                questRepository.updateQuestProgress(quest.id, 1f)
            }
        } else {
            // Витрати перевищили ліміт - скидаємо серію
            questProgressPrefs.edit().apply {
                putString(questKey, today)
                putInt(streakKey, 0)
                putString(lastCheckKey, today)
                apply()
            }
            questRepository.updateQuestProgress(quest.id, 0f)
        }
    }

    private suspend fun checkDailyLimitQuest(quest: Quest) {
        if (quest.title == "П'ять транзакцій") {
            val today = getTodayDateString()
            val todayExpenses = getExpensesCountForDate(today)

            val progress = (todayExpenses.toFloat() / quest.targetAmount.toFloat()).coerceIn(0f, 1f)
            questRepository.updateQuestProgress(quest.id, progress)

            // Якщо досягли 5 транзакцій - готово до завершення
            if (todayExpenses >= quest.targetAmount.toInt()) {
                questRepository.updateQuestProgress(quest.id, 1f)
            }
        }
    }
    // Допоміжні функції для роботи з датами та витратами
    private fun getYesterdayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }
    private suspend fun getExpensesForDate(dateString: String): Double {
        val calendar = parseDateString(dateString)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        return expenseRepository.getTotalExpenses(1, startOfDay, endOfDay).first() ?: 0.0
    }

    private suspend fun getExpensesCountForDate(dateString: String): Int {
        val calendar = parseDateString(dateString)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        return expenseRepository.getExpensesByDateRange(1, startOfDay, endOfDay)
            .first()
            .filter { it.type == com.example.financegame.data.local.database.entities.ExpenseType.EXPENSE }
            .size
    }

    private fun parseDateString(dateString: String): Calendar {
        val parts = dateString.split("-")
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, parts[0].toInt())
        calendar.set(Calendar.MONTH, parts[1].toInt())
        calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
        return calendar
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