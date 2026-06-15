package com.emotionme.stable

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long,

    // Локализованная строка — хранится для отображения в NotesActivity
    val mood: String,
    val location: String,
    val weather: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),

    // Стабильные языко-независимые ключи (добавлены в версии 5).
    // Используются для группировки в статистике и отображения
    // на текущем языке через MoodKeys.*ResId().
    // Значения из MoodKeys.MOOD_*, PLACE_*, WEATHER_*.
    val moodKey: String = "",
    val locationKey: String = "",
    val weatherKey: String = ""
)