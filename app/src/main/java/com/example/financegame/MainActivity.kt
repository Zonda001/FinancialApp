package com.example.financegame

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
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
import com.example.financegame.ui.navigation.MainScreen
import com.example.financegame.ui.screens.auth.LoginScreen
import com.example.financegame.ui.screens.auth.RegistrationScreen
import com.example.financegame.ui.screens.settings.SettingsViewModel
import com.example.financegame.ui.theme.FinanceGameTheme
import com.example.financegame.util.BiometricAuthManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var database: AppDatabase
    private lateinit var settingsViewModel: SettingsViewModel

    private var isAuthenticated = mutableStateOf(false)
    private var isRegistered = mutableStateOf(false)
    private var isLoading = mutableStateOf(true)

    // SharedPreferences для збереження паролю
    private val PREFS_NAME = "FinanceGamePrefs"
    private val KEY_PASSWORD = "user_password"
    private val KEY_REGISTERED = "user_registered"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        biometricAuthManager = BiometricAuthManager(this)
        database = AppDatabase.getDatabase(this)
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        lifecycleScope.launch {
            // Перевіряємо чи користувач зареєстрований
            val wasRegistered = prefs.getBoolean(KEY_REGISTERED, false)

            if (wasRegistered) {
                // Користувач вже реєструвався - показуємо екран входу
                isRegistered.value = true
                isAuthenticated.value = false
            } else {
                // Перший запуск - показуємо реєстрацію
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
                            // Можна додати Splash Screen
                        }

                        !isRegistered.value -> {
                            // ПЕРШИЙ РАЗ - Екран реєстрації
                            RegistrationScreen(
                                onRegistrationComplete = { nickname, avatar, password, useBiometric ->
                                    lifecycleScope.launch {
                                        // Зберігаємо користувача в БД
                                        database.userDao().insertUser(
                                            User(
                                                id = 1,
                                                name = nickname,
                                                avatarUrl = avatar,
                                                email = "",
                                                level = 1,
                                                experience = 0,
                                                totalPoints = 0
                                            )
                                        )

                                        // Зберігаємо пароль та статус реєстрації
                                        prefs.edit().apply {
                                            putString(KEY_PASSWORD, password)
                                            putBoolean(KEY_REGISTERED, true)
                                            apply()
                                        }

                                        // Налаштування біометрії
                                        settingsViewModel.setBiometricEnabled(useBiometric)

                                        // Входимо в додаток
                                        isRegistered.value = true
                                        isAuthenticated.value = true
                                    }
                                },
                                onGuestMode = {
                                    lifecycleScope.launch {
                                        database.userDao().insertUser(
                                            User(
                                                id = 1,
                                                name = "Гість",
                                                avatarUrl = "👤",
                                                email = "",
                                                level = 1,
                                                experience = 0,
                                                totalPoints = 0
                                            )
                                        )

                                        prefs.edit().apply {
                                            putBoolean(KEY_REGISTERED, true)
                                            apply()
                                        }

                                        isRegistered.value = true
                                        isAuthenticated.value = true
                                    }
                                },
                                biometricAvailable = biometricAuthManager.isBiometricAvailable()
                            )
                        }

                        !isAuthenticated.value -> {
                            // НАСТУПНІ РАЗИ - Екран входу
                            val savedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
                            val biometricEnabled = settingsViewModel.biometricEnabled.collectAsState().value

                            LoginScreen(
                                savedPassword = savedPassword,
                                biometricAvailable = biometricAuthManager.isBiometricAvailable() && biometricEnabled,
                                onPasswordSuccess = {
                                    isAuthenticated.value = true
                                },
                                onBiometricClick = {
                                    biometricAuthManager.authenticate(
                                        activity = this@MainActivity,
                                        onSuccess = {
                                            isAuthenticated.value = true
                                        },
                                        onError = { errorMessage ->
                                            // Можна показати Toast
                                        },
                                        onFailed = {
                                            // Автентифікація не вдалася
                                        }
                                    )
                                }
                            )
                        }

                        else -> {
                            // Головний екран додатку
                            MainScreen(settingsViewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}