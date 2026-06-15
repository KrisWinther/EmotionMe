package com.emotionme.stable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import java.util.Calendar

@Suppress("DEPRECATION")
class MoodCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity

    private var year = 0
    private var month = 0
    private var moodMap: Map<Int, String> = emptyMap()  // day → moodKey

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.5f * dp
        color = MoodKeys.COLOR_NEUTRAL
    }
    private val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = "#FF000000".toColorInt()
        typeface = Typeface.DEFAULT_BOLD
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#18000000".toColorInt()
    }

    // @param map  day-of-month -> moodKey (из MoodKeys.MOOD_*)
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
        val h = (headerH + 6 * cellSize + 30 * dp).toInt()
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

        // Заголовки дней недели — локализованы через ресурсы
        val dayNames = listOf(
            context.getString(R.string.day1),
            context.getString(R.string.day2),
            context.getString(R.string.day3),
            context.getString(R.string.day4),
            context.getString(R.string.day5),
            context.getString(R.string.day6),
            context.getString(R.string.day7)
        )
        dayNames.forEachIndexed { i, name ->
            canvas.drawText(name, padH + i * cellSize + cellSize / 2f, headerH * 0.75f, headerPaint)
        }

        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month
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

            // Цвет по moodKey из MoodKeys (язык-независимо)
            val fillColor = MoodKeys.moodColor(moodMap[day])

            canvas.drawRoundRect(
                RectF(left + 2 * dp, top + 2 * dp, right + 2 * dp, bottom + 2 * dp),
                radius, radius, shadowPaint
            )
            cellPaint.color = fillColor
            val cellRect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(cellRect, radius, radius, cellPaint)

            if (isCurrentMonth && day == todayDay) {
                canvas.drawRoundRect(cellRect, radius, radius, borderPaint)
            }

            dayPaint.color = dayTextColor(fillColor)
            canvas.drawText(day.toString(), cx, cy + dayPaint.textSize * 0.38f, dayPaint)
        }

        drawLegend(canvas, padH, headerH + 5.3 * cellSize + 15 * dp)
    }

    private fun drawLegend(canvas: Canvas, startX: Float, y: Double) {
        // Легенда — названия берём из ресурсов по ключу (локализованы)
        val items = MoodKeys.MOOD_ORDER.map { key ->
            MoodKeys.moodColor(key) to context.getString(MoodKeys.moodColorResId(key))
        }

        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f * sp; color = "#FF000000".toColorInt()
        }
        val dotR = 6f * dp
        val gapX = 10f * dp
        val gapY = 24f * dp
        val mid = (items.size + 1) / 2
        val rows = listOf(items.take(mid), items.drop(mid))

        rows.forEachIndexed { index, rowItems ->
            val rowWidth = rowItems.sumOf { (_, label) ->
                (dotR * 2 + 4 * dp + legendPaint.measureText(label) + gapX).toDouble()
            }.toFloat() - gapX

            var curX = (width - rowWidth) / 2f
            val curY = (y + index * gapY).toFloat()

            rowItems.forEach { (color, label) ->
                cellPaint.color = color
                canvas.drawCircle(curX + dotR, curY, dotR, cellPaint)
                curX += dotR * 2 + 4 * dp
                canvas.drawText(label, curX, curY + dotR * 0.4f, legendPaint)
                curX += legendPaint.measureText(label) + gapX
            }
        }
    }

    private fun dayTextColor(bg: Int): Int {
        val lum = 0.299 * Color.red(bg) / 255.0 +
                0.587 * Color.green(bg) / 255.0 +
                0.114 * Color.blue(bg) / 255.0
        return if (lum > 0.65) "#CC000000".toColorInt() else Color.WHITE
    }
}
