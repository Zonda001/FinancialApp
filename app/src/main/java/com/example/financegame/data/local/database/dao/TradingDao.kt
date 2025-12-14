package com.example.financegame.data.local.database.dao

import androidx.room.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDao {
    @Query("SELECT * FROM trading_positions WHERE userId = :userId ORDER BY openedAt DESC")
    fun getAllPositions(userId: Int): Flow<List<TradingPosition>>

    @Query("SELECT * FROM trading_positions WHERE userId = :userId AND status = 'ACTIVE' ORDER BY openedAt DESC")
    fun getActivePositions(userId: Int): Flow<List<TradingPosition>>

    // ✅ ВИПРАВЛЕНО: Сортування за часом закриття, щоб нові позиції були зверху
    @Query("SELECT * FROM trading_positions WHERE userId = :userId AND status IN ('WON', 'LOST', 'CLOSED') ORDER BY closesAt DESC")
    fun getClosedPositions(userId: Int): Flow<List<TradingPosition>>

    @Query("SELECT * FROM trading_positions WHERE id = :positionId LIMIT 1")
    suspend fun getPositionById(positionId: Int): TradingPosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: TradingPosition): Long

    @Update
    suspend fun updatePosition(position: TradingPosition)

    @Query("UPDATE trading_positions SET currentPrice = :price WHERE id = :positionId")
    suspend fun updatePositionPrice(positionId: Int, price: Double)

    // ✅ ВИПРАВЛЕНО: Оновлюємо і час закриття при закритті позиції
    @Query("""
        UPDATE trading_positions 
        SET status = :status, 
            profitLoss = :profitLoss,
            closesAt = :closedAt
        WHERE id = :positionId
    """)
    suspend fun closePosition(positionId: Int, status: PositionStatus, profitLoss: Int, closedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deletePosition(position: TradingPosition)

    @Query("DELETE FROM trading_positions WHERE userId = :userId")
    suspend fun deleteAllPositions(userId: Int)

    // ✅ ВИПРАВЛЕНО: Статистика тільки для WON/LOST позицій
    @Query("SELECT SUM(profitLoss) FROM trading_positions WHERE userId = :userId AND status IN ('WON', 'LOST')")
    fun getTotalProfitLoss(userId: Int): Flow<Int?>

    @Query("SELECT COUNT(*) FROM trading_positions WHERE userId = :userId AND status = 'WON'")
    fun getWonPositionsCount(userId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM trading_positions WHERE userId = :userId AND status = 'LOST'")
    fun getLostPositionsCount(userId: Int): Flow<Int>

    // ✅ ДОДАНО: Загальна кількість закритих позицій для статистики
    @Query("SELECT COUNT(*) FROM trading_positions WHERE userId = :userId AND status IN ('WON', 'LOST', 'CLOSED')")
    fun getTotalClosedPositionsCount(userId: Int): Flow<Int>
}