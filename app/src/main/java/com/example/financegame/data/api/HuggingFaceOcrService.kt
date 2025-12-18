package com.example.financegame.data.api

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class HuggingFaceOcrService {

    data class ReceiptData(
        val totalAmount: Double,
        val items: List<ReceiptItem>,
        val date: String?,
        val merchantName: String?,
        val pdv: String?,
        val discount: String?,
        val doSplaty: String?,
        val bezgotivkova: String?,
        val success: Boolean,
        val error: String? = null
    )

    data class ReceiptItem(
        val name: String,
        val price: Double,
        val quantity: Int = 1,
        val confidence: Double = 0.0
    )

    companion object {
        // ✅ ВИПРАВЛЕНО: URL без зайвого слешу в кінці
        private const val BASE_URL = "https://zonda001-receipt-ocr.hf.space"
        private const val API_ENDPOINT = "$BASE_URL/api/ocr"

        private val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)  // Збільшено таймаут
            .readTimeout(180, TimeUnit.SECONDS)     // Збільшено таймаут
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)         // ✅ ДОДАНО: автоматичний retry
            .build()
    }

    /**
     * ✅ НОВИЙ МЕТОД: Розбудити Space якщо він заснув
     */
    suspend fun wakeUpSpace(): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🔥 Waking up Hugging Face Space...")

            // Викликаємо головну сторінку щоб розбудити Space
            val request = Request.Builder()
                .url(BASE_URL)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful

            if (success) {
                println("✅ Space is awake!")
                // Даємо час Space повністю запуститись
                kotlinx.coroutines.delay(3000)
            } else {
                println("⚠️ Wake-up response: ${response.code}")
            }

            response.close()
            success
        } catch (e: Exception) {
            println("⚠️ Wake-up failed: ${e.message}")
            false
        }
    }

    /**
     * ✅ НОВИЙ МЕТОД: Перевірка здоров'я API
     */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🏥 Checking API health...")

            val request = Request.Builder()
                .url("$BASE_URL/health")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful) {
                println("✅ API Health: OK")
                println("Response: $body")
                return@withContext true
            } else {
                println("❌ API Health check failed: ${response.code}")
                println("Response: $body")
                return@withContext false
            }
        } catch (e: Exception) {
            println("❌ Health check error: ${e.message}")
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * ✅ ВИПРАВЛЕНО: Розпізнає чек з автоматичним wake-up
     */
    suspend fun processReceipt(bitmap: Bitmap): ReceiptData = withContext(Dispatchers.IO) {
        try {
            println("📸 Starting Hugging Face OCR processing...")

            // 1️⃣ Спочатку розбудимо Space
            println("⏰ Step 1: Waking up Space...")
            wakeUpSpace()

            // 2️⃣ Перевіримо здоров'я API
            println("🏥 Step 2: Checking API health...")
            val isHealthy = checkHealth()
            if (!isHealthy) {
                println("⚠️ API is not healthy, but continuing anyway...")
            }

            // 3️⃣ Конвертуємо Bitmap в JPEG
            println("🖼️ Step 3: Converting image...")
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val imageBytes = stream.toByteArray()
            println("📦 Image size: ${imageBytes.size / 1024}KB")

            // 4️⃣ Створюємо multipart request
            println("📤 Step 4: Preparing request...")
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "receipt.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            // 5️⃣ Відправляємо запит (БЕЗ слешу в кінці!)
            println("🚀 Step 5: Sending request to: $API_ENDPOINT")
            val request = Request.Builder()
                .url(API_ENDPOINT)  // ✅ БЕЗ "/" в кінці
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            println("📥 Response code: ${response.code}")
            println("📥 Response message: ${response.message}")

            if (!response.isSuccessful) {
                println("❌ HTTP Error: ${response.code}")
                println("❌ Response body: $responseBody")

                return@withContext ReceiptData(
                    totalAmount = 0.0,
                    items = emptyList(),
                    date = null,
                    merchantName = null,
                    pdv = null,
                    discount = null,
                    doSplaty = null,
                    bezgotivkova = null,
                    success = false,
                    error = "HTTP ${response.code}: ${response.message}\n$responseBody"
                )
            }

            if (responseBody == null) {
                println("❌ Empty response body")
                return@withContext ReceiptData(
                    totalAmount = 0.0,
                    items = emptyList(),
                    date = null,
                    merchantName = null,
                    pdv = null,
                    discount = null,
                    doSplaty = null,
                    bezgotivkova = null,
                    success = false,
                    error = "Empty response from server"
                )
            }

            println("✅ Response received, parsing...")
            println("Raw response (first 500 chars): ${responseBody.take(500)}")

            // 6️⃣ Парсимо відповідь
            val json = JSONObject(responseBody)

            if (json.getBoolean("success")) {
                println("✅ OCR SUCCESS")

                val receiptObj = json.getJSONObject("receipt")
                val suma = parseUkrainianNumber(receiptObj.optString("suma", "0"))
                val pdv = receiptObj.optString("pdv")
                val discount = receiptObj.optString("discount")
                val doSplaty = receiptObj.optString("do_splaty")
                val bezgotivkova = receiptObj.optString("bezgotivkova")

                val itemsArray = receiptObj.optJSONArray("items") ?: JSONArray()
                val items = parseItems(itemsArray)

                val meta = json.optJSONObject("meta")
                val filename = meta?.optString("filename")

                println("💰 Total: $suma грн")
                println("📝 Items found: ${items.size}")

                ReceiptData(
                    totalAmount = suma,
                    items = items,
                    date = null,
                    merchantName = filename,
                    pdv = pdv,
                    discount = discount,
                    doSplaty = doSplaty,
                    bezgotivkova = bezgotivkova,
                    success = true
                )
            } else {
                val errorMsg = json.optString("error", "Unknown error")
                println("❌ OCR Failed: $errorMsg")
                ReceiptData(
                    totalAmount = 0.0,
                    items = emptyList(),
                    date = null,
                    merchantName = null,
                    pdv = null,
                    discount = null,
                    doSplaty = null,
                    bezgotivkova = null,
                    success = false,
                    error = errorMsg
                )
            }

        } catch (e: Exception) {
            println("❌ Exception in processReceipt: ${e.message}")
            e.printStackTrace()
            ReceiptData(
                totalAmount = 0.0,
                items = emptyList(),
                date = null,
                merchantName = null,
                pdv = null,
                discount = null,
                doSplaty = null,
                bezgotivkova = null,
                success = false,
                error = "Error: ${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun parseUkrainianNumber(text: String?): Double {
        if (text.isNullOrBlank()) return 0.0

        return try {
            val cleaned = text
                .replace("грн", "")
                .replace("ГРН", "")
                .replace(" ", "")
                .replace(",", ".")
                .trim()

            cleaned.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            println("⚠️ Failed to parse number: $text")
            0.0
        }
    }

    private fun parseItems(jsonArray: JSONArray): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()

        for (i in 0 until jsonArray.length()) {
            try {
                val item = jsonArray.getJSONObject(i)
                val name = item.optString("name", "Товар")
                val priceStr = item.optString("price", "0")
                val price = parseUkrainianNumber(priceStr)
                val confidence = item.optDouble("confidence", 0.0)

                items.add(
                    ReceiptItem(
                        name = name,
                        price = price,
                        quantity = 1,
                        confidence = confidence
                    )
                )

                println("  ✅ Item: $name - $price грн (confidence: ${String.format("%.2f", confidence)})")
            } catch (e: Exception) {
                println("⚠️ Failed to parse item $i: ${e.message}")
            }
        }

        return items
    }

    fun suggestCategory(merchantName: String?): String {
        return when {
            merchantName == null -> "Їжа"
            "сільпо" in merchantName.lowercase() -> "Їжа"
            "атб" in merchantName.lowercase() -> "Їжа"
            "novus" in merchantName.lowercase() -> "Їжа"
            "аптека" in merchantName.lowercase() -> "Здоров'я"
            "rozetka" in merchantName.lowercase() -> "Інше"
            else -> "Їжа"
        }
    }

    /**
     * ✅ ВИПРАВЛЕНО: Тест з'єднання з детальним логуванням
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🔍 Testing connection to Hugging Face API...")
            println("Base URL: $BASE_URL")

            // Спочатку розбудимо Space
            wakeUpSpace()

            // Потім перевіримо здоров'я
            val isHealthy = checkHealth()

            if (isHealthy) {
                println("✅ Connection test PASSED")
            } else {
                println("⚠️ Connection test FAILED")
            }

            isHealthy
        } catch (e: Exception) {
            println("❌ Connection test error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}