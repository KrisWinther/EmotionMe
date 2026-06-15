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

@Suppress("DEPRECATION")
class PasswordActivity : AppCompatActivity() {

    private var doubleClick: Long = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
        enableEdgeToEdge()

        setContentView(R.layout.activity_password)

        val btnSave = findViewById<Button>(R.id.btnSavePassword)
        val btnBack = findViewById<TextView>(R.id.btnBackPassword)
        val spot1 = findViewById<View>(R.id.spot1)
        val spot2 = findViewById<View>(R.id.spot2)
        val spot3 = findViewById<View>(R.id.spot3)

        startFloatingAnimation(spot1, 5000)
        startFloatingAnimation(spot2, 7000)
        startFloatingAnimation(spot3, 9000)

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
                    getString(R.string.hint_confirm_password),
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
            Snackbar.make(
                root,
                getString(R.string.error_fill_fields),
                Snackbar.LENGTH_SHORT
            )
                .show()
            return
        }

        if (newPass != confirmPass) {
            Snackbar.make(
                root,
                getString(R.string.error_passwords_not_match),
                Snackbar.LENGTH_SHORT
            )
                .show()
            return
        }

        if (newPass.length < 5) {
            Snackbar.make(
                root,
                getString(R.string.error_password_not_enough_chars),
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

            // Проверяем, что новый пароль не совпадает с текущим
            val isSamePassword = if (user.salt.isBlank()) {
                newPass == user.password   // старый аккаунт без соли
            } else {
                PasswordHasher.verify(
                    newPass,
                    user.salt,
                    user.password
                )
            }

            if (isSamePassword) {
                runOnUiThread {
                    Snackbar.make(
                        root,
                        getString(R.string.error_passwords_not_match),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }
                return@Thread
            }

            // Генерируем новую соль и хэш
            val newSalt = PasswordHasher.generateSalt()
            val newHash = PasswordHasher.hash(
                newPass,
                newSalt
            )
            db.userDao().updatePassword(
                userId,
                newHash,
                newSalt
            )

            runOnUiThread {
                Toast.makeText(
                    this,
                    getString(R.string.password_changed),
                    Toast.LENGTH_SHORT
                )
                    .show()
                finish()
                overridePendingTransition(
                    R.anim.slide_out_left,
                    R.anim.slide_in_right
                )
            }
        }.start()
    }

    fun startFloatingAnimation(view: View, duration: Long) {
        val animX = ObjectAnimator.ofFloat(
            view,
            "translationX",
            -150f, 150f
        ).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val animY = ObjectAnimator.ofFloat(
            view,
            "translationY",
            -150f, 150f
        ).apply {
            this.duration = duration + 800 // Разная скорость для естественности
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(
                animX,
                animY
            )
            start()
        }
    }
}