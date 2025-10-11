package com.example.financegame.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension для створення DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_KEY = booleanPreferencesKey("biometric_enabled")
        val BUDGET_ALERTS_KEY = booleanPreferencesKey("budget_alerts")
    }

    // Збереження теми
    suspend fun saveTheme(theme: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    // Отримання теми
    val themeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(themeName)
        }

    // Збереження мови
    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    val languageFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: "uk" // Українська за замовчуванням
        }

    // Збереження валюти
    suspend fun saveCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }

    val currencyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENCY_KEY] ?: "грн"
        }

    // Сповіщення
    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    val notificationsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }

    // Біометрія
    suspend fun saveBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_KEY] = enabled
        }
    }

    val biometricFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BIOMETRIC_KEY] ?: false
        }

    // Попередження про бюджет
    suspend fun saveBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BUDGET_ALERTS_KEY] = enabled
        }
    }

    val budgetAlertsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BUDGET_ALERTS_KEY] ?: true
        }
}

// Enum для режимів теми
enum class ThemeMode {
    LIGHT,      // Світла
    DARK,       // Темна
    SYSTEM      // Системна
}