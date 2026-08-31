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
    val weatherKey: String = "",

    // Результаты офлайн-анализа текста заметки (добавлены в версии 6).
    // Заполняются через TextAnalyzer.analyze(note) при сохранении записи.
    // sentimentScore: -1.0 (очень негативно) .. +1.0 (очень позитивно)
    val sentimentScore: Float = 0f,
    // Ключевые слова через запятую, например "домашка,друзья,гулял"
    val keywords: String = "",
    // Одна из TextAnalyzer.CATEGORY_*
    val eventCategory: String = TextAnalyzer.CATEGORY_OTHER
)