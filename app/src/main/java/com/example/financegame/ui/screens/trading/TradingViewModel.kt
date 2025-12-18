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

    // Зберігаємо останні відомі ціни, щоб вони не зникали під час оновлення
    private val _assetPrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val assetPrices: StateFlow<Map<String, Double>> = _assetPrices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var priceUpdateJob: Job? = null

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

    val totalProfitLoss: StateFlow<Int> = tradingRepository.getTotalProfitLoss(1)
        .map { it ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val winRate: StateFlow<Float> = combine(
        tradingRepository.getWonPositionsCount(1),
        tradingRepository.getLostPositionsCount(1)
    ) { won, lost ->
        val total = won + lost
        if (total > 0) (won.toFloat() / total.toFloat()) * 100f else 0f
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    init {
        startPriceUpdates()
        checkExpiredPositions()
    }

    // ======================== PRICE UPDATES ========================

    private fun startPriceUpdates() {
        priceUpdateJob?.cancel()
        priceUpdateJob = viewModelScope.launch {
            // Початкове завантаження
            updateAllPrices()

            while (true) {
                delay(30000) // Оновлення кожні 30 секунд
                println("⏰ Starting price update cycle...")
                updateAllPrices()
                updateActivePositions()
                println("✅ Price update cycle completed")
            }
        }
    }

    private suspend fun updateAllPrices() {
        try {
            // Створюємо нову мапу на основі старої, щоб зберегти старі ціни
            val updatedPrices = _assetPrices.value.toMutableMap()

            println("🔄 Updating prices for ${DefaultTradingAssets.assets.size} assets...")

            DefaultTradingAssets.assets.forEach { asset ->
                priceApiService.getAssetPrice(asset.symbol, asset.category.name)?.let { price ->
                    // Оновлюємо тільки якщо отримали нову ціну
                    updatedPrices[asset.symbol] = price
                    println("  ✅ ${asset.symbol}: $price")
                } ?: println("  ⚠️ ${asset.symbol}: no price received")
                // Якщо ціна не отримана - залишаємо стару
            }

            _assetPrices.value = updatedPrices
            println("📊 Total prices in map: ${updatedPrices.size}")
        } catch (e: Exception) {
            println("❌ Error updating prices: ${e.message}")
            e.printStackTrace()
            // При помилці просто залишаємо старі ціни
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
                    // Оновлюємо через repository щоб Room емітив зміни
                    val updatedPosition = position.copy(currentPrice = currentPrice)
                    tradingRepository.updatePosition(updatedPosition)
                    println("  ✅ ${position.symbol}: ${position.currentPrice} → $currentPrice")
                } catch (e: Exception) {
                    println("  ❌ Failed to update ${position.symbol}: ${e.message}")
                }
            } else if (currentPrice == null) {
                println("  ⚠️ No price for ${position.symbol}")
            } else {
                println("  ℹ️ ${position.symbol}: price unchanged ($currentPrice)")
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
                return@launch
            }

            val currentPrice = _assetPrices.value[asset.symbol] ?: return@launch
            val closesAt = System.currentTimeMillis() + (duration.hours * 60 * 60 * 1000)

            val position = TradingPosition(
                userId = 1,
                symbol = asset.symbol,
                type = type,
                entryPrice = currentPrice,
                currentPrice = currentPrice,
                amount = amount,
                duration = duration,
                closesAt = closesAt
            )

            tradingRepository.openPosition(position)

            // Віднімаємо бали
            userRepository.updateUser(user.copy(totalPoints = user.totalPoints - amount))
        }
    }

    fun closePositionEarly(position: TradingPosition) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch

            val profitLoss = tradingRepository.calculateProfitLoss(position)

            tradingRepository.closePosition(
                position.id,
                PositionStatus.CLOSED,
                profitLoss
            )

            // Повертаємо бали + прибуток/збиток (але не менше 0)
            val newPoints = user.totalPoints + position.amount + profitLoss
            userRepository.updateUser(user.copy(totalPoints = newPoints.coerceAtLeast(0)))
        }
    }

    private fun checkExpiredPositions() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Перевірка кожні 10 секунд

                activePositions.value.forEach { position ->
                    if (tradingRepository.isPositionExpired(position)) {
                        val profitLoss = tradingRepository.calculateProfitLoss(position)
                        val status = if (profitLoss >= 0) PositionStatus.WON else PositionStatus.LOST

                        tradingRepository.closePosition(position.id, status, profitLoss)

                        // Повертаємо бали
                        currentUser.value?.let { user ->
                            val newPoints = user.totalPoints + position.amount + profitLoss
                            userRepository.updateUser(user.copy(totalPoints = newPoints.coerceAtLeast(0)))
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        priceUpdateJob?.cancel()
    }
}