package com.emotionme.stable

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "mood_entries")

data class MoodEntry(

    @PrimaryKey(autoGenerate = true)

    val id: Int = 0,
    val userId: Long,
    val mood: String,
    val location: String,
    val weather: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)