package com.example.financegame.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.settings.AppTheme
import com.example.financegame.data.settings.SettingsDataStore
import com.example.financegame.data.settings.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val appTheme: StateFlow<AppTheme> = settingsDataStore.appThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.OCEAN
        )

    val language: StateFlow<String> = settingsDataStore.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "uk"
        )

    val currency: StateFlow<String> = settingsDataStore.currencyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "грн"
        )

    val notificationsEnabled: StateFlow<Boolean> = settingsDataStore.notificationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val biometricEnabled: StateFlow<Boolean> = settingsDataStore.biometricFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val budgetAlertsEnabled: StateFlow<Boolean> = settingsDataStore.budgetAlertsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.saveTheme(theme)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsDataStore.saveAppTheme(theme)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.saveLanguage(language)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            settingsDataStore.saveCurrency(currency)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNotificationsEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveBiometricEnabled(enabled)
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveBudgetAlertsEnabled(enabled)
        }
    }
}