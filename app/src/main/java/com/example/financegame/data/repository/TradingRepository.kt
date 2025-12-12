package com.example.financegame.data.repository

import com.example.financegame.data.api.PriceApiService
import com.example.financegame.data.local.database.dao.TradingDao
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TradingRepository(
    private val tradingDao: TradingDao,
    private val priceApiService: PriceApiService
) {
    companion object {
        // Множник для прибутків/збитків (10x левередж)
        private const val LEVERAGE_MULTIPLIER = 10
    }

    // ======================== POSITIONS ========================

    fun getAllPositions(userId: Int): Flow<List<TradingPosition>> =
        tradingDao.getAllPositions(userId)

    fun getActivePositions(userId: Int): Flow<List<TradingPosition>> =
        tradingDao.getActivePositions(userId)

    fun getClosedPositions(userId: Int): Flow<List<TradingPosition>> =
        tradingDao.getClosedPositions(userId)

    suspend fun openPosition(position: TradingPosition): Long =
        tradingDao.insertPosition(position)

    suspend fun updatePosition(position: TradingPosition) =
        tradingDao.updatePosition(position)

    suspend fun closePosition(positionId: Int, status: PositionStatus, profitLoss: Int) =
        tradingDao.closePosition(positionId, status, profitLoss)

    // ======================== PRICES ========================

    suspend fun getCurrentPrice(symbol: String, category: AssetCategory): Double? {
        return priceApiService.getAssetPrice(symbol, category.name)
    }

    suspend fun updatePositionPrices(positions: List<TradingPosition>) {
        positions.forEach { position ->
            val category = DefaultTradingAssets.assets
                .find { it.symbol == position.symbol }
                ?.category ?: return@forEach

            val currentPrice = getCurrentPrice(position.symbol, category) ?: return@forEach
            tradingDao.updatePositionPrice(position.id, currentPrice)
        }
    }

    // ======================== STATISTICS ========================

    fun getTotalProfitLoss(userId: Int): Flow<Int?> =
        tradingDao.getTotalProfitLoss(userId)

    fun getWonPositionsCount(userId: Int): Flow<Int> =
        tradingDao.getWonPositionsCount(userId)

    fun getLostPositionsCount(userId: Int): Flow<Int> =
        tradingDao.getLostPositionsCount(userId)

    // ======================== CALCULATIONS ========================

    /**
     * Розрахувати прибуток/збиток для позиції з 10x леверіджем
     *
     * Формула: P/L = ставка × зміна_ціни% × 10
     *
     * Приклад:
     * - Ставка: 100 балів
     * - Зміна ціни: +2%
     * - LONG: 100 × 2% × 10 = +20 балів
     * - SHORT: 100 × (-2%) × 10 = -20 балів
     */
    fun calculateProfitLoss(position: TradingPosition): Int {
        val priceChange = position.currentPrice - position.entryPrice
        val changePercent = (priceChange / position.entryPrice) * 100

        // Для SHORT інвертуємо результат
        val effectiveChange = if (position.type == PositionType.SHORT) {
            -changePercent
        } else {
            changePercent
        }

        // Прибуток/збиток з 10x леверіджем
        return (position.amount * effectiveChange * LEVERAGE_MULTIPLIER / 100).toInt()
    }

    /**
     * Отримати поточний P/L для відображення в реальному часі
     */
    fun getCurrentProfitLoss(position: TradingPosition, currentPrice: Double): Int {
        val priceChange = currentPrice - position.entryPrice
        val changePercent = (priceChange / position.entryPrice) * 100

        val effectiveChange = if (position.type == PositionType.SHORT) {
            -changePercent
        } else {
            changePercent
        }

        return (position.amount * effectiveChange * LEVERAGE_MULTIPLIER / 100).toInt()
    }

    /**
     * Отримати відсоток зміни ціни з урахуванням леверіджу
     */
    fun getEffectiveChangePercent(position: TradingPosition, currentPrice: Double): Double {
        val priceChange = currentPrice - position.entryPrice
        val changePercent = (priceChange / position.entryPrice) * 100

        val effectiveChange = if (position.type == PositionType.SHORT) {
            -changePercent
        } else {
            changePercent
        }

        return effectiveChange * LEVERAGE_MULTIPLIER
    }

    /**
     * Перевірити чи закінчилась позиція
     */
    fun isPositionExpired(position: TradingPosition): Boolean {
        return System.currentTimeMillis() >= position.closesAt
    }

    /**
     * Автоматично закрити прострочені позиції
     */
    suspend fun closeExpiredPositions(userId: Int) {
        val activePositions = tradingDao.getActivePositions(userId).first()

        activePositions.forEach { position ->
            if (isPositionExpired(position)) {
                val profitLoss = calculateProfitLoss(position)
                val status = if (profitLoss >= 0) PositionStatus.WON else PositionStatus.LOST

                closePosition(position.id, status, profitLoss)
            }
        }
    }

    /**
     * Отримати множник леверіджу для відображення користувачу
     */
    fun getLeverageMultiplier(): Int = LEVERAGE_MULTIPLIER
}