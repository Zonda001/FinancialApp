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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class QuestViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val questRepository = QuestRepository(database.questDao())
    private val userRepository = UserRepository(database.userDao())
    private val expenseRepository = ExpenseRepository(database.expenseDao())

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
        // Автоматична перевірка квестів при завантаженні
        viewModelScope.launch {
            checkAndUpdateQuestProgress()
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
        val endTime = quest.startDate + (quest.targetDays * 24 * 60 * 60 * 1000L)

        expenseRepository.getTotalExpenses(1, quest.startDate, endTime).firstOrNull()?.let { total ->
            val saved = quest.targetAmount - (total ?: 0.0)
            val progress = (saved / quest.targetAmount).toFloat().coerceIn(0f, 1f)

            questRepository.updateQuestProgress(quest.id, progress)

            if (progress >= 1f && !quest.isCompleted) {
                completeQuest(quest)
            }
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
                if (!quest.isCompleted) {
                    completeQuest(quest)
                }
            } else {
                val progress = (daysPassed.toFloat() / quest.targetDays.toFloat()).coerceIn(0f, 1f)
                questRepository.updateQuestProgress(quest.id, progress)
            }
        }
    }

    private suspend fun checkWeeklyGoalQuest(quest: Quest) {
        val endTime = quest.startDate + (quest.targetDays * 24 * 60 * 60 * 1000L)

        expenseRepository.getTotalExpenses(1, quest.startDate, endTime).firstOrNull()?.let { total ->
            val progress = if (total != null && total <= quest.targetAmount) {
                1f
            } else {
                ((quest.targetAmount / (total ?: quest.targetAmount)).toFloat()).coerceIn(0f, 1f)
            }

            questRepository.updateQuestProgress(quest.id, progress)

            if (progress >= 1f && !quest.isCompleted && System.currentTimeMillis() >= endTime) {
                completeQuest(quest)
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
            // Додаємо досвід і бали за виконання квесту
            userRepository.addExperience(1, quest.reward)

            // Перевіряємо квести знову після виконання
            checkAndUpdateQuestProgress()
        }
    }

    fun refreshQuests() {
        viewModelScope.launch {
            checkAndUpdateQuestProgress()
        }
    }
}