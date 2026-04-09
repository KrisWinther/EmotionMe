package com.emotionme.stable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.max

@Suppress("DEPRECATION")
class StatsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<StatItem> = emptyList()
    private val sp = resources.displayMetrics.scaledDensity
    private val dp = resources.displayMetrics.density

    // Цвета эмоций
    private val colorHappy = "#FFCA28".toColorInt() // жёлтый
    private val colorJoy = "#ff8614".toColorInt()   // оранжевый
    private val colorPeace = "#66BB6A".toColorInt() // зелёный
    private val colorNeutral = "#8a8a8a".toColorInt()   // серый
    private val colorTired = "#8A2BE2".toColorInt() // фиолетовый
    private val colorSad = "#42A5F5".toColorInt()   // голубой
    private val colorAngry = "#e02900".toColorInt()   // красный

    // Краски
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#e5e4e2".toColorInt()
        strokeWidth = 1f * dp
        style = Paint.Style.STROKE
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF000000".toColorInt()
        strokeWidth = 1.5f * dp
        style = Paint.Style.STROKE
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#18000000".toColorInt()
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 13f * sp
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#CC000000".toColorInt()
        textAlign = Paint.Align.CENTER
        textSize = 15f * sp
    }
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF000000".toColorInt()
        textAlign = Paint.Align.RIGHT
        textSize = 10f * sp
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#88000000".toColorInt()
        textAlign = Paint.Align.CENTER
        textSize = 14f * sp
    }

    fun setData(items: List<StatItem>) {
        data = items
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padL = 48f * dp
        val padR = 16f * dp
        val padT = 24f * dp
        val padB = 52f * dp

        val chartW = width - padL - padR
        val chartH = height - padT - padB

        if (data.isEmpty()) {
            canvas.drawText(
                "Нет данных за период (╯°□°）╯︵ ┻━┻ ",
                width / 2f, height / 2f + 5 * dp, emptyPaint
            )
            return
        }

        val maxVal = max(1, data.maxOf { it.count })
        // Округляем верхнюю границу до красивого числа
        val topVal = niceMax(maxVal)
        val stepVal = when {
            topVal <= 10 -> 1
            topVal <= 20 -> 2
            topVal <= 30 -> 3
            topVal <= 40 -> 4
            topVal <= 50 -> 5
            topVal <= 60 -> 6
            topVal <= 70 -> 7
            topVal <= 80 -> 8
            topVal <= 90 -> 9
            topVal <= 100 -> 10
            topVal <= 250 -> 25
            topVal <= 500 -> 50
            topVal <= 1000 -> 100
            else -> 200
        }
        val steps = topVal / stepVal


        // Горизонтальные линии сетки + шкала Y
        for (i in 0..steps) {
            val v = i * stepVal
            val y = padT + chartH - (v.toFloat() / topVal) * chartH
            canvas.drawLine(padL, y, padL + chartW, y, gridPaint)
            canvas.drawText(v.toString(), padL - 6 * dp, y + 4 * dp, scalePaint)
        }

        // Ось X
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)
        // Ось Y
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)

        val emotionOrder = listOf(
            "Счастье",
            "Радость",
            "Умиротворение",
            "Нейтрально",
            "Усталость",
            "Грусть",
            "Злость"
        )

        data = data.sortedWith(
            compareBy {
                labelToOrderIndex(
                    it.label,
                    emotionOrder
                )
            }
        )

        val locationOrder = listOf(
            "Дома",
            "\uD83D\uDCBC",
            "На прогулке",
            "В пути",
            "В гостях",
            "В ресторане",
            "В магазине",
            "В путешествии",
            "На вечеринке"
        )

        data = data.sortedWith(
            compareBy {
                labelToOrderIndex(
                    it.label,
                    locationOrder
                )
            }
        )

        val weatherOrder = listOf(
            "Солнечно",
            "Пасмурно",
            "Облачно",
            "Непогода"
        )

        data = data.sortedWith(
            compareBy {
                labelToOrderIndex(
                    it.label,
                    weatherOrder
                )
            }
        )

        // Шкалы для диаграмм
        val barSlot = chartW / data.size
        val barW = barSlot * 0.55f
        val radius = 8f * dp

        data.forEachIndexed { i, item ->
            val cx = padL + barSlot * i + barSlot / 2f
            val barH = (item.count.toFloat() / topVal) * chartH
            val left = cx - barW / 2f
            val right = cx + barW / 2f
            val top = padT + chartH - barH
            val bottom = padT + chartH

            // Тень
            val shadowRect = RectF(left + 3 * dp, top + 3 * dp, right + 3 * dp, bottom)
            canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)

            // Заливка шкалы
            val color = getColorForItem(item.label)
            barPaint.shader = null
            barPaint.color = color
            val barRect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(barRect, radius, radius, barPaint)

            // Значение внутри шкалы (если шкалв достаточно высокая)
            if (barH > 28 * dp) {
                canvas.drawText(item.count.toString(), cx, top + 20 * dp, valuePaint)
            } else {
                // Значение над шкалой
                val overPaint = Paint(scalePaint).apply { textAlign = Paint.Align.CENTER }
                canvas.drawText(item.count.toString(), cx, top - 4 * dp, overPaint)
            }

            // Эмодзи
            val emoji = emojiFor(item.label)
            if (emoji.isNotEmpty()) {
                canvas.drawText(emoji, cx, padT + chartH + 25 * dp, labelPaint)
            }
        }
    }

    private fun niceMax(v: Int): Int {
        if (v <= 4) return 4
        val step = when {
            v <= 10 -> 2
            v <= 20 -> 5
            v <= 50 -> 10
            v <= 100 -> 25
            v <= 250 -> 50
            v <= 500 -> 100
            v <= 1000 -> 250
            else -> 500
        }
        return ((v + step - 1) / step) * step
    }

    private fun emojiFor(label: String): String = when {
        label.contains("\uD83E\uDD70") -> "\uD83E\uDD70"
        label.contains("😊") -> "😊"
        label.contains("\uD83D\uDE07") -> "\uD83D\uDE07"
        label.contains("😐") -> "😐"
        label.contains("\uD83D\uDE34") -> "\uD83D\uDE34"
        label.contains("😢") -> "😢"
        label.contains("😡") -> "😡"

        label.contains("🏡") -> "🏡"
        label.contains("📚") || label.contains("💼") -> "💼"
        label.contains("⛺") -> "⛺"
        label.contains("\uD83D\uDEB6") || label.contains("\uD83D\uDE99") -> "\uD83D\uDE99"
        label.contains("\uD83E\uDD42") -> "\uD83E\uDD42"
        label.contains("\uD83C\uDF5D") || label.contains("☕") -> "☕"
        label.contains("\uD83D\uDCB3") -> "\uD83D\uDCB3"
        label.contains("\uD83D\uDDFA\uFE0F") -> "\uD83D\uDDFA\uFE0F"
        label.contains("\uD83C\uDF89") -> "\uD83C\uDF89"

        label.contains("☀") -> "☀️"
        label.contains("⛅") -> "⛅"
        label.contains("☁") -> "☁️"
        label.contains("⛈") -> "⛈️"
        else -> ""
    }

    private fun getColorForItem(label: String): Int = when {

        // Эмоции
        label.contains("😡") || label.contains("Злость", ignoreCase = true) -> colorAngry
        label.contains("😢") || label.contains("Грусть", ignoreCase = true) -> colorSad
        label.contains("\uD83D\uDE34") || label.contains(
            "Усталость",
            ignoreCase = true
        ) -> colorTired

        label.contains("😐") || label.contains("Нейтрально", ignoreCase = true) -> colorNeutral
        label.contains("\uD83D\uDE07") || label.contains(
            "Умиротворение",
            ignoreCase = true
        ) -> colorPeace

        label.contains("😊") || label.contains("Радость", ignoreCase = true) -> colorJoy
        label.contains("\uD83E\uDD70") || label.contains("Счастье", ignoreCase = true) -> colorHappy

        // Места
        label.contains("Дома", ignoreCase = true) -> "#607D8B".toColorInt()
        label.contains("На учёбе") || label.contains("На работе") -> "#4CAF50".toColorInt()
        label.contains("На прогулке") -> "#FFC107".toColorInt()
        label.contains("В пути") || label.contains("В транспорте") -> "#009688".toColorInt()
        label.contains("В гостях") -> "#E91E63".toColorInt()
        label.contains("В ресторане") || label.contains("В кафе") -> "#FF5722".toColorInt()
        label.contains("В магазине") -> "#795548".toColorInt()
        label.contains("В путешествии") -> "#3F51B5".toColorInt()
        label.contains("На вечеринке") -> "#9C27B0".toColorInt()

        // Погода
        label.contains("Солнечно") -> "#FFCA28".toColorInt()
        label.contains("Пасмурно") -> "#78909C".toColorInt()
        label.contains("Облачно") -> "#90A4AE".toColorInt()
        label.contains("Непогода") -> "#5C6BC0".toColorInt()

        else -> "#476fff".toColorInt()
    }

    private fun labelToOrderIndex(label: String, order: List<String>): Int {
        for ((index, emotion) in order.withIndex()) {
            if (label.contains(emotion, ignoreCase = true)) {
                return index
            }
        }

        for ((index, location) in order.withIndex()) {
            if (label.contains(location, ignoreCase = true)) {
                return index
            }
        }

        for ((index, weather) in order.withIndex()) {
            if (label.contains(weather, ignoreCase = true)) {
                return index
            }
        }

        return Int.MAX_VALUE
    }
}
