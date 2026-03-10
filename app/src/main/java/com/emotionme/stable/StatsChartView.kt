package com.emotionme.stable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.max

@Suppress("DEPRECATION")
class StatsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    val spSize = 15f
    private var data: List<StatItem> = emptyList()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = spSize * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }

    fun setData(items: List<StatItem>) {
        data = items
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxValue = max(1, data.maxOf { it.count })
        val barWidth = width / (data.size * 2f)
        val bottom = height - 60f

        data.forEachIndexed { index, item ->

            barPaint.color = getColorForItem(item.label)

            val left = index * 2 * barWidth + barWidth / 2
            val barHeight = (item.count / maxValue.toFloat()) * height * 0.65f
            val top = bottom - barHeight

            // столбец
            canvas.drawRect(
                left,
                top,
                left + barWidth,
                bottom,
                barPaint
            )

            // значение над столбцом
            canvas.drawText(
                item.count.toString(),
                left + barWidth / 2,
                top - 25,
                textPaint
            )

            // подпись снизу
            canvas.drawText(
                item.label,
                left + barWidth / 2,
                height.toFloat() - 15,
                textPaint
            )
        }
    }

    private fun getColorForItem(label: String): Int {
        return when {
            label.contains("😡") || label.contains("Злость", ignoreCase = true) ->
                "#E53935".toColorInt()

            label.contains("😢") || label.contains("Грусть", ignoreCase = true) ->
                "#1E88E5".toColorInt()

            label.contains("😐") || label.contains("Нейтрально", ignoreCase = true) ->
                "#FDD835".toColorInt()

            label.contains("😊") || label.contains("Радость", ignoreCase = true) ->
                "#43A047".toColorInt()

            else ->
                "#5680E9".toColorInt()
        }
    }
}