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

    @Query("SELECT * FROM trading_positions WHERE userId = :userId AND status IN ('WON', 'LOST', 'CLOSED') ORDER BY openedAt DESC")
    fun getClosedPositions(userId: Int): Flow<List<TradingPosition>>

    @Query("SELECT * FROM trading_positions WHERE id = :positionId")
    fun getPositionById(positionId: Int): Flow<TradingPosition?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: TradingPosition): Long

    @Update
    suspend fun updatePosition(position: TradingPosition)

    @Query("UPDATE trading_positions SET currentPrice = :price WHERE id = :positionId")
    suspend fun updatePositionPrice(positionId: Int, price: Double)

    @Query("UPDATE trading_positions SET status = :status, profitLoss = :profitLoss WHERE id = :positionId")
    suspend fun closePosition(positionId: Int, status: PositionStatus, profitLoss: Int)

    @Delete
    suspend fun deletePosition(position: TradingPosition)

    @Query("DELETE FROM trading_positions WHERE userId = :userId")
    suspend fun deleteAllPositions(userId: Int)

    // Статистика
    @Query("SELECT SUM(profitLoss) FROM trading_positions WHERE userId = :userId AND status IN ('WON', 'LOST')")
    fun getTotalProfitLoss(userId: Int): Flow<Int?>

    @Query("SELECT COUNT(*) FROM trading_positions WHERE userId = :userId AND status = 'WON'")
    fun getWonPositionsCount(userId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM trading_positions WHERE userId = :userId AND status = 'LOST'")
    fun getLostPositionsCount(userId: Int): Flow<Int>
}