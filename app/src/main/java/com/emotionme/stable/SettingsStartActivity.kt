package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class SettingsStartActivity : AppCompatActivity() {

    private var selectedTheme: String = ThemeManager.THEME_BLUE
    private var selectedLang: String = LanguageManager.LANG_RUSSIAN

    @SuppressLint("MissingInflatedId", "ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_auth)

        // Анимация фона
        startFloatingAnimation(findViewById(R.id.spot1), 5000)
        startFloatingAnimation(findViewById(R.id.spot2), 8000)
        startFloatingAnimation(findViewById(R.id.spot3), 10000)

        // Заголовок
        findViewById<TextView>(R.id.settingsTitleTV).text = getString(R.string.screen_settings)

        // Кнопка назад
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        selectedTheme = ThemeManager.getSavedTheme(this)
        val themeGroup = findViewById<RadioGroup>(R.id.themeRadioGroup)
        themeGroup.removeAllViews()

        ThemeManager.allThemes.forEach { themeKey ->
            val rb = RadioButton(this).apply {
                id = themeKey.hashCode()
                text = ThemeManager.displayName(this@SettingsStartActivity, themeKey)
                textSize = 15f
                setTextColor(R.color.black)
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
                setTextColor(R.color.black)
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