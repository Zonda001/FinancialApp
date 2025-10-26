package com.example.financegame.data.local.database.dao

import androidx.room.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY startDate DESC")
    fun getAllQuests(): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE isCompleted = 0 ORDER BY startDate DESC")
    fun getActiveQuests(): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE isCompleted = 1 ORDER BY endDate DESC")
    fun getCompletedQuests(): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE id = :questId")
    fun getQuestById(questId: Int): Flow<Quest?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest): Long

    @Update
    suspend fun updateQuest(quest: Quest)

    @Delete
    suspend fun deleteQuest(quest: Quest)

    @Query("UPDATE quests SET progress = :progress WHERE id = :questId")
    suspend fun updateQuestProgress(questId: Int, progress: Float)

    @Query("UPDATE quests SET isCompleted = 1, endDate = :endDate WHERE id = :questId")
    suspend fun completeQuest(questId: Int, endDate: Long)

    // Новий метод для скидання квесту
    @Query("UPDATE quests SET isCompleted = 0, progress = 0, endDate = NULL WHERE id = :questId")
    suspend fun resetQuest(questId: Int)
}