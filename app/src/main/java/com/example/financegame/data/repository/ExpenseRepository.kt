package com.example.financegame.data.repository

import com.example.financegame.data.local.database.dao.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    fun getAllExpenses(userId: Int): Flow<List<Expense>> = expenseDao.getAllExpenses(userId)

    fun getExpensesByDateRange(userId: Int, startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(userId, startDate, endDate)

    fun getExpensesByCategory(userId: Int, category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(userId, category)

    fun getTotalExpenses(userId: Int, startDate: Long, endDate: Long): Flow<Double?> =
        expenseDao.getTotalExpenses(userId, startDate, endDate)

    fun getTotalIncome(userId: Int, startDate: Long, endDate: Long): Flow<Double?> =
        expenseDao.getTotalIncome(userId, startDate, endDate)

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
}