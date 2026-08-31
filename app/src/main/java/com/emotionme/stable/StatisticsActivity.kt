package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class StatisticsActivity : AppCompatActivity() {

    private var currentMoodStats: List<StatItem> = emptyList()
    private var currentLocationStats: List<StatItem> = emptyList()
    private var currentWeatherStats: List<StatItem> = emptyList()
    private var currentDominantByDay: Map<Int, String> = emptyMap()
    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.applyLanguage(this)
        ThemeManager.applyTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)

        val db = AppDatabase.getInstance(this)
        val dao = db.moodDao()
        val userId = SessionManager.getUser(this)

        val moodChart = findViewById<StatsChartView>(R.id.moodChart)
        val sentimentTrendChart = findViewById<SentimentLineChartView>(R.id.sentimentTrendChart)
        val locationChart = findViewById<StatsChartView>(R.id.locationChart)
        val weatherChart = findViewById<StatsChartView>(R.id.weatherChart)
        val calendarView = findViewById<MoodCalendarView>(R.id.moodCalendar)
        val btnBackCharts = findViewById<ImageView>(R.id.btnBackCharts)
        val spinnerMonth = findViewById<Spinner>(R.id.spinnerMonth)
        val spinnerYear = findViewById<Spinner>(R.id.spinnerYear)
        val btnExport = findViewById<MaterialButton>(R.id.btnExportReport)
        val tvMood = findViewById<TextView>(R.id.moodTV)
        val tvSentiment = findViewById<TextView>(R.id.sentimentTrendTV)
        val tvPlace = findViewById<TextView>(R.id.locationTV)
        val tvWeather = findViewById<TextView>(R.id.weatherTV)
        val tvCalendar = findViewById<TextView>(R.id.calendarTV)

        startFloatingAnimation(findViewById(R.id.spot1), 5000)
        startFloatingAnimation(findViewById(R.id.spot2), 8000)
        startFloatingAnimation(findViewById(R.id.spot3), 10000)

        tvMood.setOnClickListener {
            Snackbar.make(
                tvMood,
                getString(R.string.info3),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        tvSentiment.setOnClickListener {
            Snackbar.make(
                tvSentiment,
                getString(R.string.info7),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        tvPlace.setOnClickListener {
            Snackbar.make(
                tvPlace,
                getString(R.string.info4),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        tvWeather.setOnClickListener {
            Snackbar.make(
                tvWeather,
                getString(R.string.info5),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        tvCalendar.setOnClickListener {
            Snackbar.make(
                tvCalendar,
                getString(R.string.info6),
                Snackbar.LENGTH_SHORT
            ).show()
        }

        val monthNames =
            (1..12).map {
                getString(
                    resources.getIdentifier(
                        "month$it",
                        "string",
                        packageName
                    )
                )
            }

        val now = Calendar.getInstance()
        currentYear = now.get(Calendar.YEAR)
        currentMonth = now.get(Calendar.MONTH)

        val years = (currentYear..2026).map { it.toString() }

        val monthAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            monthNames
        )
        monthAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerMonth.adapter = monthAdapter

        val yearAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            years
        )
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

                // Доминирующее настроение по дням — теперь берём moodKey
                val dominantByDay: Map<Int, String> = entries
                    .groupBy { entry ->
                        Calendar.getInstance()
                            .apply { timeInMillis = entry.timestamp }
                            .get(Calendar.DAY_OF_MONTH)
                    }
                    .mapValues { (_, dayEntries) ->
                        dayEntries
                            .groupingBy { it.moodKey }
                            .eachCount()
                            .maxByOrNull { it.value }?.key ?: ""
                    }

                // Тренд по тексту: среднее sentimentScore за каждый день месяца.
                // Дни без записей получают null (см. SentimentLineChartView — не рисуем
                // ложное значение "0.0", а просто разрываем линию).
                val daysInMonth = Calendar.getInstance()
                    .apply { set(currentYear, currentMonth, 1) }
                    .getActualMaximum(Calendar.DAY_OF_MONTH)

                val avgScoreByDay: Map<Int, Float> = entries
                    .groupBy { entry ->
                        Calendar.getInstance()
                            .apply { timeInMillis = entry.timestamp }
                            .get(Calendar.DAY_OF_MONTH)
                    }
                    .mapValues { (_, dayEntries) ->
                        dayEntries.map { it.sentimentScore }.average().toFloat()
                    }

                val sentimentTrend: List<SentimentLineChartView.DayScore> =
                    (1..daysInMonth).map { day ->
                        SentimentLineChartView.DayScore(day, avgScoreByDay[day])
                    }

                currentMoodStats = moodStats
                currentLocationStats = locationStats
                currentWeatherStats = weatherStats
                currentDominantByDay = dominantByDay

                runOnUiThread {
                    moodChart.setData(moodStats)
                    sentimentTrendChart.setData(sentimentTrend)
                    locationChart.setData(locationStats)
                    weatherChart.setData(weatherStats)
                    calendarView.setData(currentYear, currentMonth, dominantByDay)
                }
            }.start()
        }

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) =
                load()

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinnerMonth.onItemSelectedListener = spinnerListener
        spinnerYear.onItemSelectedListener = spinnerListener

        btnExport.setOnClickListener {
            btnExport.isEnabled = false
            btnExport.text = getString(R.string.generating)

            Thread {
                val user = db.userDao().getById(userId)
                val userName = user?.login ?: getString(R.string.username_default)

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
                    btnExport.text = getString(R.string.save_data)

                    if (fileName != null) {
                        Snackbar.make(
                            btnExport,
                            "${getString(R.string.finished)}: $fileName",
                            Snackbar.LENGTH_LONG
                        ).setAction(getString(R.string.share)) { shareImage(fileName) }.show()
                    } else {
                        Snackbar.make(
                            btnExport,
                            getString(R.string.error_saving_data),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
        }

        btnBackCharts.setOnClickListener { finish() }
    }

    private fun shareImage(fileName: String) {
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
                startActivity(Intent.createChooser(
                    intent,
                    getString(
                    R.string.share_data)))
            }
        }
    }

    fun startFloatingAnimation(view: View, duration: Long) {
        val animX = ObjectAnimator.ofFloat(
            view,
            "translationX",
            -150f, 150f
        )
            .apply {
                this.duration = duration; repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE; interpolator =
                AccelerateDecelerateInterpolator()
            }
        val animY = ObjectAnimator.ofFloat(
            view,
            "translationY",
            -150f, 150f
        )
            .apply {
                this.duration = duration + 800; repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE; interpolator =
                AccelerateDecelerateInterpolator()
            }
        AnimatorSet().apply { playTogether(animX, animY); start() }
    }
}