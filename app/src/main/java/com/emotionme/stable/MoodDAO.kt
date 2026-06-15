package com.emotionme.stable

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MoodDAO {

    // Вставка
    @Insert
    fun insert(entry: MoodEntry)

    @Query("UPDATE mood_entries SET note = '' WHERE id = :entryId")
    fun clearNote(entryId: Int)

    // Статистика по KEY-полям (основные — для графиков)

    @Query("""
        SELECT moodKey AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        GROUP BY moodKey
    """)
    fun getMoodStatsRange(uid: Long, from: Long, to: Long): List<StatItem>

    @Query("""
        SELECT locationKey AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        GROUP BY locationKey
    """)
    fun getLocationStatsRange(uid: Long, from: Long, to: Long): List<StatItem>

    @Query("""
        SELECT weatherKey AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        GROUP BY weatherKey
    """)
    fun getWeatherStatsRange(uid: Long, from: Long, to: Long): List<StatItem>

    // Все записи диапазона (для календаря доминирующего настроения)

    @Query("""
        SELECT * FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        ORDER BY timestamp ASC
    """)
    fun getEntriesRange(uid: Long, from: Long, to: Long): List<MoodEntry>

    // Для мотивационного блока в MainActivity

    @Query("""
        SELECT moodKey AS mood, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from
        GROUP BY moodKey
    """)
    fun getStatsFrom(uid: Long, from: Long): List<MoodStat>

    // Все записи пользователя
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAll(userId: Long): List<MoodEntry>

    // AnswersActivity

    @Query("SELECT MIN(timestamp) FROM mood_entries WHERE userId = :uid")
    fun getFirstEntryTimestamp(uid: Long): Long?

    @Query("""
        SELECT COUNT(DISTINCT (timestamp / 86400000))
        FROM mood_entries WHERE userId = :uid
    """)
    fun getActiveDaysCount(uid: Long): Int
}
