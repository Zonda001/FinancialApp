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
import com.example.financegame.data.settings.ThemeMode
import com.example.financegame.ui.navigation.MainScreen
import com.example.financegame.ui.screens.auth.BiometricAuthScreen
import com.example.financegame.ui.screens.settings.SettingsViewModel
import com.example.financegame.ui.theme.FinanceGameTheme
import com.example.financegame.util.BiometricAuthManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var biometricAuthManager: BiometricAuthManager
    private var isAuthenticated = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        biometricAuthManager = BiometricAuthManager(this)
        val settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        // Перевіряємо чи увімкнена біометрія
        lifecycleScope.launch {
            val biometricEnabled = settingsViewModel.biometricEnabled.first()

            if (biometricEnabled && biometricAuthManager.isBiometricAvailable()) {
                // Показуємо екран автентифікації
                isAuthenticated.value = false
            } else {
                // Пропускаємо автентифікацію
                isAuthenticated.value = true
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
                    if (isAuthenticated.value) {
                        MainScreen(settingsViewModel = settingsViewModel)
                    } else {
                        BiometricAuthScreen(
                            onAuthenticate = {
                                biometricAuthManager.authenticate(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        isAuthenticated.value = true
                                    },
                                    onError = { error ->
                                        // Показуємо помилку
                                    },
                                    onFailed = {
                                        // Автентифікація не вдалась
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}