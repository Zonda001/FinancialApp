package com.example.financegame

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
import com.example.financegame.ui.screens.auth.BiometricAuthScreen
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
    private var needsBiometric = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        biometricAuthManager = BiometricAuthManager(this)
        database = AppDatabase.getDatabase(this)
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            val user = database.userDao().getCurrentUser().first()

            if (user != null) {
                isRegistered.value = true

                val biometricEnabled = settingsViewModel.biometricEnabled.first()

                if (biometricEnabled && biometricAuthManager.isBiometricAvailable()) {
                    needsBiometric.value = true
                    isAuthenticated.value = false
                } else {
                    isAuthenticated.value = true
                }
            } else {
                isRegistered.value = false
                isAuthenticated.value = false
            }
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
                        !isRegistered.value -> {
                            RegistrationScreen(
                                onRegistrationComplete = { nickname, avatar, password, useBiometric ->
                                    lifecycleScope.launch {
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

                                        // Зберігаємо налаштування біометрії
                                        settingsViewModel.setBiometricEnabled(useBiometric)

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
                                        isRegistered.value = true
                                        isAuthenticated.value = true
                                    }
                                },
                                biometricAvailable = biometricAuthManager.isBiometricAvailable()
                            )
                        }

                        needsBiometric.value && !isAuthenticated.value -> {
                            BiometricAuthScreen(
                                onAuthenticate = {
                                    biometricAuthManager.authenticate(
                                        activity = this@MainActivity,
                                        onSuccess = {
                                            isAuthenticated.value = true
                                        },
                                        onError = { _ ->
                                            // Тут можна додати Toast або лог
                                        },
                                        onFailed = {
                                            // Автентифікація не вдалася
                                        }
                                    )
                                }
                            )
                        }

                        else -> {
                            MainScreen(settingsViewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}
