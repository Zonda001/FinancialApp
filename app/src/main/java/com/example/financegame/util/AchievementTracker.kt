package com.example.financegame.util

import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.AchievementCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Система автоматичного відстеження досягнень
 */
class AchievementTracker(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {

    // ======================== ЗАГАЛЬНІ ДОСЯГНЕННЯ ========================

    suspend fun checkExpenseAdded() {
        val expenseCount = database.expenseDao().getAllExpenses(1).first().size
        updateAchievementProgress("Новачок", expenseCount, AchievementCategory.GENERAL)
        updateAchievementProgress("Активний користувач", expenseCount, AchievementCategory.GENERAL)
        updateAchievementProgress("Фінансовий гуру", expenseCount, AchievementCategory.GENERAL)
    }

    suspend fun checkLevelAchievement(level: Int) {
        updateAchievementProgress("Майстер фінансів", level, AchievementCategory.GENERAL)
        updateAchievementProgress("Легенда", level, AchievementCategory.GENERAL)
    }

    suspend fun checkExpenseWithDescription() {
        val expenses = database.expenseDao().getAllExpenses(1).first()
        val withDescription = expenses.count { it.description.isNotEmpty() }
        updateAchievementProgress("Перфекціоніст", withDescription, AchievementCategory.GENERAL)
    }

    suspend fun checkCategoryDiversity() {
        val expenses = database.expenseDao().getAllExpenses(1).first()
        val uniqueCategories = expenses.map { it.category }.distinct().size
        updateAchievementProgress("Різноманітність", uniqueCategories, AchievementCategory.GENERAL)
    }

    suspend fun checkTimeBasedAchievements(hour: Int) {
        if (hour < 9) {
            unlockAchievement("Ранкова пташка")
        }
        if (hour >= 23) {
            unlockAchievement("Нічний дозор")
        }
    }

    // ======================== ЗАОЩАДЖЕННЯ ========================

    suspend fun checkSavingsAchievements(savedAmount: Double) {
        updateAchievementProgress("Економний", savedAmount.toInt(), AchievementCategory.SAVINGS)
        updateAchievementProgress("Скарбничка", savedAmount.toInt(), AchievementCategory.SAVINGS)
        updateAchievementProgress("Фінансова свобода", savedAmount.toInt(), AchievementCategory.SAVINGS)
    }

    suspend fun checkDailyLimitAchievement(dailyExpense: Double) {
        if (dailyExpense < 100.0) {
            // Перевіряємо чи це вже 7 днів поспіль
            val achievement = database.achievementDao()
                .getAllAchievements()
                .first()
                .find { it.title == "Мінімаліст" }

            achievement?.let {
                val newProgress = it.currentProgress + 1
                updateAchievementProgress("Мінімаліст", newProgress, AchievementCategory.SAVINGS)
            }
        }
    }

    // ======================== КВЕСТИ ========================

    suspend fun checkQuestCompletion() {
        val completedQuests = database.questDao().getCompletedQuests().first().size
        updateAchievementProgress("Герой квестів", completedQuests, AchievementCategory.QUESTS)
        updateAchievementProgress("Шукач пригод", completedQuests, AchievementCategory.QUESTS)
        updateAchievementProgress("Легендарний герой", completedQuests, AchievementCategory.QUESTS)
    }

    // ======================== СЕРІЇ ========================

    suspend fun checkStreakAchievement(streakDays: Int) {
        updateAchievementProgress("Тижнева серія", streakDays, AchievementCategory.STREAK)
        updateAchievementProgress("Місячна відданість", streakDays, AchievementCategory.STREAK)
        updateAchievementProgress("Незламний", streakDays, AchievementCategory.STREAK)
    }

    // ======================== ДОПОМІЖНІ ФУНКЦІЇ ========================

    private suspend fun updateAchievementProgress(title: String, progress: Int, category: AchievementCategory) {
        val achievements = database.achievementDao().getAllAchievements().first()
        val achievement = achievements.find { it.title == title && it.category == category }

        achievement?.let {
            if (!it.isUnlocked) {
                database.achievementDao().updateAchievementProgress(it.id, progress)

                // Якщо досягнення виконано - розблоковуємо
                if (progress >= it.requirement) {
                    unlockAchievement(title)
                }
            }
        }
    }

    private suspend fun unlockAchievement(title: String) {
        val achievements = database.achievementDao().getAllAchievements().first()
        val achievement = achievements.find { it.title == title }

        achievement?.let {
            if (!it.isUnlocked) {
                database.achievementDao().unlockAchievement(it.id, System.currentTimeMillis())
            }
        }
    }

    // ======================== ПУБЛІЧНІ МЕТОДИ ДЛЯ ВИКЛИКІВ ========================

    fun onExpenseAdded() {
        scope.launch {
            checkExpenseAdded()
            checkExpenseWithDescription()
            checkCategoryDiversity()
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            checkTimeBasedAchievements(hour)
        }
    }

    fun onQuestCompleted() {
        scope.launch {
            checkQuestCompletion()
        }
    }

    fun onLevelUp(level: Int) {
        scope.launch {
            checkLevelAchievement(level)
        }
    }

    fun onDailyExpenseCheck(dailyExpense: Double) {
        scope.launch {
            checkDailyLimitAchievement(dailyExpense)
        }
    }

    fun onSavingsUpdate(savedAmount: Double) {
        scope.launch {
            checkSavingsAchievements(savedAmount)
        }
    }

    fun onStreakUpdate(days: Int) {
        scope.launch {
            checkStreakAchievement(days)
        }
    }
}