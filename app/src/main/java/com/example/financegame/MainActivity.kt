package com.example.financegame

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
import com.example.financegame.data.settings.ThemeMode
import com.example.financegame.ui.navigation.MainScreen
import com.example.financegame.ui.screens.settings.SettingsViewModel
import com.example.financegame.ui.theme.FinanceGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Створюємо ViewModel на рівні Activity
        val settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            // Спостерігаємо за змінами налаштувань
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val appTheme by settingsViewModel.appTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()

            // Визначаємо чи використовувати темну тему
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            // Отримуємо назву теми для передачі в FinanceGameTheme
            val themeName = appTheme.name.lowercase()

            // Застосовуємо тему
            FinanceGameTheme(themeName = themeName, darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}