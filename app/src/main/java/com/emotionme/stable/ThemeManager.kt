package com.emotionme.stable

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

object ThemeManager {

    private const val PREFS_NAME = "settings"
    private const val KEY_THEME = "app_theme"

    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_ORANGE = "orange"
    const val THEME_RED = "red"
    const val THEME_PINK = "pink"
    const val THEME_GREEN = "green"
    const val THEME_MONO = "mono"
    const val THEME_YELLOW = "yellow"

    val allThemes = listOf(
        THEME_RED, THEME_ORANGE, THEME_YELLOW, THEME_GREEN,
        THEME_BLUE, THEME_PURPLE, THEME_PINK, THEME_MONO
    )

    /** Локализованное название темы через ресурсы */
    fun displayName(context: Context, theme: String): String = when (theme) {
        THEME_RED -> context.getString(R.string.theme_red)
        THEME_ORANGE -> context.getString(R.string.theme_orange)
        THEME_YELLOW -> context.getString(R.string.theme_yellow)
        THEME_GREEN -> context.getString(R.string.theme_green)
        THEME_BLUE -> context.getString(R.string.theme_blue)
        THEME_PURPLE -> context.getString(R.string.theme_purple)
        THEME_PINK -> context.getString(R.string.theme_pink)
        THEME_MONO -> context.getString(R.string.theme_mono)
        else -> context.getString(R.string.theme_blue)
    }

    /** Обратная совместимость без контекста (для мест где строки не нужны) */
    fun displayName(theme: String): String = when (theme) {
        THEME_RED -> "❤️ Red / Красный"
        THEME_ORANGE -> "🧡 Orange / Оранжевый"
        THEME_YELLOW -> "💛 Yellow / Жёлтый"
        THEME_GREEN -> "💚 Green / Зелёный"
        THEME_BLUE -> "💙 Blue / Синий"
        THEME_PURPLE -> "💜 Purple / Фиолетовый"
        THEME_PINK -> "🩷 Pink / Розовый"
        THEME_MONO -> "🖤 Mono / Монохром"
        else -> "💙 Blue / Синий"
    }

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_THEME, theme) }
    }

    fun getSavedTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_BLUE) ?: THEME_BLUE
    }

    fun applyTheme(activity: AppCompatActivity) {
        val styleRes = when (getSavedTheme(activity)) {
            THEME_BLUE -> R.style.Theme_EmotionMe_Blue
            THEME_PURPLE -> R.style.Theme_EmotionMe_Purple
            THEME_ORANGE -> R.style.Theme_EmotionMe_Orange
            THEME_GREEN -> R.style.Theme_EmotionMe_Green
            THEME_MONO -> R.style.Theme_EmotionMe_Mono
            THEME_RED -> R.style.Theme_EmotionMe_Red
            THEME_PINK -> R.style.Theme_EmotionMe_Pink
            THEME_YELLOW -> R.style.Theme_EmotionMe_Yellow
            else -> R.style.Theme_EmotionMe_Blue
        }
        activity.setTheme(styleRes)
    }
}
