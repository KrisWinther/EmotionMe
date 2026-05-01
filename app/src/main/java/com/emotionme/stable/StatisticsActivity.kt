package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class StatisticsActivity : AppCompatActivity() {

    // Текущие данные — нужны для передачи в экспортер
    private var currentMoodStats: List<StatItem> = emptyList()
    private var currentLocationStats: List<StatItem> = emptyList()
    private var currentWeatherStats: List<StatItem> = emptyList()
    private var currentDominantByDay: Map<Int, String> = emptyMap()
    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val db = AppDatabase.getInstance(this)
        val dao = db.moodDao()
        val userId = SessionManager.getUser(this)

        val moodChart = findViewById<StatsChartView>(R.id.moodChart)
        val locationChart = findViewById<StatsChartView>(R.id.locationChart)
        val weatherChart = findViewById<StatsChartView>(R.id.weatherChart)
        val calendarView = findViewById<MoodCalendarView>(R.id.moodCalendar)
        val btnBackCharts = findViewById<TextView>(R.id.btnBackCharts)
        val spinnerMonth = findViewById<Spinner>(R.id.spinnerMonth)
        val spinnerYear = findViewById<Spinner>(R.id.spinnerYear)
        val btnExport = findViewById<MaterialButton>(R.id.btnExportReport)
        val tvMood = findViewById<TextView>(R.id.moodTV)
        val tvPlace = findViewById<TextView>(R.id.locationTV)
        val tvWeather = findViewById<TextView>(R.id.weatherTV)
        val tvCalendar = findViewById<TextView>(R.id.calendarTV)
        val spot1 = findViewById<View>(R.id.spot1)
        val spot2 = findViewById<View>(R.id.spot2)
        val spot3 = findViewById<View>(R.id.spot3)

        startFloatingAnimation(spot1, 5000)
        startFloatingAnimation(spot2, 8000)
        startFloatingAnimation(spot3, 10000)

        tvMood.setOnClickListener {
            Snackbar.make(
                tvMood,
                "Здесь отображается настроение, которое ты отметил(-а) \uD83D\uDE0D",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        tvPlace.setOnClickListener {
            Snackbar.make(
                tvPlace,
                "Здесь отображаются места, где ты чаще всего бывал(-а) в этом месяце \uD83C\uDFDE\uFE0F",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        tvWeather.setOnClickListener {
            Snackbar.make(
                tvWeather,
                "Здесь отображается погода, которую ты чаще всего наблюдал(-а) в этом месяце ☀\uFE0F",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        tvCalendar.setOnClickListener {
            Snackbar.make(
                tvCalendar,
                "Это календарь настроения, каждый месяц на нём появляется уникальный рисунок! \uD83D\uDCC5",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель",
            "Май", "Июнь", "Июль", "Август",
            "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )

        val now = Calendar.getInstance()
        currentYear = now.get(Calendar.YEAR)
        currentMonth = now.get(Calendar.MONTH)

        val years = (2020..currentYear).map { it.toString() }

        val monthAdapter = ArrayAdapter(this, R.layout.item_spinner, monthNames)
        monthAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerMonth.adapter = monthAdapter

        val yearAdapter = ArrayAdapter(this, R.layout.item_spinner, years)
        yearAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerYear.adapter = yearAdapter

        spinnerMonth.setSelection(currentMonth)
        spinnerYear.setSelection(years.indexOf(currentYear.toString()))

        fun load() {
            currentMonth = spinnerMonth.selectedItemPosition
            currentYear = spinnerYear.selectedItem.toString().toInt()

            val from = Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val to = Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, 1)
            }.timeInMillis

            Thread {
                val moodStats = dao.getMoodStatsRange(userId, from, to)
                val locationStats = dao.getLocationStatsRange(userId, from, to)
                val weatherStats = dao.getWeatherStatsRange(userId, from, to)
                val entries = dao.getEntriesRange(userId, from, to)

                val dominantByDay: Map<Int, String> = entries
                    .groupBy { entry ->
                        Calendar.getInstance().apply {
                            timeInMillis = entry.timestamp
                        }.get(Calendar.DAY_OF_MONTH)
                    }
                    .mapValues { (_, dayEntries) ->
                        dayEntries.groupingBy { it.mood }.eachCount()
                            .maxByOrNull { it.value }?.key ?: ""
                    }

                // Сохраняем для экспорта
                currentMoodStats = moodStats
                currentLocationStats = locationStats
                currentWeatherStats = weatherStats
                currentDominantByDay = dominantByDay

                runOnUiThread {
                    moodChart.setData(moodStats)
                    locationChart.setData(locationStats)
                    weatherChart.setData(weatherStats)
                    calendarView.setData(currentYear, currentMonth, dominantByDay)
                }
            }.start()
        }

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                load()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerMonth.onItemSelectedListener = spinnerListener
        spinnerYear.onItemSelectedListener = spinnerListener

        // Экспорт в PNG
        btnExport.setOnClickListener {
            btnExport.isEnabled = false
            btnExport.text = "Генерация..."

            Thread {
                // Получаем имя пользователя
                val user = db.userDao().getById(userId)
                val userName = user?.login ?: "Пользователь"

                val fileName = ReportExporter.export(
                    context = this,
                    userName = userName,
                    year = currentYear,
                    month = currentMonth,
                    moodStats = currentMoodStats,
                    locationStats = currentLocationStats,
                    weatherStats = currentWeatherStats,
                    dominantByDay = currentDominantByDay
                )

                runOnUiThread {
                    btnExport.isEnabled = true
                    btnExport.text = "\uD83D\uDCF8 Сохранить отчёт"

                    if (fileName != null) {
                        Snackbar.make(
                            btnExport,
                            "Готово: $fileName",
                            Snackbar.LENGTH_LONG
                        ).setAction("Поделиться") {
                            shareImage(fileName)
                        }.show()
                    } else {
                        Snackbar.make(
                            btnExport,
                            "Ошибка при сохранении 😞",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
        }

        btnBackCharts.setOnClickListener { finish() }
    }

    private fun shareImage(fileName: String) {
        val uri: Uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            .buildUpon()
            .appendPath(fileName)
            .build()

        // Ищем файл через MediaStore и делимся им
        val cursor = contentResolver.query(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(android.provider.MediaStore.Images.Media._ID),
            "${android.provider.MediaStore.Images.Media.DISPLAY_NAME} = ?",
            arrayOf(fileName),
            "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(0)
                val shareUri = android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Поделиться отчётом"))
            }
        }
    }

    fun startFloatingAnimation(view: View, duration: Long) {
        val animX = ObjectAnimator.ofFloat(view, "translationX", -150f, 150f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val animY = ObjectAnimator.ofFloat(view, "translationY", -150f, 150f).apply {
            this.duration = duration + 800 // Разная скорость для естественности
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(animX, animY)
            start()
        }
    }
}