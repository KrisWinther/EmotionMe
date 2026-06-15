package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.getUser(this) != 0L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        LanguageManager.applyLanguage(this)
        ThemeManager.applyTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        val etLogin = findViewById<EditText>(R.id.etLogin)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnSettingsStart = findViewById<TextView>(R.id.btnSettingsStart)
        val spot1 = findViewById<View>(R.id.spot1)
        val spot2 = findViewById<View>(R.id.spot2)
        val spot3 = findViewById<View>(R.id.spot3)

        startFloatingAnimation(spot1, 5000)
        startFloatingAnimation(spot2, 7000)
        startFloatingAnimation(spot3, 9000)

        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()
        val executor = Executors.newSingleThreadExecutor()

        btnSettingsStart.setOnClickListener {
            startActivity(Intent(this, SettingsStartActivity::class.java))
        }

        fun snack(message: Int) =
            Snackbar.make(btnLogin, message, Snackbar.LENGTH_SHORT).show()

        btnLogin.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            if (login.isBlank() || password.isBlank()) {
                snack(R.string.error_fill_fields)
                return@setOnClickListener
            }

            executor.execute {
                val user = userDao.login(login)
                val verified = when {
                    user == null -> false
                    user.salt.isBlank() -> {
                        // Старый аккаунт без соли — plain-text fallback,
                        // Сразу рехэшируем пароль
                        if (user.password == password) {
                            val newSalt = PasswordHasher.generateSalt()
                            val newHash = PasswordHasher.hash(password, newSalt)
                            userDao.updatePassword(user.id, newHash, newSalt)
                            true
                        } else false
                    }

                    else -> PasswordHasher.verify(
                        password,
                        user.salt,
                        user.password
                    )
                }

                runOnUiThread {
                    if (verified && user != null) {
                        SessionManager.saveUser(this, user.id)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        snack(R.string.error_wrong_credentials)
                    }
                }
            }
        }

        btnRegister.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            if (login.isBlank() || password.isBlank()) {
                snack(R.string.error_fill_fields)
                return@setOnClickListener
            }

            executor.execute {
                val exists = userDao.getByLogin(login)

                if (exists != null) {
                    runOnUiThread { snack(R.string.error_user_exists) }
                } else {
                    val salt = PasswordHasher.generateSalt()
                    val hash = PasswordHasher.hash(password, salt)
                    val id = userDao.insert(User(login = login, password = hash, salt = salt))

                    runOnUiThread {
                        SessionManager.saveUser(
                            this,
                            id
                        )
                        startActivity(Intent(
                            this,
                            OnboardingActivity::class.java)
                        )
                        finish()
                    }
                }
            }
        }
    }

    fun startFloatingAnimation(view: View, duration: Long) {
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
            view, "translationY",
            -150f, 150f
        )
            .apply {
            this.duration = duration + 800 // Разная скорость для естественности
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet()
            .apply {
            playTogether(animX, animY)
            start()
        }
    }
}