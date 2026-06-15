package com.emotionme.stable

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import java.util.Locale

object LanguageManager {

    private const val PREFS_NAME = "settings"
    private const val KEY_LANGUAGE = "app_language"

    const val LANG_RUSSIAN = "ru"
    const val LANG_ENGLISH = "en"

    val allLanguages = listOf(LANG_RUSSIAN, LANG_ENGLISH)

    fun displayName(lang: String): String = when (lang) {
        LANG_RUSSIAN -> "🇷🇺 Русский"
        LANG_ENGLISH -> "🇬🇧 English"
        else -> "🇷🇺 Русский"
    }

    fun saveLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_LANGUAGE, lang) }
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_RUSSIAN) ?: LANG_RUSSIAN
    }

    fun applyLanguage(activity: AppCompatActivity) {
        val lang = getSavedLanguage(activity)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
    }

    fun applyLanguage(context: Context): Context {
        val lang = getSavedLanguage(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
