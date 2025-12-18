package com.example.financegame.ui.screens.expenses

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.api.HuggingFaceOcrService
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.*
import com.example.financegame.data.repository.ExpenseRepository
import com.example.financegame.data.repository.UserRepository
import com.example.financegame.util.AchievementTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseRepository = ExpenseRepository(database.expenseDao())
    private val userRepository = UserRepository(database.userDao())

    private val prefs = application.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
    private val EXPENSE_LIMIT_KEY = "expense_limit"

    private val achievementTracker = AchievementTracker(database, viewModelScope, getApplication())
    private val ocrService = HuggingFaceOcrService()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _expenseLimit = MutableStateFlow(prefs.getFloat(EXPENSE_LIMIT_KEY, 0f).toDouble())
    val expenseLimit: StateFlow<Double> = _expenseLimit

    private val _isProcessingReceipt = MutableStateFlow(false)
    val isProcessingReceipt: StateFlow<Boolean> = _isProcessingReceipt

    private val _ocrResult = MutableStateFlow<HuggingFaceOcrService.ReceiptData?>(null)
    val ocrResult: StateFlow<HuggingFaceOcrService.ReceiptData?> = _ocrResult

    private val _ocrError = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError

    private val _showImagePicker = MutableStateFlow<ImagePickerType?>(null)
    val showImagePicker: StateFlow<ImagePickerType?> = _showImagePicker

    enum class ImagePickerType {
        CAMERA, GALLERY
    }

    val allExpenses: StateFlow<List<Expense>> = expenseRepository.getAllExpenses(1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMonthExpenses: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startOfMonth = calendar.timeInMillis

        expenseRepository.getTotalExpenses(1, startOfMonth, System.currentTimeMillis())
            .collect { emit(it ?: 0.0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val currentMonthIncome: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startOfMonth = calendar.timeInMillis

        expenseRepository.getTotalIncome(1, startOfMonth, System.currentTimeMillis())
            .collect { emit(it ?: 0.0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    init {
        viewModelScope.launch {
            val isConnected = ocrService.testConnection()
            if (isConnected) {
                println("✅ Hugging Face API connection OK")
            } else {
                println("⚠️ Hugging Face API connection failed")
            }
        }
    }

    fun setExpenseLimit(limit: Double) {
        viewModelScope.launch {
            _expenseLimit.value = limit
            prefs.edit().putFloat(EXPENSE_LIMIT_KEY, limit.toFloat()).apply()
        }
    }

    fun addExpense(
        amount: Double,
        category: String,
        type: ExpenseType,
        description: String = ""
    ) {
        viewModelScope.launch {
            val expense = Expense(
                userId = 1,
                amount = amount,
                category = category,
                type = type,
                description = description,
                date = System.currentTimeMillis()
            )
            expenseRepository.insertExpense(expense)
            userRepository.addExperience(1, 10)
            achievementTracker.onExpenseAdded()
            updateQuestProgress()
            _showAddDialog.value = false
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    fun showAddExpenseDialog() {
        _showAddDialog.value = true
    }

    fun hideAddExpenseDialog() {
        _showAddDialog.value = false
    }

    fun requestImagePicker(type: ImagePickerType) {
        _showImagePicker.value = type
    }

    fun clearImagePicker() {
        _showImagePicker.value = null
    }

    fun processReceiptImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isProcessingReceipt.value = true
            _ocrError.value = null
            _ocrResult.value = null

            try {
                println("📸 Starting Hugging Face OCR processing...")

                val result = ocrService.processReceipt(bitmap)

                if (result.success) {
                    println("✅ Hugging Face OCR SUCCESS: ${result.totalAmount} грн")
                    _ocrResult.value = result
                } else {
                    println("❌ Hugging Face OCR FAILED: ${result.error}")
                    _ocrError.value = result.error ?: "Не вдалося розпізнати чек"
                }
            } catch (e: Exception) {
                println("❌ Exception in processReceiptImage: ${e.message}")
                e.printStackTrace()
                _ocrError.value = "Помилка мережі: ${e.message}"
            } finally {
                _isProcessingReceipt.value = false
            }
        }
    }

    // ✅ ВИПРАВЛЕНО: Використовуємо "До сплати" як основну суму
    fun addExpenseFromReceipt(receiptData: HuggingFaceOcrService.ReceiptData, category: String) {
        viewModelScope.launch {
            // Пріоритет: do_splaty > totalAmount
            val finalAmount = receiptData.doSplaty?.replace(",", ".")?.toDoubleOrNull()
                ?: receiptData.totalAmount

            addExpense(
                amount = finalAmount,
                category = category,
                type = ExpenseType.EXPENSE,
                description = receiptData.merchantName ?: ""
            )
            clearOcrResult()
        }
    }

    fun clearOcrResult() {
        _ocrResult.value = null
        _ocrError.value = null
    }

    private fun updateQuestProgress() {
        viewModelScope.launch {
            val activeQuests = database.questDao().getActiveQuests().first()

            activeQuests.forEach { quest ->
                when (quest.title) {
                    "Перший крок" -> {
                        val hasExpenses = database.expenseDao().getAllExpenses(1).first().isNotEmpty()
                        if (hasExpenses && quest.progress < 1f) {
                            database.questDao().updateQuestProgress(quest.id, 1f)
                        }
                    }
                    "П'ять транзакцій" -> {
                        val today = getTodayDateString()
                        val todayExpenses = getExpensesCountForDate(today)

                        val progress = (todayExpenses.toFloat() / quest.targetAmount.toFloat()).coerceIn(0f, 1f)
                        database.questDao().updateQuestProgress(quest.id, progress)

                        if (todayExpenses >= quest.targetAmount.toInt()) {
                            database.questDao().updateQuestProgress(quest.id, 1f)
                        }
                    }
                }
            }
        }
    }

    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private suspend fun getExpensesCountForDate(dateString: String): Int {
        val calendar = parseDateString(dateString)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        return expenseRepository.getExpensesByDateRange(1, startOfDay, endOfDay)
            .first()
            .filter { it.type == ExpenseType.EXPENSE }
            .size
    }

    private fun parseDateString(dateString: String): Calendar {
        val parts = dateString.split("-")
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, parts[0].toInt())
        calendar.set(Calendar.MONTH, parts[1].toInt())
        calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
        return calendar
    }
}