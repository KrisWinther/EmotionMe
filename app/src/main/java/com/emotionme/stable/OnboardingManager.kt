package com.emotionme.stable

import android.content.Context

object OnboardingManager {

    private const val SEP = "|||"

    // Ключи строковых ресурсов для симптомов
    val symptomResKeys = listOf(
        R.string.symptom_fatigue,
        R.string.symptom_anxiety,
        R.string.symptom_irritability,
        R.string.symptom_emptiness,
        R.string.symptom_depression,
        R.string.symptom_thoughts,
        R.string.symptom_sleep,
        R.string.symptom_good
    )

    val goalResKeys = listOf(
        R.string.goal_stress,
        R.string.goal_track,
        R.string.goal_sleep,
        R.string.goal_emotions,
        R.string.goal_productivity,
        R.string.goal_happy
    )

    val frequencyResKeys = listOf(
        R.string.freq_daily,
        R.string.freq_several,
        R.string.freq_weekly,
        R.string.freq_needed
    )

    // Локализованные списки (зависят от контекста)
    fun symptomOptions(context: Context) = symptomResKeys.map { context.getString(it) }
    fun goalOptions(context: Context) = goalResKeys.map { context.getString(it) }
    fun frequencyOptions(context: Context) = frequencyResKeys.map { context.getString(it) }

    // Работа с User из Room
    fun isCompleted(user: User): Boolean = user.onboardingDone == 1

    fun symptomsToList(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split(SEP)

    fun symptomsToString(list: List<String>): String = list.joinToString(SEP)
}
