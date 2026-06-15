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
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private var doubleClick: Long = 0

    // Параллельные списки: отображаемые строки -> стабильные ключи
    private val moodKeys = listOf(
        MoodKeys.MOOD_HAPPY,
        MoodKeys.MOOD_JOY,
        MoodKeys.MOOD_CALM,
        MoodKeys.MOOD_NEUTRAL,
        MoodKeys.MOOD_TIRED,
        MoodKeys.MOOD_SAD,
        MoodKeys.MOOD_ANGRY
    )
    private val placeKeys = listOf(
        MoodKeys.PLACE_HOME,
        MoodKeys.PLACE_WORK,
        MoodKeys.PLACE_WALK,
        MoodKeys.PLACE_ON_WAY,
        MoodKeys.PLACE_GUEST,
        MoodKeys.PLACE_CAFE,
        MoodKeys.PLACE_SHOPPING,
        MoodKeys.PLACE_TRAVEL,
        MoodKeys.PLACE_PARTY
    )
    private val weatherKeys = listOf(
        MoodKeys.WEATHER_CLEAR,
        MoodKeys.WEATHER_CLOUDY1,
        MoodKeys.WEATHER_CLOUDY2,
        MoodKeys.WEATHER_BAD
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
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

        startFloatingAnimation(findViewById(R.id.spot1), 5000)
        startFloatingAnimation(findViewById(R.id.spot2), 7000)
        startFloatingAnimation(findViewById(R.id.spot3), 9000)

        mainTV.setOnClickListener {
            Snackbar.make(
                mainTV,
                getString(R.string.info2),
                Snackbar.LENGTH_SHORT)
                .show()
        }

        etNote.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Приветствие
        Thread {
            val user = db.userDao().getById(userId)
            val displayName = user?.displayName?.ifBlank { user.login } ?: ""
            val greeting = listOf(
                "${getString(R.string.greets1)}, $displayName? 👋",
                "${getString(R.string.greets2)}, $displayName! 👋",
                "${getString(R.string.greets3)}, $displayName?",
                "${getString(R.string.greets4)}, $displayName! 😇",
                "${getString(R.string.greets5)}, $displayName?",
                "$displayName! ${getString(R.string.greets6)} 😊"
            ).random()
            runOnUiThread { greets.text = greeting }
        }.start()

        // Мотивация (теперь сравниваем по KEY, не по локализованному тексту)
        Thread {
            val user = db.userDao().getById(userId)
            val displayName = user?.displayName?.ifBlank { user.login } ?: ""
            val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            val stats = db.moodDao().getStatsFrom(userId, dayAgo)

            val tired = stats.filter { it.mood == MoodKeys.MOOD_TIRED }.sumOf { it.count }
            val good = stats.filter {
                it.mood == MoodKeys.MOOD_HAPPY ||
                        it.mood == MoodKeys.MOOD_JOY ||
                        it.mood == MoodKeys.MOOD_CALM
            }.sumOf { it.count }
            val bad = stats.filter {
                it.mood == MoodKeys.MOOD_SAD ||
                        it.mood == MoodKeys.MOOD_ANGRY
            }.sumOf { it.count }

            val message = when {
                tired > bad && tired > good -> listOf(
                    getString(R.string.motiv_tired_1),
                    "${getString(R.string.motiv_bad_2)}, $displayName",
                    getString(R.string.motiv_tired_3),
                    "${getString(R.string.motiv_tired_4)}, $displayName"
                ).random()

                bad > good -> listOf(
                    "${getString(R.string.motiv_bad_1)}, $displayName",
                    getString(R.string.motiv_bad_2),
                    "${getString(R.string.motiv_bad_3)}, $displayName",
                    getString(R.string.motiv_bad_4)
                ).random()

                else -> listOf(
                    "${getString(R.string.motiv_good_1)}, $displayName! ❤️",
                    getString(R.string.motiv_good_2),
                    getString(R.string.motiv_good_3)
                ).random()
            }
            runOnUiThread { showMotivation.text = message }
        }.start()

        // Спиннеры — показываем локализованные строки
        val moodLabels = moodKeys.map {
            getString(MoodKeys.moodResId(it))
        }
        val moodAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            moodLabels
        )
        moodAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerMood.adapter = moodAdapter

        val placeLabels = placeKeys.map { getString(MoodKeys.placeResId(it)) }
        val placeAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            placeLabels
        )
        placeAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerPlace.adapter = placeAdapter

        val weatherLabels = weatherKeys.map { getString(MoodKeys.weatherResId(it)) }
        val weatherAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner,
            weatherLabels
        )
        weatherAdapter.setDropDownViewResource(R.layout.item_spinner)
        spinnerWeather.adapter = weatherAdapter

        // Сохранение
        btnSave.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - doubleClick < 2000) {
                performSave(spinnerMood, spinnerPlace, spinnerWeather, etNote, btnSave)
                doubleClick = 0
            } else {
                doubleClick = now
                Snackbar.make(
                    btnSave,
                    R.string.btn_save_confirm,
                    Snackbar.LENGTH_SHORT
                )
                    .setDuration(2000)
                    .show()
            }
        }

        btnStats.setOnClickListener { startActivity(Intent(
            this,
            StatisticsActivity::class.java)
        ) }
        btnNotes.setOnClickListener { startActivity(Intent(
            this,
            NotesActivity::class.java)
        ) }
        btnSettings.setOnClickListener { startActivity(Intent(this,
            SettingsActivity::class.java)
        ) }
    }

    private fun performSave(
        spinnerMood: Spinner,
        spinnerPlace: Spinner,
        spinnerWeather: Spinner,
        etNote: EditText,
        btnSave: MaterialButton
    ) {
        val db = AppDatabase.getInstance(this)
        val userId = SessionManager.getUser(this)

        // Выбранный индекс -> ключ (стабильный) + локализованная строка (для отображения)
        val moodIdx = spinnerMood.selectedItemPosition
        val placeIdx = spinnerPlace.selectedItemPosition
        val weatherIdx = spinnerWeather.selectedItemPosition

        val moodKey = moodKeys[moodIdx]
        val placeKey = placeKeys[placeIdx]
        val weatherKey = weatherKeys[weatherIdx]

        val moodLabel = spinnerMood.selectedItem.toString()
        val placeLabel = spinnerPlace.selectedItem.toString()
        val weatherLabel = spinnerWeather.selectedItem.toString()

        val entry = MoodEntry(
            userId = userId,
            mood = moodLabel,     // локализованная строка (для отображения в Notes)
            location = placeLabel,
            weather = weatherLabel,
            note = etNote.text.toString(),
            timestamp = System.currentTimeMillis(),
            moodKey = moodKey,       // стабильный ключ (для статистики)
            locationKey = placeKey,
            weatherKey = weatherKey
        )

        Thread {
            db.moodDao().insert(entry)
            runOnUiThread {
                Snackbar.make(btnSave, R.string.saved, Snackbar.LENGTH_SHORT).show()
                etNote.text.clear()
            }
        }.start()
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            this.duration = duration + 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        AnimatorSet().apply { playTogether(animX, animY); start() }
    }
}
