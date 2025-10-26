package com.example.financegame.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.Expense
import com.example.financegame.data.local.database.entities.ExpenseType
import com.example.financegame.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class CategoryExpense(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class PeriodReport(
    val totalExpenses: Double,
    val totalIncome: Double,
    val balance: Double,
    val categoryBreakdown: List<CategoryExpense>,
    val dailyAverage: Double
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseRepository = ExpenseRepository(database.expenseDao())

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod

    init {
        // ✅ Квест: "📊 Переглянь статистику" - виконується при відкритті екрану
        viewModelScope.launch {
            checkAndCompleteQuest("📊 Переглянь статистику")
        }
    }

    val periodReport: StateFlow<PeriodReport?> = _selectedPeriod.flatMapLatest { period ->
        flow {
            val (startDate, endDate) = getPeriodDates(period)

            expenseRepository.getExpensesByDateRange(1, startDate, endDate).collect { expenses ->
                val totalExpenses = expenses.filter { it.type == ExpenseType.EXPENSE }
                    .sumOf { it.amount }

                val totalIncome = expenses.filter { it.type == ExpenseType.INCOME }
                    .sumOf { it.amount }

                val balance = totalIncome - totalExpenses

                val categoryMap = expenses
                    .filter { it.type == ExpenseType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }

                val categoryBreakdown = categoryMap.map { (category, amount) ->
                    CategoryExpense(
                        category = category,
                        amount = amount,
                        percentage = if (totalExpenses > 0) {
                            ((amount / totalExpenses) * 100).toFloat()
                        } else 0f
                    )
                }.sortedByDescending { it.amount }

                val daysDiff = ((endDate - startDate) / (24 * 60 * 60 * 1000)).toInt() + 1
                val dailyAverage = if (daysDiff > 0) totalExpenses / daysDiff else 0.0

                emit(
                    PeriodReport(
                        totalExpenses = totalExpenses,
                        totalIncome = totalIncome,
                        balance = balance,
                        categoryBreakdown = categoryBreakdown,
                        dailyAverage = dailyAverage
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val expensesForPeriod: StateFlow<List<Expense>> = _selectedPeriod.flatMapLatest { period ->
        val (startDate, endDate) = getPeriodDates(period)
        expenseRepository.getExpensesByDateRange(1, startDate, endDate)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    private fun getPeriodDates(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        return when (period) {
            ReportPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, endDate)
            }
            ReportPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, endDate)
            }
            ReportPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, endDate)
            }
            ReportPeriod.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, endDate)
            }
        }
    }

    // ✅ Функція перевірки та виконання квестів
    private suspend fun checkAndCompleteQuest(questTitle: String) {
        val quests = database.questDao().getActiveQuests().first()
        val quest = quests.find { it.title == questTitle }

        quest?.let {
            if (!it.isCompleted) {
                database.questDao().updateQuestProgress(it.id, 1f)
                database.questDao().completeQuest(it.id, System.currentTimeMillis())

                val user = database.userDao().getCurrentUser().first()
                user?.let { currentUser ->
                    val newExp = currentUser.experience + it.reward
                    val newLevel = (kotlin.math.sqrt(newExp.toDouble() / 100.0)).toInt() + 1
                    val newTotalPoints = currentUser.totalPoints + it.reward

                    database.userDao().updateUser(
                        currentUser.copy(
                            experience = newExp,
                            level = newLevel,
                            totalPoints = newTotalPoints
                        )
                    )
                }
            }
        }
    }
}

enum class ReportPeriod(val displayName: String) {
    TODAY("Сьогодні"),
    THIS_WEEK("Цей тиждень"),
    THIS_MONTH("Цей місяць"),
    THIS_YEAR("Цей рік")
}