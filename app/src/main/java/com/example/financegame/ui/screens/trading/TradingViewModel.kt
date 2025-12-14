package com.example.financegame.ui.screens.trading

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.api.PriceApiService
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.*
import com.example.financegame.data.repository.TradingRepository
import com.example.financegame.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TradingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val priceApiService = PriceApiService()
    private val tradingRepository = TradingRepository(database.tradingDao(), priceApiService)
    private val userRepository = UserRepository(database.userDao())

    private val _selectedAsset = MutableStateFlow<TradingAsset?>(null)
    val selectedAsset: StateFlow<TradingAsset?> = _selectedAsset

    private val _assetPrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val assetPrices: StateFlow<Map<String, Double>> = _assetPrices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var priceUpdateJob: Job? = null
    private var positionCheckJob: Job? = null

    val activePositions: StateFlow<List<TradingPosition>> =
        tradingRepository.getActivePositions(1)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val closedPositions: StateFlow<List<TradingPosition>> =
        tradingRepository.getClosedPositions(1)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val currentUser = userRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ✅ ВИПРАВЛЕНО: Правильний розрахунок P/L тільки для закритих позицій
    val totalProfitLoss: StateFlow<Int> = closedPositions
        .map { positions ->
            positions.sumOf { it.profitLoss }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // ✅ ВИПРАВЛЕНО: Win rate тільки для WON/LOST позицій
    val winRate: StateFlow<Float> = closedPositions
        .map { positions ->
            val wonAndLost = positions.filter {
                it.status == PositionStatus.WON || it.status == PositionStatus.LOST
            }
            val won = wonAndLost.count { it.status == PositionStatus.WON }
            val total = wonAndLost.size

            if (total > 0) (won.toFloat() / total.toFloat()) * 100f else 0f
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    init {
        startPriceUpdates()
        startPositionChecks()
    }

    // ======================== PRICE UPDATES ========================

    private fun startPriceUpdates() {
        priceUpdateJob?.cancel()
        priceUpdateJob = viewModelScope.launch {
            updateAllPrices()

            while (true) {
                delay(30000)
                println("⏰ Starting price update cycle...")
                updateAllPrices()
                updateActivePositions()
                println("✅ Price update cycle completed")
            }
        }
    }

    private suspend fun updateAllPrices() {
        try {
            val updatedPrices = _assetPrices.value.toMutableMap()

            println("🔄 Updating prices for ${DefaultTradingAssets.assets.size} assets...")

            DefaultTradingAssets.assets.forEach { asset ->
                priceApiService.getAssetPrice(asset.symbol, asset.category.name)?.let { price ->
                    updatedPrices[asset.symbol] = price
                    println("  ✅ ${asset.symbol}: $price")
                } ?: println("  ⚠️ ${asset.symbol}: no price received")
            }

            _assetPrices.value = updatedPrices
            println("📊 Total prices in map: ${updatedPrices.size}")
        } catch (e: Exception) {
            println("❌ Error updating prices: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun updateActivePositions() {
        val positions = activePositions.value
        if (positions.isEmpty()) {
            println("⚠️ No active positions to update")
            return
        }

        println("🔄 Updating ${positions.size} active positions...")

        positions.forEach { position ->
            val currentPrice = _assetPrices.value[position.symbol]
            if (currentPrice != null && currentPrice != position.currentPrice) {
                try {
                    val updatedPosition = position.copy(currentPrice = currentPrice)
                    tradingRepository.updatePosition(updatedPosition)
                    println("  ✅ ${position.symbol}: ${position.currentPrice} → $currentPrice")
                } catch (e: Exception) {
                    println("  ❌ Failed to update ${position.symbol}: ${e.message}")
                }
            }
        }
    }

    fun refreshPrices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                updateAllPrices()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ======================== POSITIONS ========================

    fun selectAsset(asset: TradingAsset) {
        _selectedAsset.value = asset
    }

    // ✅ ВИПРАВЛЕНО: Правильне віднімання балів при відкритті позиції
    fun openPosition(
        asset: TradingAsset,
        type: PositionType,
        amount: Int,
        duration: TradingDuration
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch

            // Перевіряємо чи достатньо балів
            if (user.totalPoints < amount) {
                println("❌ Недостатньо балів: потрібно $amount, є ${user.totalPoints}")
                return@launch
            }

            val currentPrice = _assetPrices.value[asset.symbol]
            if (currentPrice == null) {
                println("❌ Ціна для ${asset.symbol} недоступна")
                return@launch
            }

            val closesAt = System.currentTimeMillis() + (duration.hours * 60 * 60 * 1000)

            val position = TradingPosition(
                userId = 1,
                symbol = asset.symbol,
                type = type,
                entryPrice = currentPrice,
                currentPrice = currentPrice,
                amount = amount,
                duration = duration,
                closesAt = closesAt,
                status = PositionStatus.ACTIVE,
                profitLoss = 0
            )

            println("📈 Opening position: ${asset.symbol} ${type.name} $amount at $currentPrice")

            try {
                tradingRepository.openPosition(position)

                // ✅ ВИПРАВЛЕНО: Віднімаємо бали одразу після відкриття
                val newPoints = user.totalPoints - amount
                userRepository.updateUser(user.copy(totalPoints = newPoints))

                println("✅ Position opened. New balance: $newPoints")
            } catch (e: Exception) {
                println("❌ Error opening position: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ✅ ВИПРАВЛЕНО: Правильний розрахунок при достроковому закритті
    fun closePositionEarly(position: TradingPosition) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch

            // Використовуємо поточну ціну з _assetPrices
            val currentPrice = _assetPrices.value[position.symbol] ?: position.currentPrice

            // Оновлюємо позицію з поточною ціною
            val updatedPosition = position.copy(currentPrice = currentPrice)

            val profitLoss = tradingRepository.calculateProfitLoss(updatedPosition)

            println("🔒 Closing position early: ${position.symbol}")
            println("   Entry: ${position.entryPrice}, Current: $currentPrice")
            println("   Amount: ${position.amount}, P/L: $profitLoss")

            try {
                tradingRepository.closePosition(
                    position.id,
                    PositionStatus.CLOSED,
                    profitLoss
                )

                // ✅ ВИПРАВЛЕНО: Повертаємо початкову ставку + прибуток/збиток
                val returnAmount = position.amount + profitLoss
                val newPoints = (user.totalPoints + returnAmount).coerceAtLeast(0)

                userRepository.updateUser(user.copy(totalPoints = newPoints))

                println("✅ Position closed. Returned: $returnAmount, New balance: $newPoints")
            } catch (e: Exception) {
                println("❌ Error closing position: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ✅ ВИПРАВЛЕНО: Окрема функція для перевірки прострочених позицій
    private fun startPositionChecks() {
        positionCheckJob?.cancel()
        positionCheckJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Перевірка кожні 5 секунд
                checkExpiredPositions()
            }
        }
    }

    // ✅ ВИПРАВЛЕНО: Правильна перевірка часу закриття
    private suspend fun checkExpiredPositions() {
        val currentTime = System.currentTimeMillis()
        val positions = activePositions.value

        if (positions.isEmpty()) return

        positions.forEach { position ->
            // Перевіряємо чи минув час закриття
            if (currentTime >= position.closesAt) {
                println("⏰ Position expired: ${position.symbol}")

                // Використовуємо поточну ціну
                val currentPrice = _assetPrices.value[position.symbol] ?: position.currentPrice
                val updatedPosition = position.copy(currentPrice = currentPrice)

                val profitLoss = tradingRepository.calculateProfitLoss(updatedPosition)
                val status = if (profitLoss >= 0) PositionStatus.WON else PositionStatus.LOST

                println("   Entry: ${position.entryPrice}, Final: $currentPrice")
                println("   P/L: $profitLoss, Status: ${status.name}")

                try {
                    tradingRepository.closePosition(position.id, status, profitLoss)

                    currentUser.value?.let { user ->
                        // ✅ ВИПРАВЛЕНО: Повертаємо початкову ставку + прибуток/збиток
                        val returnAmount = position.amount + profitLoss
                        val newPoints = (user.totalPoints + returnAmount).coerceAtLeast(0)

                        userRepository.updateUser(user.copy(totalPoints = newPoints))

                        println("✅ Position expired and closed. Returned: $returnAmount, New balance: $newPoints")
                    }
                } catch (e: Exception) {
                    println("❌ Error closing expired position: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        priceUpdateJob?.cancel()
        positionCheckJob?.cancel()
    }
}