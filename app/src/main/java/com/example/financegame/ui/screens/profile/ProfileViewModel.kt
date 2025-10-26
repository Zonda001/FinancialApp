package com.example.financegame.ui.screens.profile

import android.app.Application
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
                // Виконуємо квест
                database.questDao().completeQuest(it.id, System.currentTimeMillis())

                // Даємо досвід
                addExperience(it.reward)
            }
        }
    }
}