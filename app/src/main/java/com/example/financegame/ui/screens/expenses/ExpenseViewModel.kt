package com.example.financegame.ui.screens.expenses

import android.app.Application
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

    // 🆕 Система відстеження досягнень
    private val achievementTracker = AchievementTracker(database, viewModelScope)

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

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

            // Перевіряємо денні ліміти
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfDay = calendar.timeInMillis

            val dailyExpense = expenseRepository.getTotalExpenses(1, startOfDay, System.currentTimeMillis()).first() ?: 0.0
            achievementTracker.onDailyExpenseCheck(dailyExpense)

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
}