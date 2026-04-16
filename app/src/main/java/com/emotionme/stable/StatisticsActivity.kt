package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class StatisticsActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        val moodTV = findViewById<TextView>(R.id.moodTV)
        val locationTV = findViewById<TextView>(R.id.locationTV)
        val weatherTV = findViewById<TextView>(R.id.weatherTV)
        val calendarTV = findViewById<TextView>(R.id.calendarTV)
        val spot1 = findViewById<View>(R.id.spot1)
        val spot2 = findViewById<View>(R.id.spot2)
        val spot3 = findViewById<View>(R.id.spot3)

        startFloatingAnimation(spot1, 7000)
        startFloatingAnimation(spot2, 9000)
        startFloatingAnimation(spot3, 12000)

        moodTV.setOnClickListener {
            Snackbar.make(
                moodTV,
                "Здесь отображается твоё настроение за месяц \uD83D\uDE0B",
                Snackbar.LENGTH_SHORT)
                .show()
        }

        locationTV.setOnClickListener {
            Snackbar.make(
                locationTV,
                "Здесь отображаются места, где ты проводил время \uD83E\uDDED",
                Snackbar.LENGTH_SHORT)
                .show()
        }

        weatherTV.setOnClickListener {
            Snackbar.make(
                weatherTV,
                "Здесь отображается погода, которую ты наблюдал" +
                        "в моментах \uD83C\uDF24\uFE0F",
                Snackbar.LENGTH_SHORT)
                .show()
        }

        calendarTV.setOnClickListener {
            Snackbar.make(calendarTV,
                "Это календарь настроения, каждый месяц гарантирует уникальность рисунка" +
                        "\uD83D\uDCC5",
                Snackbar.LENGTH_SHORT)
                .show()
        }

        val monthNames = listOf(
            "Январь",
            "Февраль",
            "Март",
            "Апрель",
            "Май",
            "Июнь",
            "Июль",
            "Август",
            "Сентябрь",
            "Октябрь",
            "Ноябрь",
            "Декабрь"
        )

        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        val years = (2026..currentYear).map { it.toString() }

        val monthAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            monthNames)
        monthAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerMonth.adapter = monthAdapter

        val yearAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            years)
        yearAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerYear.adapter = yearAdapter

        spinnerMonth.setSelection(currentMonth)
        spinnerYear.setSelection(years.indexOf(currentYear.toString()))

        fun load() {
            val selectedMonth = spinnerMonth.selectedItemPosition
            val selectedYear = spinnerYear.selectedItem.toString().toInt()

            val from = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val to = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, 1)
            }.timeInMillis

            Thread {
                val moodStats = dao.getMoodStatsRange(userId, from, to)
                val locationStats = dao.getLocationStatsRange(userId, from, to)
                val weatherStats = dao.getWeatherStatsRange(userId, from, to)
                val entries = dao.getEntriesRange(userId, from, to)

                // Группируем записи по дню, находим доминирующую эмоцию
                val dominantByDay: Map<Int, String> = entries
                    .groupBy { entry ->
                        Calendar.getInstance().apply {
                            timeInMillis = entry.timestamp
                        }.get(Calendar.DAY_OF_MONTH)
                    }
                    .mapValues { (_, dayEntries) ->
                        dayEntries
                            .groupingBy { it.mood }
                            .eachCount()
                            .maxByOrNull { it.value }
                            ?.key ?: ""
                    }

                runOnUiThread {
                    moodChart.setData(moodStats)
                    locationChart.setData(locationStats)
                    weatherChart.setData(weatherStats)
                    calendarView.setData(selectedYear, selectedMonth, dominantByDay)
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

        btnBackCharts.setOnClickListener {
            finish()
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
            this.duration = duration + 1000 // Разная скорость для естественности
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