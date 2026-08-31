package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class AnswersActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_answers)

        startFloatingAnimation(findViewById(R.id.spot1), 5000)
        startFloatingAnimation(findViewById(R.id.spot2), 8000)
        startFloatingAnimation(findViewById(R.id.spot3), 10000)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        loadData()

        // Кнопка «Пройти анкету заново» перенесена сюда из SettingsActivity
        findViewById<MaterialButton>(R.id.btnOnboarding).setOnClickListener {
            val userId = SessionManager.getUser(this)
            val db = AppDatabase.getInstance(this)
            Thread {
                db.userDao().resetOnboarding(userId)
                runOnUiThread {
                    startActivity(Intent(
                        this,
                        OnboardingActivity::class.java)
                    )
                    finish()
                }
            }.start()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadData() {
        val userId = SessionManager.getUser(this)
        val db = AppDatabase.getInstance(this)

        executor.execute {
            val user = db.userDao().getById(userId)
            val firstTs = db.moodDao().getFirstEntryTimestamp(userId)
            val activeDays = db.moodDao().getActiveDaysCount(userId)

            runOnUiThread {
                if (user == null) return@runOnUiThread

                val noData = getString(R.string.answers_no_data)

                // Имя
                findViewById<TextView>(R.id.tvName).text =
                    user.displayName.ifBlank { noData }

                // Цель
                findViewById<TextView>(R.id.tvGoal).text =
                    user.goal.ifBlank { noData }

                // Частота
                findViewById<TextView>(R.id.tvFrequency).text =
                    user.entryFrequency.ifBlank { noData }

                // Симптомы — отдельными строчками
                val symptomsGroup = findViewById<LinearLayout>(R.id.symptomsChipGroup)
                symptomsGroup.removeAllViews()
                val symptoms = OnboardingManager.symptomsToList(user.symptoms)
                if (symptoms.isEmpty()) {
                    val tv = TextView(this).apply {
                        text = noData
                        textSize = 15f
                        setTextColor(getColor(R.color.black))
                    }
                    symptomsGroup.addView(tv)
                } else {
                    symptoms.forEach { symptom ->
                        val tv = TextView(this).apply {
                            text = symptom
                            textSize = 15f
                            setTextColor(getColor(R.color.black))
                            setPadding(0, 4, 0, 4)
                        }
                        symptomsGroup.addView(tv)
                    }
                }

                // Дата начала записей / аккаунта
                if (firstTs != null && firstTs > 0L) {
                    val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    "${getString(R.string.answers_started_label)}: ${fmt.format(Date(firstTs))}"
                } else {
                    "${getString(R.string.answers_started_label)}: $noData"
                }
                findViewById<TextView>(R.id.tvStartedDate).text =
                    if (firstTs != null && firstTs > 0L) {
                        val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        fmt.format(Date(firstTs))
                    } else noData

                // Счётчик активных дней
                val daysSuffix = getString(R.string.answers_days_suffix)
                findViewById<TextView>(R.id.tvActiveDays).text =
                    "$activeDays $daysSuffix"
            }
        }
    }

    private fun startFloatingAnimation(view: View, duration: Long) {
        val animX = ObjectAnimator.ofFloat(
            view,
            "translationX",
            -150f, 150f
        )
            .apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val animY = ObjectAnimator.ofFloat(
            view,
            "translationY",
            -150f, 150f
        )
            .apply {
            this.duration = duration + 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        AnimatorSet().apply { playTogether(
            animX, animY)
            start()
        }
    }
}
