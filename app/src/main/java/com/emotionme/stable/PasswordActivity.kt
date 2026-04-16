package com.emotionme.stable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class PasswordActivity : AppCompatActivity() {

    private var doubleClick: Long = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_password)

        val btnSave = findViewById<Button>(R.id.btnSavePassword)
        val btnBack = findViewById<TextView>(R.id.btnBackPassword)
        val spot1 = findViewById<View>(R.id.spot1)
        val spot2 = findViewById<View>(R.id.spot2)
        val spot3 = findViewById<View>(R.id.spot3)

        startFloatingAnimation(spot1, 7000)
        startFloatingAnimation(spot2, 9000)
        startFloatingAnimation(spot3, 12000)

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - doubleClick < 2000) {
                performAction()
                doubleClick = 0
            } else {
                doubleClick = currentTime
                Snackbar.make(
                    btnSave,
                    "Нажми ещё раз, чтобы подтвердить изменение пароля",
                    Snackbar.LENGTH_SHORT
                )
                    .setDuration(2000)
                    .show()
            }
        }
    }

    private fun performAction() {

        val db = AppDatabase.getInstance(this)
        val userId = SessionManager.getUser(this)

        val etNew = findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave = findViewById<Button>(R.id.btnSavePassword)
        val newPass = etNew.text.toString().trim()
        val confirmPass = etConfirm.text.toString().trim()
        val root = btnSave.rootView

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Snackbar.make(root, "Заполните все поля", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            Snackbar.make(root, "Пароли не совпадают", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (newPass.length < 5) {
            Snackbar.make(
                root,
                "Пароль должен содержать не менее 5 символов",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        Thread {
            val user = db.userDao().getById(userId)

            if (user == null) {
                runOnUiThread { finish() }
                return@Thread
            }

            if (newPass == user.password) {
                runOnUiThread {
                    Snackbar.make(
                        root,
                        "Новый пароль должен отличаться",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                return@Thread
            }

            db.userDao().updatePassword(userId, newPass)
            runOnUiThread {
                Toast.makeText(this, "Пароль успешно изменён ✅", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
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