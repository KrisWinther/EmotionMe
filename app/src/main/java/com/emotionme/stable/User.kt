package com.emotionme.stable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val login: String,

    // PBKDF2-хэш пароля (Base64)
    val password: String,

    // Криптографическая соль (Base64, 32 байта)
    @ColumnInfo(defaultValue = "")
    val salt: String = "",

    // Онбординг
    @ColumnInfo(defaultValue = "")
    val displayName: String = "",

    @ColumnInfo(defaultValue = "")
    val symptoms: String = "",

    @ColumnInfo(defaultValue = "")
    val goal: String = "",

    @ColumnInfo(defaultValue = "")
    val entryFrequency: String = "",

    @ColumnInfo(defaultValue = "0")
    val onboardingDone: Int = 0
)
