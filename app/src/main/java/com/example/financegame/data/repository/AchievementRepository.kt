package com.example.financegame.data.repository

import com.example.financegame.data.local.database.dao.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

class AchievementRepository(private val achievementDao: AchievementDao) {
    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    fun getUnlockedAchievements(): Flow<List<Achievement>> = achievementDao.getUnlockedAchievements()

    fun getLockedAchievements(): Flow<List<Achievement>> = achievementDao.getLockedAchievements()

    suspend fun updateAchievementProgress(achievementId: Int, progress: Int) =
        achievementDao.updateAchievementProgress(achievementId, progress)

    suspend fun unlockAchievement(achievementId: Int) =
        achievementDao.unlockAchievement(achievementId, System.currentTimeMillis())

    fun getUnlockedCount(): Flow<Int> = achievementDao.getUnlockedCount()
}
