package com.example.financegame.ui.screens.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.User
import com.example.financegame.data.repository.UserRepository
import com.example.financegame.data.repository.AchievementRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(database.userDao())
    private val achievementRepository = AchievementRepository(database.achievementDao())

    // SharedPreferences для нагород за стрік
    private val streakRewardPrefs = application.getSharedPreferences("StreakRewards", Context.MODE_PRIVATE)

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
                val newExp = user.experience + points
                val newLevel = calculateLevel(newExp)
                val newTotalPoints = user.totalPoints + points

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