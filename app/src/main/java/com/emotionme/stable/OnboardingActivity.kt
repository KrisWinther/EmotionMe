package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class OnboardingActivity : AppCompatActivity() {

    private val TOTAL_STEPS = 4
    private var currentStep = 0

    private var displayName = ""
    private val selectedSymptoms = mutableSetOf<String>()
    private var selectedGoal = ""
    private var selectedFrequency = ""

    private var userId = 0L
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        userId = SessionManager.getUser(this)

        startFloatingAnimation(findViewById(R.id.spot1), 5000)
        startFloatingAnimation(findViewById(R.id.spot2), 7500)
        startFloatingAnimation(findViewById(R.id.spot3), 10000)

        showStep(0)
    }

    // Навигация
    private fun showStep(step: Int) {
        currentStep = step

        listOf(
            R.id.stepName to 0,
            R.id.stepSymptoms to 1,
            R.id.stepGoal to 2,
            R.id.stepFrequency to 3
        ).forEach { (viewId, idx) ->
            findViewById<View>(viewId).visibility =
                if (idx == step) View.VISIBLE else View.GONE
        }

        when (step) {
            0 -> setupStepName()
            1 -> setupStepSymptoms()
            2 -> setupStepGoal()
            3 -> setupStepFrequency()
        }
    }

    private fun goBack() {
        if (currentStep > 0) showStep(currentStep - 1)
    }

    // Шаг 0: Имя

    private fun setupStepName() {
        val etName = findViewById<EditText>(R.id.etName)
        val btnNext = findViewById<MaterialButton>(R.id.btnNextName)

        if (displayName.isNotBlank()) etName.setText(displayName)

        btnNext.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                snack(btnNext, getString(R.string.onboarding_name_error))
                return@setOnClickListener
            }
            displayName = name
            showStep(1)
        }
    }

    // Шаг 1: Симптомы

    private fun setupStepSymptoms() {
        val container = findViewById<LinearLayout>(R.id.symptomsContainer)
        val btnNext = findViewById<MaterialButton>(R.id.btnNextSymptoms)
        val btnBack = findViewById<ImageButton>(R.id.btnBack1)

        btnBack.setOnClickListener { goBack() }

        container.removeAllViews()
        OnboardingManager.symptomOptions(this).forEach { symptom ->
            val cb = CheckBox(this).apply {
                text = symptom
                textSize = 15f
                setTextColor(getColor(R.color.black))
                setPadding(8, 12, 8, 12)
                isChecked = selectedSymptoms.contains(symptom)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedSymptoms.add(symptom)
                    else selectedSymptoms.remove(symptom)
                }
            }
            container.addView(cb)
        }

        btnNext.setOnClickListener {
            if (selectedSymptoms.isEmpty()) {
                snack(btnNext, getString(R.string.onboarding_symptoms_error))
                return@setOnClickListener
            }
            showStep(2)
        }
    }

    // Шаг 2: Цель

    private fun setupStepGoal() {
        val container = findViewById<LinearLayout>(R.id.goalsContainer)
        val btnNext = findViewById<MaterialButton>(R.id.btnNextGoal)
        val btnBack = findViewById<ImageButton>(R.id.btnBack2)

        btnBack.setOnClickListener { goBack() }

        container.removeAllViews()
        val rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }

        OnboardingManager.goalOptions(this).forEach { goal ->
            val rb = RadioButton(this).apply {
                text = goal
                textSize = 15f
                setTextColor(getColor(R.color.black))
                setPadding(8, 12, 8, 12)
                id = goal.hashCode()
            }
            rg.addView(rb)
            if (goal == selectedGoal) rg.check(rb.id)
        }

        rg.setOnCheckedChangeListener { _, id ->
            selectedGoal = OnboardingManager.goalOptions(this)
                .firstOrNull { it.hashCode() == id } ?: ""
        }
        container.addView(rg)

        btnNext.setOnClickListener {
            if (selectedGoal.isBlank()) {
                snack(btnNext, getString(R.string.onboarding_goal_error))
                return@setOnClickListener
            }
            showStep(3)
        }
    }

    // Шаг 3: Частота

    private fun setupStepFrequency() {
        val container = findViewById<LinearLayout>(R.id.frequencyContainer)
        val btnFinish = findViewById<MaterialButton>(R.id.btnFinish)
        val btnBack = findViewById<ImageButton>(R.id.btnBack3)

        btnBack.setOnClickListener { goBack() }

        container.removeAllViews()
        val rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }

        OnboardingManager.frequencyOptions(this).forEach { freq ->
            val rb = RadioButton(this).apply {
                text = freq
                textSize = 15f
                setTextColor(getColor(R.color.black))
                setPadding(8, 12, 8, 12)
                id = freq.hashCode()
            }
            rg.addView(rb)
            if (freq == selectedFrequency) rg.check(rb.id)
        }

        rg.setOnCheckedChangeListener { _, id ->
            selectedFrequency = OnboardingManager.frequencyOptions(this)
                .firstOrNull { it.hashCode() == id } ?: ""
        }
        container.addView(rg)

        btnFinish.setOnClickListener {
            if (selectedFrequency.isBlank()) {
                snack(btnFinish, getString(R.string.onboarding_frequency_error))
                return@setOnClickListener
            }
            saveAndFinish(btnFinish)
        }
    }

    // Сохранение в Room

    private fun saveAndFinish(anchor: View) {
        val symptomsStr = OnboardingManager.symptomsToString(selectedSymptoms.toList())

        executor.execute {
            AppDatabase.getInstance(this).userDao().updateOnboarding(
                id = userId,
                displayName = displayName,
                symptoms = symptomsStr,
                goal = selectedGoal,
                entryFrequency = selectedFrequency
            )
            runOnUiThread {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    // Утилиты
    private fun snack(anchor: View, msg: String) =
        Snackbar.make(
            anchor,
            msg,
            Snackbar.LENGTH_SHORT
        )
            .show()

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
        AnimatorSet().apply {
            playTogether(
                animX, animY
            )
            start()
        }
    }
}
