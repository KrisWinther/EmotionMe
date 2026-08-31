package com.emotionme.stable

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [User::class, MoodEntry::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDAO
    abstract fun moodDao(): MoodDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 2 -> 3: поля онбординга
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE User ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE User ADD COLUMN symptoms TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE User ADD COLUMN goal TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE User ADD COLUMN entryFrequency TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE User ADD COLUMN onboardingDone INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 3 -> 4: соль для хэширования паролей
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE User ADD COLUMN salt TEXT NOT NULL DEFAULT ''")
            }
        }

        /** 4 -> 5: стабильные ключи для записей настроения.
         *
         * Оптимизировано: вместо трёх отдельных полных проходов по таблице
         * (UPDATE ... WHERE mood LIKE, потом location, потом weather —
         * каждый из них раньше делал full table scan) теперь один проход —
         * все три CASE-выражения выполняются в одном UPDATE.
         *
         * Это и есть основная причина замедления запуска после обновления:
         * на устройствах с большим количеством записей (тысячи строк)
         * три полных сканирования таблицы внутри миграции, выполняемой
         * синхронно при первом открытии БД, давали заметную задержку.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. Добавляем колонки
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN moodKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN locationKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN weatherKey TEXT NOT NULL DEFAULT ''")

                // 2. Один проход по таблице — заполняем все три колонки сразу
                db.execSQL("""
                    UPDATE mood_entries SET
                        moodKey = CASE
                            WHEN mood LIKE '%🥰%' THEN 'mood_happy'
                            WHEN mood LIKE '%😊%' THEN 'mood_joy'
                            WHEN mood LIKE '%😇%' THEN 'mood_calm'
                            WHEN mood LIKE '%😐%' THEN 'mood_neutral'
                            WHEN mood LIKE '%😴%' THEN 'mood_tired'
                            WHEN mood LIKE '%😢%' THEN 'mood_sad'
                            WHEN mood LIKE '%😡%' THEN 'mood_angry'
                            ELSE 'mood_neutral'
                        END,
                        locationKey = CASE
                            WHEN location LIKE '%🏡%' THEN 'place_home'
                            WHEN location LIKE '%📚%' OR location LIKE '%💼%' THEN 'place_work'
                            WHEN location LIKE '%⛺%' THEN 'place_walk'
                            WHEN location LIKE '%🚶%' OR location LIKE '%🚙%' THEN 'place_on_the_way'
                            WHEN location LIKE '%🥂%' THEN 'place_guest'
                            WHEN location LIKE '%🍝%' OR location LIKE '%☕%' THEN 'place_cafe'
                            WHEN location LIKE '%💳%' THEN 'place_shopping'
                            WHEN location LIKE '%🗺%' THEN 'place_travel'
                            WHEN location LIKE '%🎉%' THEN 'place_party'
                            ELSE 'place_home'
                        END,
                        weatherKey = CASE
                            WHEN weather LIKE '%☀%'  THEN 'weather_clear'
                            WHEN weather LIKE '%⛅%'  THEN 'weather_cloudy1'
                            WHEN weather LIKE '%☁%'  THEN 'weather_cloudy2'
                            WHEN weather LIKE '%⛈%'  THEN 'weather_bad'
                            ELSE 'weather_clear'
                        END
                """.trimIndent())

                // 3. Индекс по userId — ускоряет все последующие запросы
                //    статистики (getMoodStatsRange и т.д.), которые
                //    фильтруют по userId + timestamp.
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mood_entries_userId ON mood_entries(userId)"
                )
            }
        }

        /** 5 -> 6: поля результатов офлайн-анализа текста заметки (TextAnalyzer).
         *
         * Новые записи получают значения сразу при сохранении (MainActivity).
         * Старые записи получают дефолты (0.0 / '' / 'category_other') —
         * при желании можно догнать их фоновым пересчётом через TextAnalyzer.analyze(note),
         * но это не обязательно для MVP.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN sentimentScore REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN keywords TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN eventCategory TEXT NOT NULL DEFAULT 'category_other'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "emotionme_db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}