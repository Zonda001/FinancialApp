package com.example.financegame.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.financegame.data.local.database.dao.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ======================== ROOM DATABASE ========================
@Database(
    entities = [User::class, Expense::class, Quest::class, Achievement::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun questDao(): QuestDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_game_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // Callback для ініціалізації початкових даних
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(database: AppDatabase) {
            val userDao = database.userDao()
            val questDao = database.questDao()
            val achievementDao = database.achievementDao()

            // Створюємо користувача за замовчуванням
            userDao.insertUser(
                User(
                    id = 1,
                    name = "Гравець",
                    email = "",
                    level = 1,
                    experience = 0,
                    totalPoints = 0
                )
            )

            // Додаємо стартові квести
            val startQuests = listOf(
                // Існуючі квести
                Quest(
                    title = "Перший крок",
                    description = "Додай свою першу витрату",
                    reward = 100,  // Збільшено з 50 до 100
                    questType = QuestType.SAVE_MONEY
                ),
                Quest(
                    title = "Економний тиждень",
                    description = "Витрачай менше 500 грн на тиждень",
                    targetAmount = 500.0,
                    targetDays = 7,
                    reward = 150,
                    questType = QuestType.WEEKLY_GOAL
                ),
                Quest(
                    title = "Без фастфуду",
                    description = "7 днів без витрат на фастфуд",
                    category = "Їжа",
                    targetDays = 7,
                    reward = 200,
                    questType = QuestType.NO_SPENDING
                ),

                // НОВІ КВЕСТИ "В ОДИН КЛІК"
                Quest(
                    title = "🎯 Швидкий старт",
                    description = "Натисни щоб отримати перші 25 балів!",
                    reward = 50,  // Збільшено з 25 до 50
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "📊 Переглянь статистику",
                    description = "Відкрий розділ звітів (просто натисни кнопку)",
                    reward = 50,  // Збільшено з 30 до 50
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "⚙️ Налаштуй тему",
                    description = "Зайди в налаштування і вибери кольорову тему",
                    reward = 50,  // Збільшено з 40 до 50
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "🏆 Переглянь досягнення",
                    description = "Відкрий розділ досягнень",
                    reward = 50,  // Збільшено з 35 до 50
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "💪 Щоденна мотивація",
                    description = "Отримай бонус просто за відвідування додатку",
                    reward = 50,  // Збільшено з 20 до 50
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                )
            )
            startQuests.forEach { questDao.insertQuest(it) }

            // Додаємо досягнення
            val startAchievements = listOf(
                Achievement(
                    title = "Новачок",
                    description = "Додай першу витрату",
                    icon = "🎯",
                    requirement = 1,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Економний",
                    description = "Заощадь 1000 грн",
                    icon = "💰",
                    requirement = 1000,
                    category = AchievementCategory.SAVINGS
                ),
                Achievement(
                    title = "Герой квестів",
                    description = "Виконай 5 квестів",
                    icon = "🏆",
                    requirement = 5,
                    category = AchievementCategory.QUESTS
                ),
                Achievement(
                    title = "Тижнева серія",
                    description = "Відстежуй витрати 7 днів поспіль",
                    icon = "🔥",
                    requirement = 7,
                    category = AchievementCategory.STREAK
                ),
                Achievement(
                    title = "Майстер фінансів",
                    description = "Досягни 10 рівня",
                    icon = "👑",
                    requirement = 10,
                    category = AchievementCategory.GENERAL
                )
            )
            startAchievements.forEach { achievementDao.insertAchievement(it) }
        }
    }
}