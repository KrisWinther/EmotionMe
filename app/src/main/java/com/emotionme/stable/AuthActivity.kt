package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class AuthActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.getUser(this) != 0L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        val etLogin = findViewById<EditText>(R.id.etLogin)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val spot1 = findViewById<View>(R.id.spot1)
        val spot2 = findViewById<View>(R.id.spot2)
        val spot3 = findViewById<View>(R.id.spot3)

        startFloatingAnimation(spot1, 7000)
        startFloatingAnimation(spot2, 9000)
        startFloatingAnimation(spot3, 12000)

        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()
        val executor = Executors.newSingleThreadExecutor()

        fun snack(message: String) =
            Snackbar.make(btnLogin, message, Snackbar.LENGTH_SHORT).show()

        btnLogin.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            if (login.isBlank() || password.isBlank()) {
                snack("Заполните все поля 💬")
                return@setOnClickListener
            }

            executor.execute {
                val user = userDao.login(login, password)

                runOnUiThread {
                    if (user != null) {
                        SessionManager.saveUser(this, user.id)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        snack("Неверный логин или пароль ❌")
                    }
                }
            }
        }

        btnRegister.setOnClickListener {
            val login = etLogin.text.toString()
            val password = etPassword.text.toString()

            if (login.isBlank() || password.isBlank()) {
                snack("Заполните поля ❌")
                return@setOnClickListener
            }

            executor.execute {
                val exists = userDao.getByLogin(login)

                if (exists != null) {
                    runOnUiThread { snack("Пользователь уже существует ❌") }
                } else {
                    val id = userDao.insert(User(login = login, password = password))

                    runOnUiThread {
                        SessionManager.saveUser(this, id)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
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