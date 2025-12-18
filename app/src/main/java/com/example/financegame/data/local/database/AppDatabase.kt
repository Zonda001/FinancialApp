package com.example.financegame.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.financegame.data.local.database.dao.*
import com.example.financegame.data.local.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Expense::class,
        Quest::class,
        Achievement::class,
        TradingPosition::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun questDao(): QuestDao
    abstract fun achievementDao(): AchievementDao
    abstract fun tradingDao(): TradingDao

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
                    .fallbackToDestructiveMigration() // ✅ ДОДАНО ЦЕЙ РЯДОК
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

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

            // ======================== КВЕСТИ "В ОДИН КЛІК" ========================
            val oneClickQuests = listOf(
                // Квести які виконуються миттєво
                Quest(
                    title = "🎯 Швидкий старт",
                    description = "Натисни кнопку щоб отримати перші бали!",
                    reward = 50,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "💪 Щоденна мотивація",
                    description = "Отримай бонус за відвідування додатку",
                    reward = 50,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "🎁 Бонус новачка",
                    description = "Отримай подарунковий досвід!",
                    reward = 100,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),

                // Квести з навігацією
                Quest(
                    title = "📊 Переглянь статистику",
                    description = "Відкрий розділ звітів та переглянь свою статистику",
                    reward = 50,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "⚙️ Налаштуй тему",
                    description = "Зайди в налаштування і вибери кольорову тему",
                    reward = 50,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "🏆 Переглянь досягнення",
                    description = "Відкрий розділ досягнень та подивись свій прогрес",
                    reward = 50,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "🌟 Зміни аватар",
                    description = "Відкрий профіль та вибери новий аватар",
                    reward = 75,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "🎨 Спробуй темну тему",
                    description = "Увімкни темну тему в налаштуваннях",
                    reward = 75,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "💰 Вибери валюту",
                    description = "Встанови свою валюту в налаштуваннях",
                    reward = 60,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                ),
                Quest(
                    title = "🔔 Увімкни сповіщення",
                    description = "Активуй сповіщення про витрати в налаштуваннях",
                    reward = 60,
                    questType = QuestType.SAVE_MONEY,
                    progress = 0f,
                    isCompleted = false
                )
            )

            // ======================== ЗВИЧАЙНІ КВЕСТИ ========================
            val regularQuests = listOf(
                Quest(
                    title = "Перший крок",
                    description = "Додай свою першу витрату",
                    reward = 100,
                    questType = QuestType.SAVE_MONEY
                ),
                Quest(
                    title = "Економний тиждень",
                    description = "Витрачай менше 200 грн протягом 7 днів поспіль",
                    targetAmount = 200.0,
                    targetDays = 7,
                    reward = 150,
                    questType = QuestType.WEEKLY_GOAL
                ),
                Quest(
                    title = "П'ять транзакцій",
                    description = "Додай 5 витрат за один день",
                    targetAmount = 5.0,
                    targetDays = 1,
                    reward = 120,
                    questType = QuestType.DAILY_LIMIT
                ),
                Quest(
                    title = "Місяць економії",
                    description = "Витрачай менше 2000 грн на місяць",
                    targetAmount = 2000.0,
                    targetDays = 30,
                    reward = 300,
                    questType = QuestType.WEEKLY_GOAL
                )
            )

            (oneClickQuests + regularQuests).forEach { questDao.insertQuest(it) }

            // ======================== ДОСЯГНЕННЯ ========================
            val achievements = listOf(
                // Загальні досягнення
                Achievement(
                    title = "Новачок",
                    description = "Додай першу витрату до журналу",
                    icon = "🎯",
                    requirement = 1,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Активний користувач",
                    description = "Додай 10 витрат",
                    icon = "📝",
                    requirement = 10,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Фінансовий гуру",
                    description = "Додай 50 витрат",
                    icon = "📊",
                    requirement = 50,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Майстер фінансів",
                    description = "Досягни 10 рівня",
                    icon = "👑",
                    requirement = 10,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Легенда",
                    description = "Досягни 20 рівня",
                    icon = "⭐",
                    requirement = 20,
                    category = AchievementCategory.GENERAL
                ),

                // Заощадження
                Achievement(
                    title = "Економний",
                    description = "Заощадь 1000 грн",
                    icon = "💰",
                    requirement = 1000,
                    category = AchievementCategory.SAVINGS
                ),
                Achievement(
                    title = "Скарбничка",
                    description = "Заощадь 5000 грн",
                    icon = "🏦",
                    requirement = 5000,
                    category = AchievementCategory.SAVINGS
                ),
                Achievement(
                    title = "Фінансова свобода",
                    description = "Заощадь 10000 грн",
                    icon = "💎",
                    requirement = 10000,
                    category = AchievementCategory.SAVINGS
                ),
                Achievement(
                    title = "Мінімаліст",
                    description = "Витрать менше 100 грн за день протягом тижня",
                    icon = "🍃",
                    requirement = 7,
                    category = AchievementCategory.SAVINGS
                ),

                // Квести
                Achievement(
                    title = "Герой квестів",
                    description = "Виконай 5 квестів",
                    icon = "🏆",
                    requirement = 5,
                    category = AchievementCategory.QUESTS
                ),
                Achievement(
                    title = "Шукач пригод",
                    description = "Виконай 15 квестів",
                    icon = "🗺️",
                    requirement = 15,
                    category = AchievementCategory.QUESTS
                ),
                Achievement(
                    title = "Легендарний герой",
                    description = "Виконай 30 квестів",
                    icon = "🎖️",
                    requirement = 30,
                    category = AchievementCategory.QUESTS
                ),

                // Серії
                Achievement(
                    title = "Тижнева серія",
                    description = "Відстежуй витрати 7 днів поспіль",
                    icon = "🔥",
                    requirement = 7,
                    category = AchievementCategory.STREAK
                ),
                Achievement(
                    title = "Місячна відданість",
                    description = "Відстежуй витрати 30 днів поспіль",
                    icon = "⚡",
                    requirement = 30,
                    category = AchievementCategory.STREAK
                ),
                Achievement(
                    title = "Незламний",
                    description = "Відстежуй витрати 100 днів поспіль",
                    icon = "💪",
                    requirement = 100,
                    category = AchievementCategory.STREAK
                ),

                // Бонусні досягнення
                Achievement(
                    title = "Перфекціоніст",
                    description = "Додай опис до 50 витрат",
                    icon = "✨",
                    requirement = 50,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Різноманітність",
                    description = "Використай всі 8 категорій витрат",
                    icon = "🎨",
                    requirement = 8,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Ранкова пташка",
                    description = "Додай витрату до 9 ранку",
                    icon = "🌅",
                    requirement = 1,
                    category = AchievementCategory.GENERAL
                ),
                Achievement(
                    title = "Нічний дозор",
                    description = "Додай витрату після 23:00",
                    icon = "🌙",
                    requirement = 1,
                    category = AchievementCategory.GENERAL
                )
            )
            achievements.forEach { achievementDao.insertAchievement(it) }
        }
    }
}