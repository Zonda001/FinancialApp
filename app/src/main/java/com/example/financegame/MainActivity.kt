package com.example.financegame

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.local.database.entities.User
import com.example.financegame.data.settings.ThemeMode
import com.example.financegame.ui.navigation.MainScreenWithLaunchers
import com.example.financegame.ui.screens.auth.LoginScreen
import com.example.financegame.ui.screens.auth.RegistrationScreen
import com.example.financegame.ui.screens.expenses.ExpenseViewModel
import com.example.financegame.ui.screens.settings.SettingsViewModel
import com.example.financegame.ui.theme.FinanceGameTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var expenseViewModel: ExpenseViewModel

    private var isAuthenticated = mutableStateOf(false)
    private var isRegistered = mutableStateOf(false)
    private var isLoading = mutableStateOf(true)

    private val PREFS_NAME = "FinanceGamePrefs"
    private val KEY_PASSWORD = "user_password"
    private val KEY_REGISTERED = "user_registered"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(this)
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        lifecycleScope.launch {
            checkStreakReset()
            checkAndResetDailyQuests()

            val wasRegistered = prefs.getBoolean(KEY_REGISTERED, false)
            val existingUser = database.userDao().getCurrentUser().first()

            if (wasRegistered && existingUser != null) {
                isRegistered.value = true
                isAuthenticated.value = false
            } else {
                isRegistered.value = false
                isAuthenticated.value = false
            }

            isLoading.value = false
        }

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val appTheme by settingsViewModel.appTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            val themeName = appTheme.name.lowercase()

            FinanceGameTheme(themeName = themeName, darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        isLoading.value -> {
                            // Splash Screen
                        }

                        !isRegistered.value -> {
                            RegistrationScreen(
                                onRegistrationComplete = { nickname, avatar, password, useBiometric ->
                                    lifecycleScope.launch {
                                        val newUser = User(
                                            id = 1,
                                            name = nickname,
                                            avatarUrl = avatar,
                                            email = "",
                                            level = 1,
                                            experience = 0,
                                            totalPoints = 0
                                        )

                                        val existingUser = database.userDao().getCurrentUser().first()
                                        if (existingUser != null) {
                                            database.userDao().updateUser(newUser)
                                        } else {
                                            database.userDao().insertUser(newUser)
                                        }

                                        prefs.edit().apply {
                                            putString(KEY_PASSWORD, password)
                                            putBoolean(KEY_REGISTERED, true)
                                            apply()
                                        }

                                        settingsViewModel.setBiometricEnabled(false)

                                        isRegistered.value = true
                                        isAuthenticated.value = true
                                    }
                                },
                                onGuestMode = {
                                    lifecycleScope.launch {
                                        val guestUser = User(
                                            id = 1,
                                            name = "Гість",
                                            avatarUrl = "👤",
                                            email = "",
                                            level = 1,
                                            experience = 0,
                                            totalPoints = 0
                                        )

                                        database.userDao().insertUser(guestUser)

                                        prefs.edit().apply {
                                            putBoolean(KEY_REGISTERED, true)
                                            apply()
                                        }

                                        isRegistered.value = true
                                        isAuthenticated.value = true
                                    }
                                },
                                biometricAvailable = false
                            )
                        }

                        !isAuthenticated.value -> {
                            val savedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""

                            LoginScreen(
                                savedPassword = savedPassword,
                                biometricAvailable = false,
                                onPasswordSuccess = {
                                    isAuthenticated.value = true
                                },
                                onBiometricClick = { }
                            )
                        }

                        else -> {
                            MainScreenWithLaunchers(
                                settingsViewModel = settingsViewModel,
                                expenseViewModel = expenseViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkAndResetDailyQuests() {
        val questPrefs = getSharedPreferences("QuestPrefs", Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val allQuests = database.questDao().getAllQuests().first()

        allQuests.forEach { quest ->
            if (isDailyQuest(quest)) {
                val lastCompletedDate = questPrefs.getString("daily_quest_${quest.id}", "")
                if (lastCompletedDate != today && quest.isCompleted) {
                    database.questDao().resetQuest(quest.id)
                }
            }
        }
    }

    private suspend fun checkStreakReset() {
        val streakPrefs = getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE)
        val lastStreakDate = streakPrefs.getString("last_streak_date", "") ?: ""
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        if (lastStreakDate != today && lastStreakDate != yesterday && lastStreakDate.isNotEmpty()) {
            streakPrefs.edit().apply {
                putInt("current_streak", 0)
                apply()
            }
        }
    }

    private fun isDailyQuest(quest: com.example.financegame.data.local.database.entities.Quest): Boolean {
        return quest.title.contains("💪 Щоденна мотивація")
    }

    private fun getTodayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    private fun getYesterdayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }
}