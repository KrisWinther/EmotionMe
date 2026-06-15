package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        private const val MIN_SPLASH_MS = 600L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        startFloatingAnimation(findViewById(R.id.spot1), 5000)
        startFloatingAnimation(findViewById(R.id.spot2), 7500)
        startFloatingAnimation(findViewById(R.id.spot3), 10000)

        val startTime = System.currentTimeMillis()

        executor.execute {
            // Любое обращение к БД триггерит открытие + миграции,
            // если они ещё не были выполнены.
            val db = AppDatabase.getInstance(applicationContext)
            val userId = SessionManager.getUser(applicationContext)
            val userExists = if (userId != 0L) db.userDao().getById(userId) != null else false

            val elapsed = System.currentTimeMillis() - startTime
            val remaining = (MIN_SPLASH_MS - elapsed).coerceAtLeast(0L)

            if (remaining > 0) Thread.sleep(remaining)

            runOnUiThread {
                val target = if (userId != 0L && userExists) {
                    MainActivity::class.java
                } else {
                    AuthActivity::class.java
                }
                startActivity(Intent(this, target))
                finish()
            }
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
