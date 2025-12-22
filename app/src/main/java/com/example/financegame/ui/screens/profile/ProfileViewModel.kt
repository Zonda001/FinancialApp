package com.example.financegame.ui.screens.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.User
import com.example.financegame.data.local.database.entities.ExpenseType
import com.example.financegame.data.repository.UserRepository
import com.example.financegame.data.repository.AchievementRepository
import com.example.financegame.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// 🆕 Data class для копілки
data class PiggyBankGoal(
    val name: String = "",
    val amount: Double = 0.0
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(database.userDao())
    private val achievementRepository = AchievementRepository(database.achievementDao())
    private val expenseRepository = ExpenseRepository(database.expenseDao())

    // SharedPreferences для нагород за стрік
    private val streakRewardPrefs = application.getSharedPreferences("StreakRewards", Context.MODE_PRIVATE)

    // 🆕 SharedPreferences для копілки
    private val piggyBankPrefs = application.getSharedPreferences("PiggyBankPrefs", Context.MODE_PRIVATE)

    val currentUser: StateFlow<User?> = userRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val unlockedAchievementsCount: StateFlow<Int> = achievementRepository.getUnlockedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 🆕 Flow для цілі копілки
    private val _piggyBankGoal = MutableStateFlow(
        PiggyBankGoal(
            name = piggyBankPrefs.getString("goal_name", "") ?: "",
            amount = piggyBankPrefs.getFloat("goal_amount", 0f).toDouble()
        )
    )
    val piggyBankGoal: StateFlow<PiggyBankGoal> = _piggyBankGoal

    // 🆕 Flow для поточного балансу (доходи - витрати)
    val currentBalance: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startOfMonth = calendar.timeInMillis

        expenseRepository.getExpensesByDateRange(1, startOfMonth, System.currentTimeMillis())
            .collect { expenses ->
                val income = expenses.filter { it.type == ExpenseType.INCOME }
                    .sumOf { it.amount }
                val spending = expenses.filter { it.type == ExpenseType.EXPENSE }
                    .sumOf { it.amount }

                emit((income - spending).coerceAtLeast(0.0))
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                userRepository.updateUser(user.copy(name = newName))
            }
        }
    }

    fun updateUserProfile(newName: String, newAvatar: String) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                val oldAvatar = user.avatarUrl

                userRepository.updateUser(
                    user.copy(
                        name = newName,
                        avatarUrl = newAvatar
                    )
                )

                // ✅ Квест: "🌟 Зміни аватар" (тільки якщо аватар змінився)
                if (oldAvatar != newAvatar) {
                    checkAndCompleteQuest("🌟 Зміни аватар")
                }
            }
        }
    }

    fun addExperience(points: Int) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                val oldLevel = user.level
                val newExp = user.experience + points
                val newLevel = calculateLevel(newExp)
                
                // Якщо рівень підвищився - додаємо бонус 100 балів
                val levelUpBonus = if (newLevel > oldLevel) {
                    100 * (newLevel - oldLevel)  // 100 балів за кожен новий рівень
                } else {
                    0
                }
                
                val newTotalPoints = user.totalPoints + points + levelUpBonus

                userRepository.updateUser(
                    user.copy(
                        experience = newExp,
                        level = newLevel,
                        totalPoints = newTotalPoints
                    )
                )
            }
        }
    }

    // ✅ НОВА ФУНКЦІЯ: Отримання нагороди за стрік
    fun claimStreakReward(currentStreak: Int) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                val rewardPoints = 100
                val currentLevel = currentStreak / 5

                // Додаємо бали користувачу
                val newTotalPoints = user.totalPoints + rewardPoints

                userRepository.updateUser(
                    user.copy(
                        totalPoints = newTotalPoints
                    )
                )

                // Зберігаємо рівень отриманої нагороди
                streakRewardPrefs.edit().putInt("last_claimed_level", currentLevel).apply()
            }
        }
    }

    // 🆕 НОВА ФУНКЦІЯ: Встановлення цілі копілки
    fun setPiggyBankGoal(name: String, amount: Double) {
        viewModelScope.launch {
            _piggyBankGoal.value = PiggyBankGoal(name, amount)

            piggyBankPrefs.edit().apply {
                putString("goal_name", name)
                putFloat("goal_amount", amount.toFloat())
                putBoolean("goal_claimed", false)
                apply()
            }
        }
    }

    // 🆕 НОВА ФУНКЦІЯ: Отримання нагороди за копілку
    fun claimPiggyBankReward() {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                val rewardExp = 200

                // Перевіряємо чи не була вже отримана нагорода
                val wasClaimed = piggyBankPrefs.getBoolean("goal_claimed", false)

                if (!wasClaimed) {
                    val newExp = user.experience + rewardExp
                    val newLevel = calculateLevel(newExp)

                    userRepository.updateUser(
                        user.copy(
                            experience = newExp,
                            level = newLevel
                        )
                    )

                    // Позначаємо що нагорода отримана
                    piggyBankPrefs.edit().putBoolean("goal_claimed", true).apply()

                    // Скидаємо ціль після отримання нагороди
                    setPiggyBankGoal("", 0.0)
                }
            }
        }
    }

    private fun calculateLevel(experience: Int): Int {
        return (kotlin.math.sqrt(experience.toDouble() / 100.0)).toInt() + 1
    }

    fun getExperienceForNextLevel(currentExp: Int, currentLevel: Int): Int {
        val nextLevelExp = (currentLevel * currentLevel) * 100
        return (nextLevelExp - currentExp).coerceAtLeast(0)
    }

    // ✅ Функція перевірки та виконання квестів
    private suspend fun checkAndCompleteQuest(questTitle: String) {
        val quests = database.questDao().getActiveQuests().first()
        val quest = quests.find { it.title == questTitle }

        quest?.let {
            if (!it.isCompleted) {
                // Оновлюємо прогрес до 100%
                database.questDao().updateQuestProgress(it.id, 1f)
            }
        }
    }
}