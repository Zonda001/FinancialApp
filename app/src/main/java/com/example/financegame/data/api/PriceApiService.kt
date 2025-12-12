package com.example.financegame.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Сервіс для отримання реальних цін активів
 * Використовує безкоштовний API без потреби в ключах
 */
class PriceApiService {

    // Кеш для останніх успішно отриманих цін
    private val priceCache = ConcurrentHashMap<String, CachedPrice>()

    data class CachedPrice(
        val price: Double,
        val timestamp: Long
    )

    companion object {
        private const val CACHE_VALIDITY_MS = 30_000L // 30 секунд
    }

    /**
     * Отримати ціну з кешу якщо вона актуальна
     */
    private fun getCachedPrice(symbol: String): Double? {
        val cached = priceCache[symbol] ?: return null
        val age = System.currentTimeMillis() - cached.timestamp

        return if (age < CACHE_VALIDITY_MS) cached.price else null
    }

    /**
     * Зберегти ціну в кеш
     */
    private fun cachePrice(symbol: String, price: Double) {
        priceCache[symbol] = CachedPrice(price, System.currentTimeMillis())
    }

    /**
     * Отримати ціну криптовалюти
     * API: CoinGecko (безкоштовний, без ключа)
     */
    suspend fun getCryptoPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        try {
            val coinId = when (symbol) {
                "BTCUSD" -> "bitcoin"
                "ETHUSD" -> "ethereum"
                "BNBUSD" -> "binancecoin"
                "XRPUSD" -> "ripple"
                "SOLUSD" -> "solana"
                else -> return@withContext getCachedPrice(symbol)
            }

            val url = "https://api.coingecko.com/api/v3/simple/price?ids=$coinId&vs_currencies=usd"
            val response = URL(url).readText()
            val json = JSONObject(response)

            val price = json.getJSONObject(coinId).getDouble("usd")
            cachePrice(symbol, price)
            price
        } catch (e: Exception) {
            e.printStackTrace()
            // Повертаємо кешовану ціну при помилці
            getCachedPrice(symbol)
        }
    }

    /**
     * Отримати ціну валютної пари (Forex)
     * API: Frankfurter (безкоштовний ECB API)
     */
    suspend fun getForexPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        try {
            val (base, quote) = when (symbol) {
                "EURUSD" -> "EUR" to "USD"
                "GBPUSD" -> "GBP" to "USD"
                "USDJPY" -> "USD" to "JPY"
                "USDCHF" -> "USD" to "CHF"
                "AUDUSD" -> "AUD" to "USD"
                else -> return@withContext getCachedPrice(symbol)
            }

            val url = "https://api.frankfurter.app/latest?from=$base&to=$quote"
            val response = URL(url).readText()
            val json = JSONObject(response)

            val price = json.getJSONObject("rates").getDouble(quote)
            cachePrice(symbol, price)
            price
        } catch (e: Exception) {
            e.printStackTrace()
            getCachedPrice(symbol)
        }
    }

    /**
     * Отримати ціну акції
     * API: Yahoo Finance альтернатива (finnhub.io потребує ключ)
     * Для демо використовуємо симульовані ціни з реалістичною волатильністю
     */
    suspend fun getStockPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        try {
            // Базова ціна
            val basePrice = when (symbol) {
                "AAPL" -> 175.0
                "GOOGL" -> 140.0
                "MSFT" -> 380.0
                "TSLA" -> 250.0
                "AMZN" -> 155.0
                else -> return@withContext getCachedPrice(symbol)
            }

            // Якщо є кешована ціна, змінюємо її трохи для реалістичності
            val currentPrice = getCachedPrice(symbol) ?: basePrice

            // Симулюємо зміну ціни ±0.5%
            val volatility = 0.005
            val change = currentPrice * volatility * (Math.random() * 2 - 1)
            val newPrice = (currentPrice + change).coerceIn(basePrice * 0.8, basePrice * 1.2)

            cachePrice(symbol, newPrice)
            newPrice
        } catch (e: Exception) {
            e.printStackTrace()
            getCachedPrice(symbol)
        }
    }

    /**
     * Універсальний метод для отримання ціни будь-якого активу
     */
    suspend fun getAssetPrice(symbol: String, category: String): Double? {
        // Спочатку перевіряємо кеш
        val cached = getCachedPrice(symbol)

        // Намагаємось отримати свіжу ціну
        val freshPrice = when (category) {
            "CRYPTO" -> getCryptoPrice(symbol)
            "FOREX" -> getForexPrice(symbol)
            "STOCKS" -> getStockPrice(symbol)
            else -> null
        }

        // Повертаємо свіжу ціну або кешовану
        return freshPrice ?: cached
    }

    /**
     * Отримати кілька цін одночасно
     */
    suspend fun getMultiplePrices(symbols: List<Pair<String, String>>): Map<String, Double> {
        val prices = mutableMapOf<String, Double>()

        symbols.forEach { (symbol, category) ->
            getAssetPrice(symbol, category)?.let { price ->
                prices[symbol] = price
            }
        }

        return prices
    }

    /**
     * Симулювати зміну ціни (для тестування)
     */
    fun simulatePriceChange(currentPrice: Double, volatility: Double = 0.02): Double {
        val change = currentPrice * volatility * (Math.random() * 2 - 1)
        return (currentPrice + change).coerceAtLeast(0.01)
    }

    /**
     * Очистити кеш (якщо потрібно)
     */
    fun clearCache() {
        priceCache.clear()
    }

    /**
     * Отримати всі кешовані ціни
     */
    fun getAllCachedPrices(): Map<String, Double> {
        return priceCache.mapValues { it.value.price }
    }
}