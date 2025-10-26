package com.example.financegame.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financegame.data.local.database.AppDatabase
import com.example.financegame.data.settings.AppTheme
import com.example.financegame.data.settings.SettingsDataStore
import com.example.financegame.data.settings.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)
    private val database = AppDatabase.getDatabase(application)

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
            initialValue = false
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
            initialValue = false
        )

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.saveTheme(theme)

            // ✅ Квест: "⚙️ Налаштуй тему"
            checkAndCompleteQuest("⚙️ Налаштуй тему")
            checkAndCompleteQuest("🎨 Спробуй темну тему")
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsDataStore.saveAppTheme(theme)

            // ✅ Квест: "⚙️ Налаштуй тему"
            checkAndCompleteQuest("⚙️ Налаштуй тему")
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

            // ✅ Квест: "💰 Вибери валюту"
            checkAndCompleteQuest("💰 Вибери валюту")
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNotificationsEnabled(enabled)

            // ✅ Квест: "🔔 Увімкни сповіщення"
            if (enabled) {
                checkAndCompleteQuest("🔔 Увімкни сповіщення")
            }
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

    // ✅ Функція перевірки та виконання квестів
    private suspend fun checkAndCompleteQuest(questTitle: String) {
        val quests = database.questDao().getActiveQuests().first()
        val quest = quests.find { it.title == questTitle }

        quest?.let {
            if (!it.isCompleted) {
                // Оновлюємо прогрес до 100%
                database.questDao().updateQuestProgress(it.id, 1f)
                // Виконуємо квест
                database.questDao().completeQuest(it.id, System.currentTimeMillis())

                // Даємо досвід користувачу
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