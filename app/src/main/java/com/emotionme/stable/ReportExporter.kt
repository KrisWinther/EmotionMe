@file:Suppress("DEPRECATION")

package com.emotionme.stable

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap

object ReportExporter {

    private val COLOR_BG = "#e8f7ff".toColorInt()
    private val COLOR_HEADER_BG = "#476fff".toColorInt()
    private const val COLOR_WHITE = Color.WHITE
    private val COLOR_DIVIDER = "#b7b6ec".toColorInt()
    private val COLOR_ACCENT = "#476fff".toColorInt()

    fun export(
        context: Context,
        userName: String,
        year: Int,
        month: Int,
        moodStats: List<StatItem>,
        locationStats: List<StatItem>,
        weatherStats: List<StatItem>,
        dominantByDay: Map<Int, String>
    ): String? {
        val bitmap = buildBitmap(
            context, userName, year, month,
            moodStats, locationStats, weatherStats, dominantByDay
        )
        return saveBitmapToGallery(context, bitmap, year, month)
    }

    private fun buildBitmap(
        context: Context,
        userName: String,
        year: Int,
        month: Int,
        moodStats: List<StatItem>,
        locationStats: List<StatItem>,
        weatherStats: List<StatItem>,
        dominantByDay: Map<Int, String>
    ): Bitmap {

        val dp = context.resources.displayMetrics.density
        val sp = context.resources.displayMetrics.scaledDensity

        val width = (420 * dp).toInt()
        val padding = (16 * dp).toInt()
        val headerHeight = (90 * dp).toInt()
        val chartHeightBig = (230 * dp).toInt() // настроение — крупный
        val chartHeightSmall = (200 * dp).toInt() // место + погода — меньше
        val labelHeight = (32 * dp).toInt()
        val gap = (12 * dp).toInt()
        val cardRadius = (20 * dp) // радиус карточек
        val footerHeight = (40 * dp).toInt()

        // Ширина нижней строки: место занимает ~55%, погода ~45%
        val rowW = width - padding * 2
        val locationW = (rowW * 0.55f).toInt()
        val weatherW = rowW - locationW - padding

        // Высота календаря
        val calW = width - padding * 2
        val cellSize = calW / 7f
        val calHeadH = (cellSize * 0.6f).toInt()
        val calHeight = (calHeadH + 6 * cellSize + 56 * dp).toInt()

        val totalH = headerHeight + gap +
                labelHeight + chartHeightBig + gap + // строка 1: настроение
                labelHeight + chartHeightSmall + gap +  // строка 2: место + погода
                labelHeight + calHeight + gap +  // строка 3: календарь
                footerHeight + padding

        val bitmap = createBitmap(width, totalH)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_BG)

        val paintCard = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_WHITE }
        val paintDiv = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER; style = Paint.Style.STROKE; strokeWidth = 1f * dp
        }
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT; textSize = 13f * sp
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT
        }

        // Шапка закруглена снизу с радиусом 20dp
        val headerRadius = 20f * dp
        val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_HEADER_BG }

        // Рисуем прямоугольник с полным скруглением, затем перекрываем верхние углы
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), headerHeight.toFloat()),
            headerRadius, headerRadius, paintHeader
        )
        // Верхние два угла прямые (перекрываем скругление)
        canvas.drawRect(0f, 0f, width.toFloat(), headerRadius, paintHeader)

        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE; textSize = 22f * sp
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#e8f7ff".toColorInt(); textSize = 14f * sp
            textAlign = Paint.Align.CENTER
        }

        val monthNames = arrayOf(
            "Январь", "Февраль", "Март", "Апрель",
            "Май", "Июнь", "Июль", "Август",
            "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )

        canvas.drawText(
            "📊 Отчёт за ${monthNames[month]} $year",
            width / 2f, headerHeight * 0.46f, paintTitle
        )
        canvas.drawText(userName, width / 2f, headerHeight * 0.75f, paintSub)

        var curY = headerHeight + gap

        // Вспомогательные функции рендера
        fun renderChart(items: List<StatItem>, w: Int, h: Int): Bitmap {
            val v = StatsChartView(context)
            v.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(
                    w,
                    android.view.View.MeasureSpec.EXACTLY
                ),
                android.view.View.MeasureSpec.makeMeasureSpec(
                    h,
                    android.view.View.MeasureSpec.EXACTLY
                )
            )
            v.layout(0, 0, w, h)
            v.setData(items)
            val bmp = createBitmap(w, h)
            v.draw(Canvas(bmp))
            return bmp
        }

        fun renderCalendar(w: Int, h: Int): Bitmap {
            val v = MoodCalendarView(context)
            v.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(
                    w,
                    android.view.View.MeasureSpec.EXACTLY
                ),
                android.view.View.MeasureSpec.makeMeasureSpec(
                    h,
                    android.view.View.MeasureSpec.EXACTLY
                )
            )
            v.layout(0, 0, w, h)
            v.setData(year, month, dominantByDay)
            val bmp = createBitmap(w, h)
            v.draw(Canvas(bmp))
            return bmp
        }

        fun drawCard(bmp: Bitmap, label: String, x: Int, y: Int) {
            val cardRect = RectF(
                x.toFloat(), y.toFloat(),
                (x + bmp.width).toFloat(), (y + labelHeight + bmp.height).toFloat()
            )
            canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paintCard)
            canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paintDiv)
            canvas.drawText(label, x + padding.toFloat(), y + labelHeight * 0.72f, paintLabel)
            canvas.drawBitmap(bmp, x.toFloat(), (y + labelHeight).toFloat(), null)
        }

        // Строка 1: Настроение на всю ширину, крупный график
        val bmpMood = renderChart(moodStats, width - padding * 2, chartHeightBig)
        drawCard(bmpMood, "Настроение", padding, curY)
        curY += labelHeight + chartHeightBig + gap

        // Строка 2: Место (55%) + Погода (45%) рядом
        val bmpLocation = renderChart(locationStats, locationW, chartHeightSmall)
        val bmpWeather = renderChart(weatherStats, weatherW, chartHeightSmall)

        drawCard(bmpLocation, "Место", padding, curY)
        drawCard(bmpWeather, "Погода", padding + locationW + padding, curY)
        curY += labelHeight + chartHeightSmall + gap

        // Строка 3: Календарь
        val bmpCal = renderCalendar(width - padding * 2, calHeight)
        drawCard(bmpCal, "Календарь настроения", padding, curY)
        curY += labelHeight + calHeight + gap

        // Подпись внизу
        val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#000000".toColorInt()
            textSize = 12f * sp
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText(
            "Сгенерировано в EmotionMe ❤️",
            width / 2f, curY + footerHeight * 0.6f, paintFooter
        )

        return bitmap
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        year: Int,
        month: Int
    ): String? {
        val monthStr = (month + 1).toString().padStart(2, '0')
        val fileName = "EmotionMe_${year}_${monthStr}.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EmotionMe")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        var stream: OutputStream? = null
        return try {
            stream = resolver.openOutputStream(uri)!!
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            fileName
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        } finally {
            stream?.close()
            bitmap.recycle()
        }
    }
}
