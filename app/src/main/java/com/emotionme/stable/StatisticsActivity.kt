package com.emotionme.stable

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StatisticsActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val db = AppDatabase.getInstance(this)
        val dao = db.moodDao()

        val userId = SessionManager.getUser(this)

        val moodChart = findViewById<StatsChartView>(R.id.moodChart)
        val locationChart = findViewById<StatsChartView>(R.id.locationChart)
        val weatherChart = findViewById<StatsChartView>(R.id.weatherChart)
        val btnBackCharts = findViewById<TextView>(R.id.btnBackCharts)
        val rg = findViewById<RadioGroup>(R.id.rgPeriod)

        fun load(from: Long) {
            Thread {
                val moodStats = dao.getMoodStatsFrom(userId, from)
                val locationStats = dao.getLocationStatsFrom(userId, from)
                val weatherStats = dao.getWeatherStatsFrom(userId, from)

                runOnUiThread {
                    moodChart.setData(moodStats)
                    locationChart.setData(locationStats)
                    weatherChart.setData(weatherStats)
                }
            }.start()
        }

        rg.setOnCheckedChangeListener { _, id ->
            val now = System.currentTimeMillis()
            when (id) {
                R.id.rbDay -> load(now - 24 * 60 * 60 * 1000L)
                R.id.rbWeek -> load(now - 7 * 24 * 60 * 60 * 1000L)
                R.id.rbMonth -> load(now - 30 * 24 * 60 * 60 * 1000L)
            }
        }

        rg.check(R.id.rbDay)

        btnBackCharts.setOnClickListener {
            finish()
        }
    }
}