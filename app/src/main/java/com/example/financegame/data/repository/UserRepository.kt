package com.example.financegame.data.repository

import com.example.financegame.data.local.database.dao.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    fun getCurrentUser(): Flow<User?> = userDao.getCurrentUser()

    fun getUserById(userId: Int): Flow<User?> = userDao.getUserById(userId)

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun addExperience(userId: Int, points: Int) {
        userDao.addExperience(userId, points)
    }

    suspend fun updateLevel(userId: Int, level: Int) {
        userDao.updateLevel(userId, level)
    }
}