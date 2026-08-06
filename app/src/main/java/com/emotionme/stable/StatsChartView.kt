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
    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity

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
    private val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#18000000".toColorInt() }
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
        data = items.sortedBy { MoodKeys.sortIndex(it.label) }
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padL = 48f * dp;
        val padR = 16f * dp
        val padT = 24f * dp;
        val padB = 52f * dp
        val chartW = width - padL - padR
        val chartH = height - padT - padB

        if (data.isEmpty()) {
            canvas.drawText(
                context.getString(R.string.no_data_column),
                width / 2f, height / 2f + 5 * dp, emptyPaint
            )
            return
        }

        val maxVal = max(1, data.maxOf { it.count })
        val topVal = niceMax(maxVal)
        val stepVal = when {
            topVal <= 10 -> 1; topVal <= 20 -> 2; topVal <= 30 -> 3
            topVal <= 40 -> 4; topVal <= 50 -> 5; topVal <= 60 -> 6
            topVal <= 70 -> 7; topVal <= 80 -> 8; topVal <= 90 -> 9
            topVal <= 100 -> 10; topVal <= 250 -> 25; topVal <= 500 -> 50
            topVal <= 1000 -> 100; else -> 200
        }
        val steps = topVal / stepVal

        // Сетка + шкала Y
        for (i in 0..steps) {
            val v = i * stepVal
            val y = padT + chartH - (v.toFloat() / topVal) * chartH
            canvas.drawLine(
                padL,
                y,
                padL + chartW,
                y,
                gridPaint
            )
            canvas.drawText(
                v.toString(),
                padL - 6 * dp,
                y + 4 * dp,
                scalePaint
            )
        }
        canvas.drawLine(
            padL,
            padT + chartH,
            padL + chartW,
            padT + chartH,
            axisPaint
        )
        canvas.drawLine(
            padL,
            padT,
            padL,
            padT + chartH,
            axisPaint
        )

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
            canvas.drawRoundRect(
                RectF(left + 3 * dp, top + 3 * dp, right + 3 * dp, bottom),
                radius, radius, shadowPaint
            )

            // Столбец — цвет по ключу
            barPaint.color = MoodKeys.colorForKey(item.label)
            canvas.drawRoundRect(RectF(left, top, right, bottom),
                radius,
                radius, barPaint
            )

            // Значение
            if (barH > 28 * dp) {
                canvas.drawText(item.count.toString(), cx, top + 20 * dp, valuePaint)
            } else {
                val overPaint = Paint(scalePaint).apply { textAlign = Paint.Align.CENTER }
                canvas.drawText(item.count.toString(), cx, top - 4 * dp, overPaint)
            }

            // Эмодзи под столбцом
            val emoji = MoodKeys.emojiForKey(item.label)
            if (emoji.isNotEmpty()) {
                canvas.drawText(emoji, cx, padT + chartH + 25 * dp, labelPaint)
            }
        }
    }

    private fun niceMax(v: Int): Int {
        if (v <= 4) return 4
        val step = when {
            v <= 10 -> 2; v <= 20 -> 5; v <= 50 -> 10
            v <= 100 -> 25; v <= 250 -> 50; v <= 500 -> 100
            v <= 1000 -> 250; else -> 500
        }
        return ((v + step - 1) / step) * step
    }
}
