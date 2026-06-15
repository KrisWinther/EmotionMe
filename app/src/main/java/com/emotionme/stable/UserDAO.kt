package com.emotionme.stable

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDAO {

    @Insert
    fun insert(user: User): Long

    @Query("SELECT * FROM User WHERE login = :login LIMIT 1")
    fun getByLogin(login: String): User?

    /**
     * Возвращает пользователя только по логину.
     * Проверка пароля — через PasswordHasher.verify() в коде,
     * а не в SQL, чтобы соль участвовала в сравнении.
     */
    @Query("SELECT * FROM User WHERE login = :login LIMIT 1")
    fun login(login: String): User?

    @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
    fun getById(id: Long): User?

    /** Обновляет пароль вместе с новой солью */
    @Query("UPDATE User SET password = :newHash, salt = :newSalt WHERE id = :id")
    fun updatePassword(id: Long, newHash: String, newSalt: String)

    @Query("""
        UPDATE User SET
            displayName = :displayName,
            symptoms = :symptoms,
            goal = :goal,
            entryFrequency = :entryFrequency,
            onboardingDone = 1
        WHERE id = :id
    """)
    fun updateOnboarding(
        id: Long,
        displayName: String,
        symptoms: String,
        goal: String,
        entryFrequency: String
    )

    @Query("UPDATE User SET onboardingDone = 0 WHERE id = :id")
    fun resetOnboarding(id: Long)
}
