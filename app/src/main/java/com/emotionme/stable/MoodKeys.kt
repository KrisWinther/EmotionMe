package com.emotionme.stable

// Стабильные языко-независимые ключи для записей в БД
//Никогда не менять существующие значения — это primary key смысла в БД

object MoodKeys {

    // Настроения
    const val MOOD_HAPPY = "mood_happy"
    const val MOOD_JOY = "mood_joy"
    const val MOOD_CALM = "mood_calm"
    const val MOOD_NEUTRAL = "mood_neutral"
    const val MOOD_TIRED = "mood_tired"
    const val MOOD_SAD = "mood_sad"
    const val MOOD_ANGRY = "mood_angry"

    // Места
    const val PLACE_HOME = "place_home"
    const val PLACE_WORK = "place_work"
    const val PLACE_WALK = "place_walk"
    const val PLACE_ON_WAY = "place_on_the_way"
    const val PLACE_GUEST = "place_guest"
    const val PLACE_CAFE = "place_cafe"
    const val PLACE_SHOPPING = "place_shopping"
    const val PLACE_TRAVEL = "place_travel"
    const val PLACE_PARTY = "place_party"

    // Погода
    const val WEATHER_CLEAR = "weather_clear"
    const val WEATHER_CLOUDY1 = "weather_cloudy1"
    const val WEATHER_CLOUDY2 = "weather_cloudy2"
    const val WEATHER_BAD = "weather_bad"

    // Цвета эмоций (для графиков и календаря)
    const val COLOR_HAPPY = 0xFFFFCA28.toInt()
    const val COLOR_JOY = 0xFFff8614.toInt()
    const val COLOR_CALM = 0xFF66BB6A.toInt()
    const val COLOR_NEUTRAL = 0xFF8a8a8a.toInt()
    const val COLOR_TIRED = 0xFF8A2BE2.toInt()
    const val COLOR_SAD = 0xFF42A5F5.toInt()
    const val COLOR_ANGRY = 0xFFe02900.toInt()
    const val COLOR_EMPTY = 0xFFF0F0F5.toInt()

    // Порядок сортировки
    val MOOD_ORDER =
        listOf(
            MOOD_HAPPY,
            MOOD_JOY,
            MOOD_CALM,
            MOOD_NEUTRAL,
            MOOD_TIRED,
            MOOD_SAD,
            MOOD_ANGRY
        )

    val PLACE_ORDER = listOf(
        PLACE_HOME,
        PLACE_WORK,
        PLACE_WALK,
        PLACE_ON_WAY,
        PLACE_GUEST,
        PLACE_CAFE,
        PLACE_SHOPPING,
        PLACE_TRAVEL,
        PLACE_PARTY
    )
    val WEATHER_ORDER = listOf(WEATHER_CLEAR, WEATHER_CLOUDY1, WEATHER_CLOUDY2, WEATHER_BAD)

    // Ключ настроения -> строковый ресурс (локализованное название)
    fun moodResId(key: String): Int = when (key) {
        MOOD_HAPPY -> R.string.mood_happy
        MOOD_JOY -> R.string.mood_joy
        MOOD_CALM -> R.string.mood_calm
        MOOD_NEUTRAL -> R.string.mood_neutral
        MOOD_TIRED -> R.string.mood_tired
        MOOD_SAD -> R.string.mood_sad
        MOOD_ANGRY -> R.string.mood_angry
        else -> R.string.mood_neutral
    }

    // Ключ настроения -> строка в легенде (без эмодзи)
    fun moodColorResId(key: String): Int = when (key) {
        MOOD_HAPPY -> R.string.mood_happy_color
        MOOD_JOY -> R.string.mood_joy_color
        MOOD_CALM -> R.string.mood_calm_color
        MOOD_NEUTRAL -> R.string.mood_neutral_color
        MOOD_TIRED -> R.string.mood_tired_color
        MOOD_SAD -> R.string.mood_sad_color
        MOOD_ANGRY -> R.string.mood_angry_color
        else -> R.string.mood_neutral_color
    }

    // Ключ места -> строковый ресурс
    fun placeResId(key: String): Int = when (key) {
        PLACE_HOME -> R.string.place_home
        PLACE_WORK -> R.string.place_work
        PLACE_WALK -> R.string.place_walk
        PLACE_ON_WAY -> R.string.place_on_the_way
        PLACE_GUEST -> R.string.place_guest
        PLACE_CAFE -> R.string.place_cafe
        PLACE_SHOPPING -> R.string.place_shopping
        PLACE_TRAVEL -> R.string.place_travel
        PLACE_PARTY -> R.string.place_party
        else -> R.string.place_home
    }

    // Ключ погоды -> строковый ресурс
    fun weatherResId(key: String): Int = when (key) {
        WEATHER_CLEAR -> R.string.weather_clear
        WEATHER_CLOUDY1 -> R.string.weather_cloudy1
        WEATHER_CLOUDY2 -> R.string.weather_cloudy2
        WEATHER_BAD -> R.string.weather_bad
        else -> R.string.weather_clear
    }

    // Ключ настроения -> цвет
    fun moodColor(key: String?): Int = when (key) {
        MOOD_HAPPY -> COLOR_HAPPY
        MOOD_JOY -> COLOR_JOY
        MOOD_CALM -> COLOR_CALM
        MOOD_NEUTRAL -> COLOR_NEUTRAL
        MOOD_TIRED -> COLOR_TIRED
        MOOD_SAD -> COLOR_SAD
        MOOD_ANGRY -> COLOR_ANGRY
        else -> COLOR_EMPTY
    }

    // Ключ места -> цвет
    fun placeColor(key: String): Int = when (key) {
        PLACE_HOME -> 0xFF607D8B.toInt()
        PLACE_WORK -> 0xFF4CAF50.toInt()
        PLACE_WALK -> 0xFFFFC107.toInt()
        PLACE_ON_WAY -> 0xFF009688.toInt()
        PLACE_GUEST -> 0xFFE91E63.toInt()
        PLACE_CAFE -> 0xFFFF5722.toInt()
        PLACE_SHOPPING -> 0xFF795548.toInt()
        PLACE_TRAVEL -> 0xFF3F51B5.toInt()
        PLACE_PARTY -> 0xFF9C27B0.toInt()
        else -> 0xFF476fff.toInt()
    }

    // Ключ погоды -> цвет
    fun weatherColor(key: String): Int = when (key) {
        WEATHER_CLEAR -> 0xFFFFCA28.toInt()
        WEATHER_CLOUDY1 -> 0xFF78909C.toInt()
        WEATHER_CLOUDY2 -> 0xFF90A4AE.toInt()
        WEATHER_BAD -> 0xFF5C6BC0.toInt()
        else -> 0xFF476fff.toInt()
    }

    // Эмодзи для подписи под столбцом графика
    fun moodEmoji(key: String): String = when (key) {
        MOOD_HAPPY -> "\uD83E\uDD70"
        MOOD_JOY -> "😊"
        MOOD_CALM -> "\uD83D\uDE07"
        MOOD_NEUTRAL -> "😐"
        MOOD_TIRED -> "\uD83D\uDE34"
        MOOD_SAD -> "😢"
        MOOD_ANGRY -> "😡"
        else -> ""
    }

    fun placeEmoji(key: String): String = when (key) {
        PLACE_HOME -> "🏡"
        PLACE_WORK -> "💼"
        PLACE_WALK -> "⛺"
        PLACE_ON_WAY -> "\uD83D\uDE99"
        PLACE_GUEST -> "\uD83E\uDD42"
        PLACE_CAFE -> "☕"
        PLACE_SHOPPING -> "\uD83D\uDCB3"
        PLACE_TRAVEL -> "\uD83D\uDDFA\uFE0F"
        PLACE_PARTY -> "\uD83C\uDF89"
        else -> ""
    }

    fun weatherEmoji(key: String): String = when (key) {
        WEATHER_CLEAR -> "☀️"
        WEATHER_CLOUDY1 -> "⛅"
        WEATHER_CLOUDY2 -> "☁️"
        WEATHER_BAD -> "⛈️"
        else -> ""
    }

    // Угадать тип ключа и вернуть правильный цвет
    fun colorForKey(key: String): Int = when {
        MOOD_ORDER.contains(key) -> moodColor(key)
        PLACE_ORDER.contains(key) -> placeColor(key)
        WEATHER_ORDER.contains(key) -> weatherColor(key)
        else -> 0xFF476fff.toInt()
    }

    fun emojiForKey(key: String): String = when {
        MOOD_ORDER.contains(key) -> moodEmoji(key)
        PLACE_ORDER.contains(key) -> placeEmoji(key)
        WEATHER_ORDER.contains(key) -> weatherEmoji(key)
        else -> ""
    }

    fun sortIndex(key: String): Int {
        MOOD_ORDER.indexOf(key).let { if (it >= 0) return it }
        PLACE_ORDER.indexOf(key).let { if (it >= 0) return 10 + it }
        WEATHER_ORDER.indexOf(key).let { if (it >= 0) return 20 + it }
        return 99
    }

    // Миграция: угадать ключ из старой локализованной строки
    // Используется ТОЛЬКО в миграции БД 4→5 через SQL CASE WHEN.
    // Здесь для справки — в коде используется SQL-вариант в AppDatabase.
}
