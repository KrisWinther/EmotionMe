package com.emotionme.stable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

/**
 * Ломаная линия тренда настроения по дням месяца, построенная на данных
 * TextAnalyzer.sentimentScore (офлайн-анализ текста заметок), а не на
 * ручном выборе mood из спиннера — в отличие от столбчатых графиков
 * (StatsChartView), которые показывают именно то, что выбрал пользователь.
 *
 * Ось X — дни месяца (1..daysInMonth), ось Y — sentimentScore от -1.0 до +1.0.
 * Дни без записей пропускаются (линия рвётся, а не идёт через 0),
 * чтобы не создавать ложное впечатление "нейтрального" дня там, где
 * пользователь просто не писал заметку.
 */
@Suppress("DEPRECATION")
class SentimentLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** @param day 1..31, @param avgScore среднее sentimentScore за этот день (null = записей не было) */
    data class DayScore(val day: Int, val avgScore: Float?)

    private var data: List<DayScore> = emptyList()
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
    private val zeroLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#AAAAAAAA".toColorInt()
        strokeWidth = 1f * dp
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f * dp, 6f * dp), 0f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * dp
    }
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF000000".toColorInt()
        textAlign = Paint.Align.RIGHT
        textSize = 10f * sp
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#CC000000".toColorInt()
        textAlign = Paint.Align.CENTER
        textSize = 10f * sp
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#88000000".toColorInt()
        textAlign = Paint.Align.CENTER
        textSize = 14f * sp
    }

    fun setData(items: List<DayScore>) {
        data = items.sortedBy { it.day }
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padL = 40f * dp
        val padR = 16f * dp
        val padT = 24f * dp
        val padB = 36f * dp
        val chartW = width - padL - padR
        val chartH = height - padT - padB

        val pointsWithData = data.filter { it.avgScore != null }

        if (pointsWithData.isEmpty()) {
            canvas.drawText(
                context.getString(R.string.no_data_column),
                width / 2f, height / 2f + 5 * dp, emptyPaint
            )
            return
        }

        val daysInMonth = data.maxOf { it.day }.coerceAtLeast(1)

        // Y: фиксированный диапазон -1.0..1.0, т.к. sentimentScore всегда в этих границах —
        // это делает график сопоставимым между месяцами (в отличие от авто-масштаба у StatsChartView).
        val minY = -1f
        val maxY = 1f

        fun xForDay(day: Int): Float = padL + chartW * (day - 1).toFloat() / (daysInMonth - 1).coerceAtLeast(1)
        fun yForScore(score: Float): Float = padT + chartH - ((score - minY) / (maxY - minY)) * chartH

        // Сетка + шкала Y (-1.0, -0.5, 0.0, 0.5, 1.0)
        val ySteps = listOf(-1f, -0.5f, 0f, 0.5f, 1f)
        for (v in ySteps) {
            val y = yForScore(v)
            canvas.drawLine(padL, y, padL + chartW, y, gridPaint)
            canvas.drawText(String.format("%.1f", v), padL - 6 * dp, y + 4 * dp, scalePaint)
        }

        // Нулевая линия — граница между позитивным и негативным настроением, выделена отдельно
        val zeroY = yForScore(0f)
        canvas.drawLine(padL, zeroY, padL + chartW, zeroY, zeroLinePaint)

        // Оси
        canvas.drawLine(padL, padT, padL, padT + chartH, axisPaint)
        canvas.drawLine(padL, padT + chartH, padL + chartW, padT + chartH, axisPaint)

        // Подписи дней (каждый 5-й, чтобы не наслаивались на 30-31 день)
        for (day in 1..daysInMonth step 5) {
            canvas.drawText(day.toString(), xForDay(day), padT + chartH + 20 * dp, labelPaint)
        }

        // Линия рисуется отдельными непрерывными сегментами — между соседними
        // днями С данными; разрыв (день без записи) не соединяется прямой,
        // чтобы не додумывать данные за пользователя.
        val segments = mutableListOf<MutableList<DayScore>>()
        var current = mutableListOf<DayScore>()
        for (point in data) {
            if (point.avgScore != null) {
                current.add(point)
            } else if (current.isNotEmpty()) {
                segments.add(current)
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) segments.add(current)

        for (segment in segments) {
            if (segment.size == 1) {
                // Единственная точка в сегменте — рисуем только маркер, линию не тянуть не из чего
                continue
            }
            val path = Path()
            val fillPath = Path()
            segment.forEachIndexed { i, point ->
                val x = xForDay(point.day)
                val y = yForScore(point.avgScore!!)
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, zeroY)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            val lastX = xForDay(segment.last().day)
            fillPath.lineTo(lastX, zeroY)
            fillPath.close()

            // Заливка под/над линией мягким градиентом цвета настроения
            fillPaint.shader = LinearGradient(
                0f, padT, 0f, padT + chartH,
                "#4034C759".toColorInt(), "#0034C759".toColorInt(),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, fillPaint)

            linePaint.color = "#34C759".toColorInt()
            canvas.drawPath(path, linePaint)
        }

        // Точки поверх линии — цвет по знаку значения (позитив/нейтраль/негатив)
        for (point in pointsWithData) {
            val x = xForDay(point.day)
            val y = yForScore(point.avgScore!!)
            pointPaint.color = colorForScore(point.avgScore)
            canvas.drawCircle(x, y, 5f * dp, pointPaint)
            canvas.drawCircle(x, y, 5f * dp, pointStrokePaint)
        }
    }

    private fun colorForScore(score: Float): Int = when {
        score >= 0.25f -> "#34C759".toColorInt()   // позитив — зелёный
        score > -0.25f -> "#FFB020".toColorInt()   // нейтрально — жёлтый
        else -> "#FF3B30".toColorInt()             // негатив — красный
    }
}
