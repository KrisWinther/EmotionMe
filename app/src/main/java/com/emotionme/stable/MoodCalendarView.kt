package com.emotionme.stable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import androidx.core.graphics.toColorInt


@Suppress("DEPRECATION")
class MoodCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity

    private var year = 0
    private var month = 0   // Начало отсчёта месяцев (Январь == 0..Декабрь == 11)
    private var moodMap: Map<Int, String> = emptyMap()

    // Цвета эмоций
    private val colorJoy = "#66BB6A".toColorInt()   // зелёный
    private val colorNeutral = "#FFCA28".toColorInt()   // жёлтый
    private val colorSad = "#42A5F5".toColorInt()   // голубой
    private val colorAngry = "#EF5350".toColorInt()   // красный
    private val colorEmpty = "#F0F0F5".toColorInt()   // нет данных (серый)
    private val colorToday = "#476fff".toColorInt()   // фиолетовый контур — сегодня

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * dp
        color = colorToday
    }
    private val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = "#FF000000".toColorInt()
        typeface = Typeface.DEFAULT_BOLD
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#18000000".toColorInt()
    }

    private val dayNames = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

    fun setData(year: Int, month: Int, map: Map<Int, String>) {
        this.year = year
        this.month = month
        this.moodMap = map
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val cellSize = (w - 16 * dp) / 7f
        val headerH = cellSize * 0.6f
        val rows = 6
        val h = (headerH + rows * cellSize + 30 * dp).toInt()
        setMeasuredDimension(w, h)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (year == 0) return

        val padH = 8f * dp
        val cellSize = (width - padH * 2) / 7f
        val headerH = cellSize * 0.6f
        headerPaint.textSize = 11f * sp
        dayPaint.textSize = 12f * sp

        // Заголовок дней недели
        dayNames.forEachIndexed { i, name ->
            val cx = padH + i * cellSize + cellSize / 2f
            canvas.drawText(name, cx, headerH * 0.75f, headerPaint)
        }

        // Вычислить, с какого дня начался месяц (1=Пн..7=Вс)
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7) // 0=Пн
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == year &&
                today.get(Calendar.MONTH) == month
        val todayDay = today.get(Calendar.DAY_OF_MONTH)

        val radius = 10f * dp

        for (day in 1..daysInMonth) {
            val pos = day - 1 + firstDow
            val col = pos % 7
            val row = pos / 7

            val left = padH + col * cellSize + 3 * dp
            val top = headerH + row * cellSize + 3 * dp
            val right = left + cellSize - 6 * dp
            val bottom = top + cellSize - 6 * dp
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f

            val dominantMood = moodMap[day]
            val fillColor = moodColor(dominantMood)

            // Отрисовка тени
            val shadowRect = RectF(left + 2 * dp, top + 2 * dp, right + 2 * dp, bottom + 2 * dp)
            canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)

            // Отрисовка ячейки дня
            cellPaint.color = fillColor
            val cellRect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(cellRect, radius, radius, cellPaint)

            // Контур для сегодняшнего дня
            if (isCurrentMonth && day == todayDay) {
                canvas.drawRoundRect(cellRect, radius, radius, borderPaint)
            }

            // Номер дня
            dayPaint.color = dayTextColor(fillColor)
            canvas.drawText(day.toString(), cx, cy + dayPaint.textSize * 0.38f, dayPaint)
        }

        // Легенда снизу
        drawLegend(canvas, padH, headerH + 6 * cellSize + 10 * dp)
    }

    private fun drawLegend(canvas: Canvas, startX: Float, y: Float) {
        val items = listOf(
            colorJoy to "Радость",
            colorNeutral to "Нейтрально",
            colorSad to "Грусть",
            colorAngry to "Злость"
        )
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f * sp
            color = "#FF000000".toColorInt()
        }
        val dotR = 6f * dp
        val gap = 10f * dp

        // Считаем суммарную ширину легенды
        val totalWidth = items.sumOf { (_, label) ->
            (dotR * 2 + 4 * dp + legendPaint.measureText(label) + gap).toDouble()
        }.toFloat() - gap  // убираем лишний gap после последнего элемента

        var x = (width - totalWidth) / 2f

        items.forEach { (color, label) ->
            cellPaint.color = color
            canvas.drawCircle(x + dotR, y, dotR, cellPaint)
            x += dotR * 2 + 4 * dp
            canvas.drawText(label, x, y + dotR * 0.4f, legendPaint)
            x += legendPaint.measureText(label) + gap
        }
    }

    private fun moodColor(mood: String?): Int = when {
        mood == null -> colorEmpty
        mood.contains("😊") || mood.contains("Радость", ignoreCase = true) -> colorJoy
        mood.contains("😐") || mood.contains("Нейтрально", ignoreCase = true) -> colorNeutral
        mood.contains("😢") || mood.contains("Грусть", ignoreCase = true) -> colorSad
        mood.contains("😡") || mood.contains("Злость", ignoreCase = true) -> colorAngry
        else -> colorEmpty
    }

    private fun dayTextColor(bg: Int): Int {
        val r = Color.red(bg) / 255.0
        val g = Color.green(bg) / 255.0
        val b = Color.blue(bg) / 255.0
        val lum = 0.299 * r + 0.587 * g + 0.114 * b
        return if (lum > 0.65) "#CC000000".toColorInt() else Color.WHITE
    }
}
