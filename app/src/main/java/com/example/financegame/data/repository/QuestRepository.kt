package com.example.financegame.data.repository

import com.example.financegame.data.local.database.dao.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

class QuestRepository(private val questDao: QuestDao) {
    fun getAllQuests(): Flow<List<Quest>> = questDao.getAllQuests()

    fun getActiveQuests(): Flow<List<Quest>> = questDao.getActiveQuests()

    fun getCompletedQuests(): Flow<List<Quest>> = questDao.getCompletedQuests()

    suspend fun insertQuest(quest: Quest) = questDao.insertQuest(quest)

    suspend fun updateQuest(quest: Quest) = questDao.updateQuest(quest)

    suspend fun updateQuestProgress(questId: Int, progress: Float) =
        questDao.updateQuestProgress(questId, progress)

    suspend fun completeQuest(questId: Int) =
        questDao.completeQuest(questId, System.currentTimeMillis())

    suspend fun resetQuest(questId: Int) = questDao.resetQuest(questId)
}