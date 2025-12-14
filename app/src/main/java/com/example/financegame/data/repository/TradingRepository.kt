package com.example.financegame.data.repository

import com.example.financegame.data.api.PriceApiService
import com.example.financegame.data.local.database.dao.TradingDao
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

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

    // ✅ ВИПРАВЛЕНО: Оновлюємо позицію з правильним P/L
    suspend fun closePosition(positionId: Int, status: PositionStatus, profitLoss: Int) {
        // Оновлюємо статус та P/L
        tradingDao.closePosition(positionId, status, profitLoss)

        println("💾 Position saved to history: ID=$positionId, Status=${status.name}, P/L=$profitLoss")
    }

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

    // ======================== CALCULATIONS ========================

    /**
     * ✅ ВИПРАВЛЕНО: Розрахунок прибутку/збитку для позиції з 10x леверіджем
     *
     * Формула: P/L = ставка × зміна_ціни% × 10
     *
     * Приклад:
     * - Ставка: 100 балів
     * - Ціна входу: 1000
     * - Поточна ціна: 1020 (+2%)
     * - LONG: 100 × 2% × 10 = +20 балів
     * - SHORT: 100 × (-2%) × 10 = -20 балів
     */
    fun calculateProfitLoss(position: TradingPosition): Int {
        val priceChange = position.currentPrice - position.entryPrice
        val changePercent = (priceChange / position.entryPrice) * 100

        println("📊 Calculating P/L:")
        println("   Symbol: ${position.symbol}")
        println("   Type: ${position.type.name}")
        println("   Entry: ${position.entryPrice}")
        println("   Current: ${position.currentPrice}")
        println("   Price change: ${String.format("%.4f", priceChange)}")
        println("   Change %: ${String.format("%.2f", changePercent)}%")

        // Для SHORT інвертуємо результат
        val effectiveChange = if (position.type == PositionType.SHORT) {
            -changePercent
        } else {
            changePercent
        }

        println("   Effective change (after type): ${String.format("%.2f", effectiveChange)}%")

        // Прибуток/збиток з 10x леверіджем
        val result = (position.amount * effectiveChange * LEVERAGE_MULTIPLIER / 100).toInt()

        println("   Amount: ${position.amount}")
        println("   Leverage: ${LEVERAGE_MULTIPLIER}x")
        println("   Final P/L: $result")

        return result
    }

    /**
     * Отримати поточний P/L для відображення в реальному часі
     */
    fun getCurrentProfitLoss(position: TradingPosition, currentPrice: Double): Int {
        val updatedPosition = position.copy(currentPrice = currentPrice)
        return calculateProfitLoss(updatedPosition)
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
        val currentTime = System.currentTimeMillis()
        val isExpired = currentTime >= position.closesAt

        if (isExpired) {
            val timeLeft = position.closesAt - currentTime
            println("⏰ Position ${position.symbol} expired (was due ${-timeLeft}ms ago)")
        }

        return isExpired
    }

    /**
     * Отримати множник леверіджу для відображення користувачу
     */
    fun getLeverageMultiplier(): Int = LEVERAGE_MULTIPLIER
}