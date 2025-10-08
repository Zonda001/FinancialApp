package com.example.financegame.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// ======================== КОРИСТУВАЧ ========================
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String = "",
    val avatarUrl: String = "",
    val level: Int = 1,
    val experience: Int = 0,
    val totalPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ======================== ВИТРАТИ/ДОХОДИ ========================
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 1, // За замовчуванням користувач з ID 1
    val amount: Double,
    val category: String,
    val type: ExpenseType = ExpenseType.EXPENSE, // EXPENSE або INCOME
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val receiptPhotoPath: String? = null
)

enum class ExpenseType {
    EXPENSE,  // Витрати
    INCOME    // Доходи
}

// ======================== КВЕСТИ ========================
@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val targetAmount: Double = 0.0,      // Для квестів типу "витрать менше X"
    val targetDays: Int = 0,              // Кількість днів для виконання
    val category: String = "загальне",    // Категорія витрат
    val reward: Int = 100,                // Бали за виконання
    val isCompleted: Boolean = false,
    val progress: Float = 0f,             // Прогрес 0.0 - 1.0
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val questType: QuestType = QuestType.SAVE_MONEY
)

enum class QuestType {
    SAVE_MONEY,        // Заощадити гроші
    NO_SPENDING,       // Не витрачати в категорії
    DAILY_LIMIT,       // Дотримуватись денного ліміту
    WEEKLY_GOAL        // Тижнева ціль
}

// ======================== ДОСЯГНЕННЯ ========================
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val icon: String = "🏆",              // Emoji як іконка
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val requirement: Int = 0,             // Вимога для відкриття
    val currentProgress: Int = 0,
    val category: AchievementCategory = AchievementCategory.GENERAL
)

enum class AchievementCategory {
    GENERAL,           // Загальні
    SAVINGS,           // Заощадження
    STREAK,            // Серії
    QUESTS             // Квести
}

// ======================== КАТЕГОРІЇ ВИТРАТ ========================
data class ExpenseCategory(
    val name: String,
    val icon: String,
    val color: String
)

// Попередньо визначені категорії
object DefaultCategories {
    val categories = listOf(
        ExpenseCategory("Їжа", "🍕", "#FF6B6B"),
        ExpenseCategory("Транспорт", "🚗", "#4ECDC4"),
        ExpenseCategory("Розваги", "🎮", "#FFE66D"),
        ExpenseCategory("Здоров'я", "💊", "#95E1D3"),
        ExpenseCategory("Комунальні", "🏠", "#A8E6CF"),
        ExpenseCategory("Одяг", "👕", "#FF8B94"),
        ExpenseCategory("Освіта", "📚", "#C7CEEA"),
        ExpenseCategory("Інше", "💰", "#B4B4B4")
    )
}