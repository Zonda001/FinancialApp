package com.example.financegame.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.example.financegame.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class OcrService {

    data class ReceiptData(
        val totalAmount: Double,
        val products: List<Product>,
        val merchantName: String?,
        val success: Boolean,
        val error: String? = null
    )

    data class Product(
        val name: String,
        val price: Double,
        val quantity: Int = 1
    )

    companion object {
        // 🔐 API ключ тепер береться з BuildConfig (безпечно!)
        private val API_KEY = BuildConfig.GEMINI_API_KEY

        private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun processReceipt(bitmap: Bitmap): ReceiptData = withContext(Dispatchers.IO) {
        try {
            println("📸 Розпізнавання чеку через Gemini 2.5 Flash API...")

            // Конвертуємо bitmap в base64
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val imageBytes = stream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            println("📦 Image size: ${imageBytes.size / 1024}KB")

            // Створюємо JSON запит
            val prompt = """
                Проаналізуй цей український чек і поверни ТІЛЬКИ валідний JSON у такому форматі:
                {
                  "success": true,
                  "total": 123.45,
                  "merchant": "Назва магазину",
                  "products": [
                    {"name": "Товар 1", "price": 50.00, "quantity": 1},
                    {"name": "Товар 2", "price": 73.45, "quantity": 2}
                  ]
                }
                
                Правила:
                - total - це ЗАГАЛЬНА СУМА (шукай "СУМА", "До сплати", "РАЗОМ")
                - products - всі товари з чеку
                - Ціни БЕЗ "грн" (тільки числа)
                - Відповідай ЛИШЕ JSON, без жодного додаткового тексту
                - Якщо не можеш розпізнати: {"success": false, "error": "причина"}
            """.trimIndent()

            // Створюємо тіло запиту
            val partsArray = JSONArray()

            // Додаємо текстовий промпт
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            // Додаємо зображення
            val imagePart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mime_type", "image/jpeg")
            inlineData.put("data", base64Image)
            imagePart.put("inline_data", inlineData)
            partsArray.put(imagePart)

            // Створюємо content
            val contentItem = JSONObject()
            contentItem.put("parts", partsArray)

            val contentsArray = JSONArray()
            contentsArray.put(contentItem)

            val requestBody = JSONObject()
            requestBody.put("contents", contentsArray)

            println("📤 Sending request to Gemini 2.5 API...")

            // Відправляємо запит
            val request = Request.Builder()
                .url("$API_URL?key=$API_KEY")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            println("📥 Response code: ${response.code}")

            if (!response.isSuccessful) {
                println("❌ HTTP Error: ${response.code}")
                println("Response: $responseBody")

                if (response.code == 429) {
                    return@withContext ReceiptData(
                        totalAmount = 0.0,
                        products = emptyList(),
                        merchantName = null,
                        success = false,
                        error = "⚠️ Перевищено ліміт запитів. Зачекайте хвилину і спробуйте знову."
                    )
                }

                return@withContext ReceiptData(
                    totalAmount = 0.0,
                    products = emptyList(),
                    merchantName = null,
                    success = false,
                    error = "HTTP ${response.code}: $responseBody"
                )
            }

            if (responseBody == null) {
                return@withContext ReceiptData(
                    totalAmount = 0.0,
                    products = emptyList(),
                    merchantName = null,
                    success = false,
                    error = "Empty response"
                )
            }

            println("✅ Response received, parsing...")

            // Парсимо відповідь
            val json = JSONObject(responseBody)

            // Отримуємо текст з відповіді
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() == 0) {
                return@withContext ReceiptData(
                    totalAmount = 0.0,
                    products = emptyList(),
                    merchantName = null,
                    success = false,
                    error = "No response from AI"
                )
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")

            println("🤖 AI Response: ${text.take(200)}")

            // Очищаємо JSON від markdown
            val cleanedJson = text
                .replace("```json", "")
                .replace("```", "")
                .trim()

            println("🧹 Cleaned JSON: ${cleanedJson.take(200)}")

            val resultJson = JSONObject(cleanedJson)

            if (resultJson.getBoolean("success")) {
                println("✅ Receipt successfully recognized")

                val totalAmount = resultJson.getDouble("total")
                val merchantName = resultJson.optString("merchant", "")

                val productsArray = resultJson.getJSONArray("products")
                val products = mutableListOf<Product>()

                for (i in 0 until productsArray.length()) {
                    val item = productsArray.getJSONObject(i)
                    products.add(
                        Product(
                            name = item.getString("name"),
                            price = item.getDouble("price"),
                            quantity = item.optInt("quantity", 1)
                        )
                    )
                }

                println("💰 Total: $totalAmount грн")
                println("📦 Products: ${products.size}")

                ReceiptData(
                    totalAmount = totalAmount,
                    products = products,
                    merchantName = merchantName.ifEmpty { null },
                    success = true
                )
            } else {
                val errorMsg = resultJson.optString("error", "Не вдалося розпізнати чек")
                println("❌ Recognition failed: $errorMsg")
                ReceiptData(
                    totalAmount = 0.0,
                    products = emptyList(),
                    merchantName = null,
                    success = false,
                    error = errorMsg
                )
            }

        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            e.printStackTrace()
            ReceiptData(
                totalAmount = 0.0,
                products = emptyList(),
                merchantName = null,
                success = false,
                error = "Помилка розпізнавання: ${e.message}"
            )
        }
    }

    fun suggestCategory(merchantName: String?): String {
        return when {
            merchantName == null -> "Їжа"
            "сільпо" in merchantName.lowercase() -> "Їжа"
            "атб" in merchantName.lowercase() -> "Їжа"
            "novus" in merchantName.lowercase() -> "Їжа"
            "ашан" in merchantName.lowercase() -> "Їжа"
            "фора" in merchantName.lowercase() -> "Їжа"
            "еко маркет" in merchantName.lowercase() -> "Їжа"
            "аптека" in merchantName.lowercase() -> "Здоров'я"
            "rozetka" in merchantName.lowercase() -> "Інше"
            else -> "Їжа"
        }
    }
}