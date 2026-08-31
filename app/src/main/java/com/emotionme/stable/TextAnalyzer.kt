package com.emotionme.stable

/**
 * Офлайн-анализатор текста заметок дневника настроения.
 *
 * Демонстрирует подход "словарь + эвристики" без обращения к сети:
 *  - тональность текста (sentiment score от -1.0 до +1.0)
 *  - предполагаемое настроение (сопоставляется с MoodKeys.MOOD_*)
 *  - ключевые слова (по частоте + вычищенным стоп-словам)
 *  - категория события (учёба/работа/друзья/семья/здоровье/отдых/...)
 *
 * Интерфейс сделан так, чтобы позже можно было подменить эту реализацию
 * на вызов облачного API (Claude/GPT), не трогая остальной код:
 * достаточно написать другой класс с тем же методом analyze(text) -> TextAnalysisResult.
 */
object TextAnalyzer {

    data class TextAnalysisResult(
        val sentimentScore: Float,      // -1.0 (очень негативно) .. +1.0 (очень позитивно)
        val moodKey: String,            // один из MoodKeys.MOOD_*
        val keywords: List<String>,     // топ ключевых слов из текста
        val eventCategory: String,      // одна из CATEGORY_* ниже
        val confidence: Float           // 0.0..1.0 — грубая оценка уверенности словарного метода
    )

    // ──────────────────────────────────────────────────────────
    // Категории событий
    // ──────────────────────────────────────────────────────────

    const val CATEGORY_STUDY = "category_study"
    const val CATEGORY_WORK = "category_work"
    const val CATEGORY_FRIENDS = "category_friends"
    const val CATEGORY_FAMILY = "category_family"
    const val CATEGORY_HEALTH = "category_health"
    const val CATEGORY_REST = "category_rest"
    const val CATEGORY_OTHER = "category_other"

    // ──────────────────────────────────────────────────────────
    // Словари. В реальном проекте стоит вынести в JSON/ресурсы,
    // здесь — прямо в коде для наглядности демонстрации.
    // ──────────────────────────────────────────────────────────

    // Слово -> вес тональности. Простые леммы/основы без окончаний,
    // сравнение идёт через "содержит подстроку" (см. matchWeight).
    private val POSITIVE_WORDS: Map<String, Float> = mapOf(
        "рад" to 0.8f, "счастл" to 1.0f, "весел" to 0.7f, "прекрасн" to 0.8f,
        "отличн" to 0.8f, "здоров" to 0.5f, "люб" to 0.7f, "нрав" to 0.5f,
        "спокойн" to 0.5f, "хорош" to 0.6f, "успел" to 0.4f, "получилось" to 0.6f,
        "гуля" to 0.5f, "друз" to 0.4f, "улыб" to 0.6f, "вкусн" to 0.4f,
        "отдохн" to 0.6f, "победа" to 0.8f, "легко" to 0.4f, "интересн" to 0.5f,
        "поддерж" to 0.5f, "горд" to 0.6f, "благодар" to 0.6f, "энерг" to 0.5f
    )

    private val NEGATIVE_WORDS: Map<String, Float> = mapOf(
        "тяжел" to -0.7f, "устал" to -0.6f, "плохо" to -0.7f, "грустн" to -0.8f,
        "злюсь" to -0.8f, "злост" to -0.8f, "бесит" to -0.8f, "раздраж" to -0.6f,
        "болит" to -0.6f, "болею" to -0.6f, "провал" to -0.7f, "не получилось" to -0.6f,
        "ссор" to -0.7f, "обид" to -0.7f, "страшно" to -0.7f, "боюсь" to -0.7f,
        "одинок" to -0.7f, "стресс" to -0.6f, "завалили" to -0.5f, "паник" to -0.8f,
        "плак" to -0.8f, "тревог" to -0.7f, "разочаров" to -0.7f, "измотан" to -0.7f
    )

    // Слова-переключатели контраста ("но", "зато", "однако") —
    // после них вес второй части предложения усиливается.
    private val CONTRAST_MARKERS = listOf(" но ", " зато ", " однако ", " хотя ", " впрочем ")

    // Отрицания — переворачивают знак ближайшего следующего слова из словаря.
    private val NEGATION_MARKERS = listOf("не ", "ни ", "нет ")

    private val CATEGORY_KEYWORDS: Map<String, List<String>> = mapOf(
        CATEGORY_STUDY to listOf("домашк", "учеб", "экзамен", "школ", "универ", "лекци", "сесси", "зачет", "препод"),
        CATEGORY_WORK to listOf("работ", "начальник", "проект", "дедлайн", "коллег", "офис", "зарплат", "совещан"),
        CATEGORY_FRIENDS to listOf("друз", "погуля", "встретил", "тусовк", "компани"),
        CATEGORY_FAMILY to listOf("сем", "мам", "пап", "родител", "брат", "сестр", "дет", "муж", "жена"),
        CATEGORY_HEALTH to listOf("болит", "болею", "врач", "здоров", "температур", "лекарств", "сон", "спал"),
        CATEGORY_REST to listOf("отдохн", "отпуск", "фильм", "книг", "прогулк", "путешеств", "игра")
    )

    private val STOP_WORDS = setOf(
        "и", "в", "во", "не", "что", "он", "на", "я", "с", "со", "как", "а", "то", "все",
        "она", "так", "его", "но", "да", "ты", "к", "у", "же", "вы", "за", "бы", "по",
        "только", "ее", "мне", "было", "вот", "от", "меня", "еще", "нет", "о", "из",
        "ему", "теперь", "когда", "даже", "ну", "вдруг", "ли", "если", "уже", "или",
        "ни", "быть", "был", "него", "до", "вас", "нибудь", "опять", "уж", "вам",
        "сказал", "ведь", "там", "потом", "себя", "ничего", "ей", "может", "они",
        "тут", "где", "есть", "надо", "ней", "для", "мы", "тебя", "их", "чем",
        "была", "сам", "чтоб", "без", "будто", "чего", "раз", "тоже", "себе", "под",
        "будет", "ж", "тогда", "кто", "этот", "того", "потому", "этого", "какой",
        "совсем", "ним", "здесь", "этом", "один", "почти", "мой", "тем", "чтобы",
        "нее", "сегодня", "сейчас", "было", "потому"
    )

    // ──────────────────────────────────────────────────────────
    // Основной метод
    // ──────────────────────────────────────────────────────────

    fun analyze(rawText: String): TextAnalysisResult {
        val text = " " + rawText.lowercase().trim() + " "

        if (text.isBlank()) {
            return TextAnalysisResult(0f, MoodKeys.MOOD_NEUTRAL, emptyList(), CATEGORY_OTHER, 0f)
        }

        val sentiment = computeSentiment(text)
        val mood = sentimentToMood(sentiment)
        val keywords = extractKeywords(rawText)
        val category = detectCategory(text)
        val confidence = estimateConfidence(text)

        return TextAnalysisResult(
            sentimentScore = sentiment,
            moodKey = mood,
            keywords = keywords,
            eventCategory = category,
            confidence = confidence
        )
    }

    // ──────────────────────────────────────────────────────────
    // Тональность
    // ──────────────────────────────────────────────────────────

    private fun computeSentiment(text: String): Float {
        // Делим текст на части по контрастным маркерам ("но", "зато"...).
        // Идея: "было тяжело, но вечером погулял с друзьями" —
        // часть после "но" должна весить больше, т.к. она обычно
        // отражает итоговое/более важное состояние автора.
        val segments = splitByContrast(text)

        var totalScore = 0f
        var totalWeight = 0f

        segments.forEachIndexed { index, segment ->
            // Последний сегмент (после последнего "но"/"зато") весит заметно сильнее:
            // по-русски именно вторая часть после контрастного союза обычно
            // отражает итоговое состояние автора ("тяжело, но погулял с друзьями" → скорее позитив).
            val segmentWeight = if (index == segments.lastIndex && segments.size > 1) 3.0f else 1.0f
            val (score, hits) = scoreSegment(segment)
            if (hits > 0) {
                totalScore += score * segmentWeight
                totalWeight += hits * segmentWeight
            }
        }

        if (totalWeight == 0f) return 0f

        val avg = totalScore / totalWeight
        return avg.coerceIn(-1f, 1f)
    }

    private fun splitByContrast(text: String): List<String> {
        var parts = listOf(text)
        for (marker in CONTRAST_MARKERS) {
            parts = parts.flatMap { it.split(marker) }
        }
        return parts.filter { it.isNotBlank() }
    }

    /** Возвращает (сумма весов совпавших слов, количество совпадений) для одного сегмента текста. */
    private fun scoreSegment(segment: String): Pair<Float, Int> {
        var score = 0f
        var hits = 0

        val allWords = POSITIVE_WORDS.asSequence().map { it to false } + NEGATIVE_WORDS.asSequence().map { it to true }

        for ((entry, _) in allWords) {
            val (stem, weight) = entry
            var searchFrom = 0
            while (true) {
                val idx = segment.indexOf(stem, searchFrom)
                if (idx == -1) break
                searchFrom = idx + stem.length

                // Проверяем отрицание перед словом (в пределах ~4 слов до совпадения)
                val windowStart = maxOf(0, idx - 20)
                val window = segment.substring(windowStart, idx)
                val negated = NEGATION_MARKERS.any { window.endsWith(it) || window.contains("$it ") }

                val effectiveWeight = if (negated) -weight else weight
                score += effectiveWeight
                hits += 1
            }
        }

        return score to hits
    }

    private fun sentimentToMood(score: Float): String = when {
        score >= 0.6f -> MoodKeys.MOOD_HAPPY
        score >= 0.25f -> MoodKeys.MOOD_JOY
        score >= 0.05f -> MoodKeys.MOOD_CALM
        score > -0.05f -> MoodKeys.MOOD_NEUTRAL
        score > -0.3f -> MoodKeys.MOOD_TIRED
        score > -0.6f -> MoodKeys.MOOD_SAD
        else -> MoodKeys.MOOD_ANGRY
    }

    // Грубая оценка уверенности: чем больше словарных попаданий
    // относительно длины текста — тем увереннее мы в результате.
    private fun estimateConfidence(text: String): Float {
        val wordCount = text.trim().split(Regex("\\s+")).size.coerceAtLeast(1)
        val (_, hits) = scoreSegment(text)
        // Доля слов текста, попавших в словарь тональности, умноженная на 2
        // и ограниченная сверху 1.0 — так 2-3 совпадения в короткой заметке
        // уже дают уверенность близкую к максимальной.
        val ratio = (hits.toFloat() / wordCount) * 2f
        return ratio.coerceIn(0f, 1f)
    }

    // ──────────────────────────────────────────────────────────
    // Ключевые слова
    // ──────────────────────────────────────────────────────────

    private fun extractKeywords(rawText: String, maxKeywords: Int = 5): List<String> {
        val words = rawText
            .lowercase()
            .replace(Regex("[^а-яёa-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 && it !in STOP_WORDS }

        return words
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .distinct()
            .take(maxKeywords)
    }

    // ──────────────────────────────────────────────────────────
    // Категория события
    // ──────────────────────────────────────────────────────────

    private fun detectCategory(text: String): String {
        var bestCategory = CATEGORY_OTHER
        var bestHits = 0

        for ((category, stems) in CATEGORY_KEYWORDS) {
            val hits = stems.count { text.contains(it) }
            if (hits > bestHits) {
                bestHits = hits
                bestCategory = category
            }
        }
        return bestCategory
    }

    // Локализованная подпись категории для быстрой обратной связи в UI
    // (Snackbar сразу после сохранения). В идеале стоит вынести в strings.xml
    // и локализовать через MoodKeys-подобный резолвер, здесь — для наглядности демо.
    fun categoryLabel(category: String): String = when (category) {
        CATEGORY_STUDY -> "учёба"
        CATEGORY_WORK -> "работа"
        CATEGORY_FRIENDS -> "друзья"
        CATEGORY_FAMILY -> "семья"
        CATEGORY_HEALTH -> "здоровье"
        CATEGORY_REST -> "отдых"
        else -> "обычный день"
    }

    // ──────────────────────────────────────────────────────────
    // Изменение настроения относительно предыдущих записей.
    // Чистая арифметика — ИИ здесь не нужен, только числовой score.
    // ──────────────────────────────────────────────────────────

    /**
     * @param currentScore sentimentScore текущей записи
     * @param previousScores sentimentScore последних N записей (без учёта текущей),
     *                       например через MoodDAO.getRecentScores(uid, limit = 7)
     * @return дельта: положительная — настроение улучшилось относительно среднего,
     *         отрицательная — ухудшилось. null если нет истории для сравнения.
     */
    fun moodDelta(currentScore: Float, previousScores: List<Float>): Float? {
        if (previousScores.isEmpty()) return null
        val avgPrevious = previousScores.average().toFloat()
        return currentScore - avgPrevious
    }
}
