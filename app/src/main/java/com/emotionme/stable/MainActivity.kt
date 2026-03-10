package com.emotionme.stable

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()

        val db = AppDatabase.getInstance(this)
        val userId = SessionManager.getUser(this)

        if (userId == 0L) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val spinnerMood = findViewById<Spinner>(R.id.spinnerMood)
        val spinnerPlace = findViewById<Spinner>(R.id.spinnerPlace)
        val spinnerWeather = findViewById<Spinner>(R.id.spinnerWeather)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnStats = findViewById<Button>(R.id.btnStats)
        val btnNotes = findViewById<Button>(R.id.btnNotes)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val showMotivation = findViewById<TextView>(R.id.showMotivation)

        Thread {
            val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            val stats = db.moodDao().getStatsFrom(userId, dayAgo)
            val good = stats.filter { it.mood.contains("😊") }.sumOf { it.count }
            val bad = stats.filter { it.mood.contains("😢") || it.mood.contains("😡") }
                .sumOf { it.count }
            val message = if (bad > good) {
                listOf(
                    "Ты справишься!",
                    "Не сдавайся, ты не один.",
                    "Даже трудные дни проходят.",
                    "Не переживай, работа принесёт свои плоды."
                ).random()
            } else {
                listOf(
                    "Превосходный день, так держать! ❤\uFE0F",
                    "Хорошо идём, пусть дальше будет также хорошо, а то и лучше! \uD83E\uDD1E",
                    "Пусть завтрашний день будет таким же хорошим как сегодняшний. \uD83E\uDD29"
                ).random()
            }
            runOnUiThread { showMotivation.text = message }
        }.start()

        val moodAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            listOf(
                "😊 Радость",
                "😐 Нейтрально",
                "😢 Грусть",
                "😡 Злость"
            )
        )
        moodAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerMood.adapter = moodAdapter

        val placeAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            listOf(
                "🏡 Дом",
                "\uD83D\uDCDA Учёба | \uD83D\uDCBC Работа",
                "⛺ Прогулка"
            )
        )
        placeAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerPlace.adapter = placeAdapter

        val weatherAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            listOf(
                "☀\uFE0F Ясно",
                "⛅ Пасмурно",
                "⛈\uFE0F Непогода"
            )
        )
        weatherAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerWeather.adapter = weatherAdapter

        btnSave.setOnClickListener {

            val entry = MoodEntry(
                userId = userId,
                mood = spinnerMood.selectedItem.toString(),
                location = spinnerPlace.selectedItem.toString(),
                note = etNote.text.toString(),
                timestamp = System.currentTimeMillis(),
                weather = spinnerWeather.selectedItem.toString()
            )

            Thread {
                db.moodDao().insert(entry)

                runOnUiThread {
                    Toast.makeText(this, "Запись сохранена ✅", Toast.LENGTH_SHORT).show()
                    etNote.text.clear()
                }
            }.start()
        }
        btnStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java)) }
        btnNotes.setOnClickListener {
            startActivity(Intent(this, NotesActivity::class.java)) }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
            }
        }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }
}