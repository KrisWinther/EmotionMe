package com.emotionme.stable

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDAO {

    @Insert
    fun insert(user: User): Long

    @Query("SELECT * FROM User WHERE login = :login LIMIT 1")
    fun getByLogin(login: String): User?

    @Query("""
        SELECT * FROM User 
        WHERE login = :login AND password = :password 
        LIMIT 1
    """)
    fun login(login: String, password: String): User?

    @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
    fun getById(id: Long) :User?

    @Query("UPDATE User SET password = :newPassword WHERE id = :id")
    fun updatePassword(id: Long, newPassword: String)
}