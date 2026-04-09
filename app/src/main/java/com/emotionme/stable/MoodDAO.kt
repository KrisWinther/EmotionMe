package com.emotionme.stable

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MoodDAO {

    // Mood

    @Query("""
        SELECT mood AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from
        GROUP BY mood
    """)
    fun getMoodStatsFrom(uid: Long, from: Long): List<StatItem>

    // Location

    @Query("""
        SELECT location AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from
        GROUP BY location
    """)
    fun getLocationStatsFrom(uid: Long, from: Long): List<StatItem>

    // Weather

    @Query("""
        SELECT weather AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from
        GROUP BY weather
    """)
    fun getWeatherStatsFrom(uid: Long, from: Long): List<StatItem>

    @Query("""
        SELECT mood AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        GROUP BY mood
    """)
    fun getMoodStatsRange(uid: Long, from: Long, to: Long): List<StatItem>

    @Query("""
        SELECT location AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        GROUP BY location
    """)
    fun getLocationStatsRange(uid: Long, from: Long, to: Long): List<StatItem>

    @Query("""
        SELECT weather AS label, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        GROUP BY weather
    """)
    fun getWeatherStatsRange(uid: Long, from: Long, to: Long): List<StatItem>

    // Доминирующая эмоция по дню, группировка

    @Query("""
        SELECT * FROM mood_entries
        WHERE userId = :uid AND timestamp >= :from AND timestamp < :to
        ORDER BY timestamp ASC
    """)
    fun getEntriesRange(uid: Long, from: Long, to: Long): List<MoodEntry>

    @Insert
    fun insert(entry: MoodEntry)

    @Query("""
        SELECT mood, COUNT(*) AS count
        FROM mood_entries
        WHERE userId = :uid
        AND timestamp >= :from
        GROUP BY mood
    """)
    fun getStatsFrom(uid: Long, from: Long): List<MoodStat>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAll(userId: Long): List<MoodEntry>

    @Query("UPDATE mood_entries SET note = '' WHERE id = :entryId")
    fun clearNote(entryId: Int)
}
