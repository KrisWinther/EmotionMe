package com.emotionme.stable

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val switchNotify = findViewById<Switch>(R.id.notification_switcher)
        val timePicker = findViewById<TimePicker>(R.id.time_picker)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val enabled = prefs.getBoolean("notify_enabled", true)
        val hour = prefs.getInt("notify_hour", 12)
        val minute = prefs.getInt("notify_minute", 0)

        switchNotify.isChecked = enabled
        timePicker.hour = hour
        timePicker.minute = minute
        timePicker.isEnabled = enabled
        timePicker.alpha = if (enabled) 1f else 0.5f
        timePicker.setIs24HourView(true)

        btnLogout.setOnClickListener {
            SessionManager.logout(this)
            startActivity(Intent(this, AuthActivity::class.java))
            finishAffinity()
        }

        btnBack.setOnClickListener {
            finish()
        }

        switchNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                Toast.makeText(this, "Уведомления включены ✅", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Уведомления выключены ❌", Toast.LENGTH_SHORT).show()
            }

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
}