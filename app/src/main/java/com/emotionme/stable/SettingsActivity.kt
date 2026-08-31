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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private var doubleClick: Long = 0
    private var selectedTheme: String = ThemeManager.THEME_BLUE
    private var selectedLang: String = LanguageManager.LANG_RUSSIAN

    @SuppressLint("MissingInflatedId", "ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Анимация фона
        startFloatingAnimation(findViewById(R.id.spot1), 5000)
        startFloatingAnimation(findViewById(R.id.spot2), 8000)
        startFloatingAnimation(findViewById(R.id.spot3), 10000)

        // Заголовок
        findViewById<TextView>(R.id.settingsTitleTV).text = getString(R.string.screen_settings)

        // Кнопка назад
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Уведомления
        val switchNotify = findViewById<MaterialSwitch>(R.id.notification_switcher)
        val timePicker = findViewById<TimePicker>(R.id.time_picker)
        val enabled = prefs.getBoolean("notify_enabled", true)
        val hour = prefs.getInt("notify_hour", 12)
        val minute = prefs.getInt("notify_minute", 0)

        switchNotify.text = getString(R.string.notifications)
        switchNotify.isChecked = enabled
        timePicker.hour = hour
        timePicker.minute = minute
        timePicker.isEnabled = enabled
        timePicker.alpha = if (enabled) 1f else 0.5f
        timePicker.setIs24HourView(true)

        switchNotify.setOnCheckedChangeListener { _, isChecked ->
            Snackbar.make(
                switchNotify,
                if (isChecked) getString(R.string.notification_on)
                else getString(R.string.notification_off),
                Snackbar.LENGTH_SHORT
            ).show()
            prefs.edit { putBoolean("notify_enabled", isChecked) }
            timePicker.isEnabled = isChecked
            timePicker.alpha = if (isChecked) 1f else 0.5f
            if (isChecked) NotificationScheduler.scheduleDaily(this, timePicker.hour)
            else NotificationScheduler.cancelAll(this)
        }

        timePicker.setOnTimeChangedListener { _, h, m ->
            prefs.edit { putInt("notify_hour", h); putInt("notify_minute", m) }
            if (switchNotify.isChecked) NotificationScheduler.scheduleDaily(this, h)
            if (prefs.getBoolean("notify_enabled", false))
                NotificationScheduler.scheduleDaily(this, prefs.getInt("notify_hour", 12))
        }

        // Цветовая тема
        selectedTheme = ThemeManager.getSavedTheme(this)
        val themeGroup = findViewById<RadioGroup>(R.id.themeRadioGroup)
        themeGroup.removeAllViews()

        ThemeManager.allThemes.forEach { themeKey ->
            val rb = RadioButton(this).apply {
                id = themeKey.hashCode()
                text = ThemeManager.displayName(this@SettingsActivity, themeKey)
                textSize = 15f
                setTextColor(android.graphics.Color.BLACK)
                setPadding(8, 16, 8, 16)
            }
            themeGroup.addView(rb)
            if (themeKey == selectedTheme) themeGroup.check(rb.id)
        }

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = ThemeManager.allThemes.firstOrNull { it.hashCode() == checkedId }
                ?: ThemeManager.THEME_BLUE
            if (newTheme != selectedTheme) {
                selectedTheme = newTheme
                ThemeManager.saveTheme(this, newTheme)
                Snackbar.make(themeGroup, getString(R.string.theme_changed), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.theme_restart)) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }.show()
            }
        }

        // Язык
        selectedLang = LanguageManager.getSavedLanguage(this)
        val langGroup = findViewById<RadioGroup>(R.id.langRadioGroup)
        langGroup.removeAllViews()

        LanguageManager.allLanguages.forEach { langKey ->
            val rb = RadioButton(this).apply {
                id = langKey.hashCode()
                text = LanguageManager.displayName(langKey)
                textSize = 15f
                setTextColor(android.graphics.Color.BLACK)
                setPadding(8, 16, 8, 16)
            }
            langGroup.addView(rb)
            if (langKey == selectedLang) langGroup.check(rb.id)
        }

        langGroup.setOnCheckedChangeListener { _, checkedId ->
            val newLang = LanguageManager.allLanguages.firstOrNull { it.hashCode() == checkedId }
                ?: LanguageManager.LANG_RUSSIAN
            if (newLang != selectedLang) {
                selectedLang = newLang
                LanguageManager.saveLanguage(this, newLang)
                Snackbar.make(langGroup, getString(R.string.lang_changed), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.theme_restart)) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }.show()
            }
        }

        // Кнопка «Мои ответы»
        findViewById<MaterialButton>(R.id.btnAnswers).setOnClickListener {
            startActivity(Intent(this, AnswersActivity::class.java))
        }

        // Кнопка «Изменить пароль»
        findViewById<MaterialButton>(R.id.btnPasswordActivity).setOnClickListener {
            startActivity(Intent(this, PasswordActivity::class.java))
        }

        // Выйти из аккаунта
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - doubleClick < 2000) {
                SessionManager.logout(this)
                startActivity(Intent(this, AuthActivity::class.java))
                finishAffinity()
                Toast.makeText(this, getString(R.string.btn_logout_action), Toast.LENGTH_SHORT)
                    .show()
                doubleClick = 0
            } else {
                doubleClick = now
                Snackbar.make(
                    btnLogout,
                    getString(R.string.btn_logout_confirm),
                    Snackbar.LENGTH_SHORT
                )
                    .setDuration(2000).show()
            }
        }

        // Версия
        val infoTV = findViewById<TextView>(R.id.versionTV)
        infoTV.text = getString(R.string.app_version)
        infoTV.setOnClickListener {
            Snackbar.make(infoTV, getString(R.string.app_info), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun startFloatingAnimation(view: View, duration: Long) {
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
