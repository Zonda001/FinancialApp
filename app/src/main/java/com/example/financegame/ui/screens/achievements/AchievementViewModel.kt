package com.example.financegame.ui.screens.achievements

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.Achievement
import com.example.financegame.data.local.database.entities.AchievementCategory
import com.example.financegame.data.repository.AchievementRepository
import com.example.financegame.util.AchievementTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AchievementViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val achievementRepository = AchievementRepository(database.achievementDao())
    private val achievementTracker = AchievementTracker(database, viewModelScope)

    val allAchievements: StateFlow<List<Achievement>> = achievementRepository.getAllAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unlockedAchievements: StateFlow<List<Achievement>> = achievementRepository.getUnlockedAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lockedAchievements: StateFlow<List<Achievement>> = achievementRepository.getLockedAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unlockedCount: StateFlow<Int> = achievementRepository.getUnlockedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalCount: StateFlow<Int> = allAchievements.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        // ✅ Квест: "🏆 Переглянь досягнення" - виконується при відкритті екрану
        viewModelScope.launch {
            checkAndCompleteQuest("🏆 Переглянь досягнення")
            refreshAchievements()
        }
    }

    fun getAchievementsByCategory(category: AchievementCategory): StateFlow<List<Achievement>> {
        return allAchievements.map { achievements ->
            achievements.filter { it.category == category }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun refreshAchievements() {
        viewModelScope.launch {
            achievementTracker.onExpenseAdded()
        }
    }

    // ✅ Функція перевірки та виконання квестів
    private suspend fun checkAndCompleteQuest(questTitle: String) {
        val quests = database.questDao().getActiveQuests().first()
        val quest = quests.find { it.title == questTitle }

        quest?.let {
            if (!it.isCompleted) {
                database.questDao().updateQuestProgress(it.id, 1f)
                database.questDao().completeQuest(it.id, System.currentTimeMillis())

                val user = database.userDao().getCurrentUser().first()
                user?.let { currentUser ->
                    val newExp = currentUser.experience + it.reward
                    val newLevel = (kotlin.math.sqrt(newExp.toDouble() / 100.0)).toInt() + 1
                    val newTotalPoints = currentUser.totalPoints + it.reward

                    database.userDao().updateUser(
                        currentUser.copy(
                            experience = newExp,
                            level = newLevel,
                            totalPoints = newTotalPoints
                        )
                    )
                }
            }
        }
    }
}