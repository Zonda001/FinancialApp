package com.example.financegame.ui.screens.expenses

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    // SharedPreferences для збереження ліміту
    private val prefs = application.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
    private val EXPENSE_LIMIT_KEY = "expense_limit"

    // 🆕 Система відстеження досягнень
    private val achievementTracker = AchievementTracker(database, viewModelScope, getApplication())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    // 🆕 Ліміт витрат
    private val _expenseLimit = MutableStateFlow(prefs.getFloat(EXPENSE_LIMIT_KEY, 0f).toDouble())
    val expenseLimit: StateFlow<Double> = _expenseLimit

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

    // 🆕 Встановлення ліміту витрат
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

// Додаємо досвід за додавання витрати
            userRepository.addExperience(1, 10)

// 🆕 Відстежуємо досягнення
            achievementTracker.onExpenseAdded()

// ✅ ОНОВЛЮЄМО ПРОГРЕС КВЕСТІВ
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
    private fun updateQuestProgress() {
        viewModelScope.launch {
            // Оновлюємо прогрес всіх активних квестів
            val activeQuests = database.questDao().getActiveQuests().first()

            activeQuests.forEach { quest ->
                when (quest.title) {
                    "Перший крок" -> {
                        // Перевіряємо чи є хоча б одна витрата
                        val hasExpenses = database.expenseDao().getAllExpenses(1).first().isNotEmpty()
                        if (hasExpenses && quest.progress < 1f) {
                            database.questDao().updateQuestProgress(quest.id, 1f)
                        }
                    }
                    "П'ять транзакцій" -> {
                        // Рахуємо витрати за сьогодні
                        val calendar = java.util.Calendar.getInstance()
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        calendar.set(java.util.Calendar.MINUTE, 0)
                        val startOfDay = calendar.timeInMillis

                        val todayExpenses = database.expenseDao()
                            .getExpensesByDateRange(1, startOfDay, System.currentTimeMillis())
                            .first()
                            .filter { it.type == com.example.financegame.data.local.database.entities.ExpenseType.EXPENSE }
                            .size

                        val progress = (todayExpenses.toFloat() / 5f).coerceIn(0f, 1f)
                        database.questDao().updateQuestProgress(quest.id, progress)
                    }
                }
            }
        }
    }
}