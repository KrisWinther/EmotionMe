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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private var doubleClick: Long = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()


        val db = AppDatabase.getInstance(this)
        val userId = SessionManager.getUser(this)

        if (userId == 0L) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val greets = findViewById<TextView>(R.id.greets)
        val spinnerMood = findViewById<Spinner>(R.id.spinnerMood)
        val spinnerPlace = findViewById<Spinner>(R.id.spinnerPlace)
        val spinnerWeather = findViewById<Spinner>(R.id.spinnerWeather)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnStats = findViewById<MaterialButton>(R.id.btnStats)
        val btnNotes = findViewById<MaterialButton>(R.id.btnNotes)
        val btnSettings = findViewById<TextView>(R.id.btnSettings)
        val showMotivation = findViewById<TextView>(R.id.showMotivation)
        val scrollView = findViewById<ScrollView>(R.id.scroll)
        val mainTV = findViewById<TextView>(R.id.mainTV)

        mainTV.setOnClickListener {
            Snackbar.make(mainTV,
                "Как насчёт того, чтобы сделать запись о своём настроении?",
                Snackbar.LENGTH_SHORT)
                .show()
        }

        etNote.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        Thread {
            val user = db.userDao().getById(userId)
            val name = user?.login
            val greeting = listOf(
                "Доброго дня, $name! ☀\uFE0F",
                "Как проходит день, $name?",
                "Привет, $name! \uD83D\uDC4B",
                "Добро пожаловать, $name!",
                "Рады видеть тебя, $name! \uD83D\uDE07",
                "Как настроение, $name?",
                "$name! А вот и ты! \uD83D\uDE0A"
            ).random()

            runOnUiThread {
                greets.text = greeting
            }
        }.start()

        Thread {
            val user = db.userDao().getById(userId)
            val name = user?.login
            val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            val stats = db.moodDao().getStatsFrom(userId, dayAgo)
            val tired = stats.filter{ it.mood.contains("\uD83D\uDE34") }.sumOf { it.count }
            val good = stats.filter { it.mood.contains("😊") ||
                    it.mood.contains("\uD83E\uDD70") ||
                    it.mood.contains("\uD83D\uDE07") }.sumOf { it.count }
            val bad = stats.filter {
                it.mood.contains("😢") || it.mood.contains("😡") }.sumOf { it.count }
            val message = when {
                tired > bad && tired > good -> listOf(
                    "Усталость - это временно, отдых — это святое! 🛌",
                    "Обязательно хорошенько отдохни, $name",
                    "Если чувствуешь усталость - отдохни, не нагружай себя",
                    "Усталость - помеха хорошей работе, не переусердствуй, $name"
                ).random()
                bad > good -> listOf(
                    "Не сдавайся, ты не один",
                    "Даже трудные дни проходят",
                    "Не переживай, работа принесёт свои плоды"
                ).random()
                else -> listOf(
                    "Превосходный день, так держать! ❤\uFE0F",
                    "Хорошо идём, пусть дальше будет также хорошо, а то и лучше! \uD83D\uDE09",
                    "Пусть завтрашний день будет таким же хорошим как сегодняшний \uD83E\uDD29"
                ).random()
            }

            runOnUiThread { showMotivation.text = message }
        }.start()

        val moodAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            listOf(
                "\uD83E\uDD70 Счастье",
                "😊 Радость",
                "\uD83D\uDE07 Умиротворение",
                "😐 Нейтрально",
                "\uD83D\uDE34 Усталость",
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
                "🏡 Дома",
                "\uD83D\uDCDA На учёбе | \uD83D\uDCBC На работе",
                "⛺ На прогулке",
                "\uD83D\uDEB6 В пути | \uD83D\uDE99 В транспорте",
                "\uD83E\uDD42 В гостях",
                "\uD83C\uDF5D В ресторане | ☕ В кафе",
                "\uD83D\uDCB3 В магазине",
                "\uD83D\uDDFA\uFE0F В путешествии",
                "\uD83C\uDF89 На вечеринке"
            )
        )
        placeAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerPlace.adapter = placeAdapter

        val weatherAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            listOf(
                "☀\uFE0F Солнечно",
                "⛅ Пасмурно",
                "☁\uFE0F Облачно",
                "⛈\uFE0F Непогода"
            )
        )
        weatherAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerWeather.adapter = weatherAdapter

        btnSave.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - doubleClick < 2000){
                performAction()
                doubleClick = 0 }
            else {
                doubleClick = currentTime
                Snackbar.make(
                    btnSave,
                    "Нажми ещё раз, чтобы сохранить запись",
                    Snackbar.LENGTH_SHORT)
                    .setDuration(2000)
                    .show()
            }
        }
        btnStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java)) }
        btnNotes.setOnClickListener {
            startActivity(Intent(this, NotesActivity::class.java)) }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted ->
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
    private fun performAction() {

        val db = AppDatabase.getInstance(this)
        val userId = SessionManager.getUser(this)

        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val spinnerMood = findViewById<Spinner>(R.id.spinnerMood)
        val spinnerPlace = findViewById<Spinner>(R.id.spinnerPlace)
        val spinnerWeather = findViewById<Spinner>(R.id.spinnerWeather)

        val data = MoodEntry(
            userId = userId,
            mood = spinnerMood.selectedItem.toString(),
            location = spinnerPlace.selectedItem.toString(),
            note = etNote.text.toString(),
            timestamp = System.currentTimeMillis(),
            weather = spinnerWeather.selectedItem.toString()
        )
        Thread {
            db.moodDao().insert(data)

            runOnUiThread {
                Snackbar.make(
                    btnSave,
                    "Запись сохранена ✅",
                    Snackbar.LENGTH_SHORT)
                    .show()
                etNote.text.clear()
            }
        }.start()
    }
}