package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val switchNotify = findViewById<MaterialSwitch>(R.id.notification_switcher)
        val timePicker = findViewById<TimePicker>(R.id.time_picker)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        val infoTV = findViewById<TextView>(R.id.versionTV)
        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnPasswordActivity = findViewById<MaterialButton>(R.id.btnPasswordActivity)
        val enabled = prefs.getBoolean("notify_enabled", true)
        val hour = prefs.getInt("notify_hour", 12)
        val minute = prefs.getInt("notify_minute", 0)
        val spot1 = findViewById<View>(R.id.spot1)
        val spot2 = findViewById<View>(R.id.spot2)
        val spot3 = findViewById<View>(R.id.spot3)

        startFloatingAnimation(spot1, 7000)
        startFloatingAnimation(spot2, 9000)
        startFloatingAnimation(spot3, 12000)

        switchNotify.isChecked = enabled
        timePicker.hour = hour
        timePicker.minute = minute
        timePicker.isEnabled = enabled
        timePicker.alpha = if (enabled) 1f else 0.5f
        timePicker.setIs24HourView(true)

        btnLogout.setOnClickListener { view ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - doubleClick < 2000){
                performAction()
                doubleClick = 0 }
            else {
                doubleClick = currentTime
                Snackbar.make(
                    btnLogout,
                    "Нажми ещё раз, чтобы выйти",
                    Snackbar.LENGTH_SHORT)
                    .setDuration(2000)
                    .show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnPasswordActivity.setOnClickListener {
            startActivity(Intent(
                this,
                PasswordActivity::class.java)
            )
        }

        infoTV.setOnClickListener{
            Snackbar.make(
                infoTV,
                "EmotionMe version 1.3beta build April 17th, 2026 by KrisWinther",
                Snackbar.LENGTH_SHORT)
                .show()
        }

        switchNotify.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "Уведомления включены ✅"
            else "Уведомления выключены ❌"
            Snackbar.make(
                switchNotify,
                message,
                Snackbar.LENGTH_SHORT)
                .show()

            prefs.edit {
                putBoolean("notify_enabled", isChecked)
            }

            timePicker.isEnabled = isChecked
            timePicker.alpha = if (isChecked) 1f else 0.5f

            if (isChecked) {
                NotificationScheduler.scheduleDaily(this, timePicker.hour)
            } else {
                NotificationScheduler.cancelAll(this)
            }
        }

        timePicker.setOnTimeChangedListener { _, h, m ->
            prefs.edit {
                putInt("notify_hour", h)
                putInt("notify_minute", m)
            }

            if (switchNotify.isChecked) {
                NotificationScheduler.scheduleDaily(this, h)
            }

            if (prefs.getBoolean("notify_enabled", false)) {
                NotificationScheduler.scheduleDaily(this, prefs.getInt("notify_hour", 12))
            }
        }
    }
    private fun performAction(){
        SessionManager.logout(this)
        startActivity(Intent(this, AuthActivity::class.java))
        finishAffinity()
        Toast.makeText(
            this,
            "Завершаем сессию…",
            Toast.LENGTH_SHORT)
            .show()
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