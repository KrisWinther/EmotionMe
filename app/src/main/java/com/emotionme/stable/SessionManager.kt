package com.emotionme.stable

import android.content.Context
import androidx.core.content.edit

object SessionManager {

    private const val PREFS = "session"
    private const val KEY_USER = "user_id"

    fun saveUser(context: Context, userId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putLong(KEY_USER, userId)
            }
    }

    fun getUser(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_USER, 0L)
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_USER)
            }
    }
}