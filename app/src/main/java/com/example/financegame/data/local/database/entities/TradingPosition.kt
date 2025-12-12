package com.example.financegame.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// ======================== TRADING POSITIONS ========================
@Entity(tableName = "trading_positions")
data class TradingPosition(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 1,
    val symbol: String,                    // BTC/USD, EUR/USD, AAPL тощо
    val type: PositionType,                // LONG або SHORT
    val entryPrice: Double,                // Ціна відкриття
    val currentPrice: Double = entryPrice, // Поточна ціна
    val amount: Int,                       // Кількість балів
    val duration: TradingDuration,         // 1h, 24h, 7d
    val openedAt: Long = System.currentTimeMillis(),
    val closesAt: Long,                    // Час закриття
    val status: PositionStatus = PositionStatus.ACTIVE,
    val profitLoss: Int = 0                // Прибуток/Збиток в балах
)

enum class PositionType {
    LONG,   // Ставка на зростання
    SHORT   // Ставка на падіння
}

enum class TradingDuration(val displayName: String, val hours: Long) {
    ONE_HOUR("1 година", 1),
    SIX_HOURS("6 годин", 6),
    ONE_DAY("1 день", 24),
    THREE_DAYS("3 дні", 72),
    ONE_WEEK("1 тиждень", 168)
}

enum class PositionStatus {
    ACTIVE,     // Активна
    WON,        // Виграна
    LOST,       // Програна
    CLOSED      // Закрита достроково
}

// ======================== TRADING ASSETS ========================
data class TradingAsset(
    val symbol: String,
    val name: String,
    val category: AssetCategory,
    val icon: String,
    val currentPrice: Double = 0.0,
    val priceChange24h: Double = 0.0
)

enum class AssetCategory(val displayName: String) {
    CRYPTO("Криптовалюти"),
    FOREX("Валюти"),
    STOCKS("Акції")
}

// Популярні активи
object DefaultTradingAssets {
    val assets = listOf(
        // Криптовалюти
        TradingAsset("BTCUSD", "Bitcoin", AssetCategory.CRYPTO, "₿"),
        TradingAsset("ETHUSD", "Ethereum", AssetCategory.CRYPTO, "Ξ"),
        TradingAsset("BNBUSD", "Binance Coin", AssetCategory.CRYPTO, "💎"),
        TradingAsset("XRPUSD", "Ripple", AssetCategory.CRYPTO, "🌊"),
        TradingAsset("SOLUSD", "Solana", AssetCategory.CRYPTO, "◎"),

        // Валюти (Forex)
        TradingAsset("EURUSD", "EUR/USD", AssetCategory.FOREX, "🇪🇺"),
        TradingAsset("GBPUSD", "GBP/USD", AssetCategory.FOREX, "🇬🇧"),
        TradingAsset("USDJPY", "USD/JPY", AssetCategory.FOREX, "🇯🇵"),
        TradingAsset("USDCHF", "USD/CHF", AssetCategory.FOREX, "🇨🇭"),
        TradingAsset("AUDUSD", "AUD/USD", AssetCategory.FOREX, "🇦🇺"),

        // Акції
        TradingAsset("AAPL", "Apple", AssetCategory.STOCKS, "🍎"),
        TradingAsset("GOOGL", "Google", AssetCategory.STOCKS, "🔍"),
        TradingAsset("MSFT", "Microsoft", AssetCategory.STOCKS, "💻"),
        TradingAsset("TSLA", "Tesla", AssetCategory.STOCKS, "⚡"),
        TradingAsset("AMZN", "Amazon", AssetCategory.STOCKS, "📦")
    )
}